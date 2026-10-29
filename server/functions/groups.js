// B3: real groups. The server creates groups, adds members, drops each day's question at the
// group's time (India time), unlocks answers and keeps streaks and freezes.
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const QUESTIONS = require("./questions.json");

const db = () => admin.firestore();
const FieldValue = admin.firestore.FieldValue;

const HOUR = 3600 * 1000;
const IST = 5.5 * HOUR; // India has no daylight saving, so a fixed offset is exact
const UNLOCK_AFTER = 2 * HOUR;
const SLOT_HOURS = { morning: 9, evening: 19, night: 22 };
const BIG_GROUP = 10;
const FREEZE_EVERY = 7;
const MAX_FREEZES = 2;
const MAX_MEMBERS = 200;

function requireUser(req) {
  if (!req.auth || !req.auth.uid) throw new HttpsError("unauthenticated", "Sign in first.");
  return req.auth.uid;
}

/** The next time the group's slot (9 AM, 7 PM or 10 PM India time) comes round, at least an hour from now. */
function nextDrop(slot, now = Date.now()) {
  const hour = SLOT_HOURS[slot] ?? 19;
  const local = new Date(now + IST);
  local.setUTCHours(hour, 0, 0, 0);
  let at = local.getTime() - IST;
  while (at <= now + HOUR) at += 24 * HOUR;
  return at;
}

/** Small groups need everyone; groups of 10 or more keep the streak at 80%. */
const streakTarget = (n) => (n >= BIG_GROUP ? Math.ceil(n * 0.8) : n);

/** Deterministic shuffle per group, so questions don't repeat until the bank runs out. */
function pickQuestion(seed, day, recent) {
  let s = (seed >>> 0) + Math.floor((day - 1) / QUESTIONS.length);
  const rand = () => {
    s |= 0; s = (s + 0x6d2b79f5) | 0;
    let t = Math.imul(s ^ (s >>> 15), 1 | s);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  const order = QUESTIONS.map((_, i) => i);
  for (let i = order.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [order[i], order[j]] = [order[j], order[i]];
  }
  const start = (day - 1) % QUESTIONS.length;
  for (let k = 0; k < order.length; k++) {
    const idx = order[(start + k) % order.length];
    if (!recent.has(idx)) return idx;
  }
  return order[start];
}

const code6 = () => {
  const abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O or 1/I mix-ups
  let out = "";
  for (let i = 0; i < 6; i++) out += abc[Math.floor(Math.random() * abc.length)];
  return out;
};

/**
 * Drops the group's next question. Before that, settles the previous day:
 * if not enough people answered, a saved freeze protects the streak, otherwise it resets.
 */
async function dropQuestion(gid, now = Date.now()) {
  const gref = db().collection("groups").doc(gid);
  await db().runTransaction(async (tx) => {
    // Firestore transactions need every read before any write.
    const g = (await tx.get(gref)).data();
    if (!g) return;
    const prevRef = g.currentId ? gref.collection("questions").doc(g.currentId) : null;
    const prev = prevRef ? (await tx.get(prevRef)).data() : null;
    const recentSnap = await tx.get(gref.collection("questions").orderBy("day", "desc").limit(20));
    let streak = g.streak || 0;
    let freezes = g.freezes || 0;
    if (prev) {
      if (!prev.allAnswered && streak > 0) {
        if (freezes > 0) { freezes -= 1; tx.update(prevRef, { frozen: true, unlocked: true }); }
        else { streak = 0; tx.update(prevRef, { unlocked: true }); }
      } else if (!prev.unlocked) tx.update(prevRef, { unlocked: true });
    }
    const day = (g.day || 0) + 1;
    const recent = new Set(recentSnap.docs.map((d) => d.get("qIndex")).filter((i) => i >= 0));
    let text, qIndex = -1, askedBy = null;
    if (g.nextQuestion) { text = g.nextQuestion; askedBy = g.nextQuestionBy || null; }
    else { qIndex = pickQuestion(g.seed || 1, day, recent); text = QUESTIONS[qIndex].text; }
    const qid = "day-" + String(day).padStart(5, "0");
    tx.set(gref.collection("questions").doc(qid), {
      day, text, qIndex, askedBy, dropAt: now, unlockAt: now + UNLOCK_AFTER,
      answeredBy: [], unlocked: false, allAnswered: false, frozen: false,
    });
    tx.update(gref, {
      day, currentId: qid, streak, freezes, nextDropAt: nextDrop(g.slot, now),
      nextQuestion: FieldValue.delete(), nextQuestionBy: FieldValue.delete(), lastActivityAt: now,
    });
  });
}

/** Creates a real group with you as the first member, and drops its first question right away. */
exports.createGroup = onCall(async (req) => {
  const uid = requireUser(req);
  const name = String((req.data && req.data.name) || "").trim().slice(0, 40);
  const slot = SLOT_HOURS[req.data && req.data.slot] ? req.data.slot : "evening";
  if (!name) throw new HttpsError("invalid-argument", "Give the group a name.");
  let code = code6();
  for (let i = 0; i < 5 && (await db().collection("invites").doc(code).get()).exists; i++) code = code6();
  const gref = db().collection("groups").doc();
  const now = Date.now();
  await gref.set({
    name, slot, createdBy: uid, memberIds: [uid], inviteCode: code, seed: Math.floor(Math.random() * 1e9),
    day: 0, streak: 0, bestStreak: 0, freezes: 0, currentId: null, nextDropAt: null, createdAt: now, lastActivityAt: now,
  });
  await db().collection("invites").doc(code).set({ gid: gref.id, at: now });
  await dropQuestion(gref.id, now);
  return { gid: gref.id, code };
});

/** Joins a group with its 6-character invite code. */
exports.joinGroup = onCall(async (req) => {
  const uid = requireUser(req);
  const code = String((req.data && req.data.code) || "").trim().toUpperCase();
  const invite = await db().collection("invites").doc(code).get();
  if (!invite.exists) throw new HttpsError("not-found", "That invite code doesn’t exist. Check it and try again.");
  const gid = invite.get("gid");
  const gref = db().collection("groups").doc(gid);
  await db().runTransaction(async (tx) => {
    const g = (await tx.get(gref)).data();
    if (!g) throw new HttpsError("not-found", "This group no longer exists.");
    if ((g.memberIds || []).includes(uid)) return;
    if ((g.memberIds || []).length >= MAX_MEMBERS) throw new HttpsError("resource-exhausted", "This group is full.");
    tx.update(gref, { memberIds: FieldValue.arrayUnion(uid), lastActivityAt: Date.now() });
  });
  return { gid };
});

/** Leaves a group. The last person to leave deletes it. */
exports.leaveGroup = onCall(async (req) => {
  const uid = requireUser(req);
  const gid = String((req.data && req.data.gid) || "");
  const gref = db().collection("groups").doc(gid);
  const g = (await gref.get()).data();
  if (!g || !(g.memberIds || []).includes(uid)) return { ok: true };
  if (g.memberIds.length <= 1) {
    await db().recursiveDelete(gref);
    if (g.inviteCode) await db().collection("invites").doc(g.inviteCode).delete();
  } else {
    await gref.update({ memberIds: FieldValue.arrayRemove(uid) });
  }
  return { ok: true };
});

/** When someone answers: count them, and unlock + grow the streak once enough people are in. */
exports.onAnswer = onDocumentCreated({ document: "groups/{gid}/questions/{qid}/answers/{uid}", region: "asia-south1" }, async (event) => {
  const { gid, qid, uid } = event.params;
  const gref = db().collection("groups").doc(gid);
  const qref = gref.collection("questions").doc(qid);
  await db().runTransaction(async (tx) => {
    const g = (await tx.get(gref)).data();
    const q = (await tx.get(qref)).data();
    if (!g || !q) return;
    const answeredBy = Array.from(new Set([...(q.answeredBy || []), uid]));
    const members = (g.memberIds || []).length;
    const inNow = answeredBy.filter((m) => g.memberIds.includes(m)).length >= streakTarget(members);
    const update = { answeredBy };
    if (inNow && !q.allAnswered) {
      update.allAnswered = true;
      update.unlocked = true;
      if (g.currentId === qid && members > 1) {
        const streak = (g.streak || 0) + 1;
        const freezes = streak % FREEZE_EVERY === 0 ? Math.min(MAX_FREEZES, (g.freezes || 0) + 1) : g.freezes || 0;
        tx.update(gref, { streak, freezes, bestStreak: Math.max(g.bestStreak || 0, streak), lastActivityAt: Date.now() });
      }
    }
    tx.update(qref, update);
  });
});

/** Every 10 minutes: unlock questions past their 2-hour window, and drop questions that are due. */
exports.clock = onSchedule({ schedule: "every 10 minutes", region: "asia-south1", timeZone: "Asia/Kolkata" }, async () => {
  const now = Date.now();
  const due = await db().collection("groups").where("nextDropAt", "<=", now).limit(200).get();
  for (const g of due.docs) await dropQuestion(g.id, now).catch((e) => console.error("drop failed", g.id, e));
  const open = await db().collectionGroup("questions").where("unlocked", "==", false).where("unlockAt", "<=", now).limit(500).get();
  const batch = db().batch();
  open.docs.forEach((d) => batch.update(d.ref, { unlocked: true }));
  if (!open.empty) await batch.commit();
});

/** Nudges a member who hasn't answered yet (push notifications arrive in B6). */
exports.nudge = onCall(async (req) => {
  const uid = requireUser(req);
  const { gid, to } = req.data || {};
  const g = (await db().collection("groups").doc(String(gid)).get()).data();
  if (!g || !g.memberIds.includes(uid) || !g.memberIds.includes(to)) throw new HttpsError("permission-denied", "You’re not in this group.");
  await db().collection("groups").doc(String(gid)).collection("nudges").add({ from: uid, to, qid: g.currentId, at: Date.now() });
  return { ok: true };
});

/** Today's question master writes tomorrow's question. */
exports.setNextQuestion = onCall(async (req) => {
  const uid = requireUser(req);
  const { gid } = req.data || {};
  const text = String((req.data && req.data.text) || "").trim().slice(0, 140);
  if (text.length < 5) throw new HttpsError("invalid-argument", "Write a question for tomorrow first.");
  const gref = db().collection("groups").doc(String(gid));
  const g = (await gref.get()).data();
  if (!g || !g.memberIds.includes(uid)) throw new HttpsError("permission-denied", "You’re not in this group.");
  const master = g.memberIds[(((g.day || 0) + (g.seed || 0)) % g.memberIds.length + g.memberIds.length) % g.memberIds.length];
  if (master !== uid) throw new HttpsError("failed-precondition", "It’s not your turn to ask yet.");
  await gref.update({ nextQuestion: text, nextQuestionBy: uid });
  return { ok: true };
});

exports._test = { nextDrop, streakTarget, pickQuestion };
