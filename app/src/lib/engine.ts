import { AppState } from "react-native";
import { DB, DropSlot, getDB, GroupRec, hydrate, NotifPrefs, qkey, QuestionRec, Sim, update, wipe } from "./store";
import { DEMO_QUESTIONS, questionIndexFor, SAMPLE_FRIENDS, SAMPLE_REPLIES } from "./sample";
import { cancelAll, scheduleFor } from "./notify";

// The rules of Askday, run on the device:
// - each group gets one question a day at its chosen time
// - answers unlock when everyone's in, or 2 hours after the drop
// - the streak grows when everyone answers, and resets when they don't
// Sample friends stand in for real people until the app goes online.

const SEC = 1000;
const MIN = 60 * SEC;
export const UNLOCK_AFTER = 2 * 60 * MIN;
export const SLOT_HOUR: Record<DropSlot, number> = { morning: 9, evening: 19, night: 22 };
export const DEFAULT_NOTIF: NotifPrefs = { daily: true, unlocked: true, nudges: true, reminders: true };

const rand = (min: number, max: number) => min + Math.random() * (max - min);
const pick = <T,>(xs: T[]) => xs[Math.floor(Math.random() * xs.length)];
const newId = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
export const friendId = (name: string) => `friend_${name.toLowerCase()}`;
export const isSample = (uid: string) => uid.startsWith("friend_");

export class AskdayError extends Error {}
export function errorMessage(err: unknown): string {
  if (err instanceof AskdayError) return err.message;
  return "Something went wrong. Try again.";
}

// ------------------------------------------------------------- scheduling

export function nextSlotAfter(slot: DropSlot, after: number): number {
  const d = new Date(after);
  d.setHours(SLOT_HOUR[slot], 0, 0, 0);
  if (d.getTime() <= after + MIN) d.setDate(d.getDate() + 1);
  return d.getTime();
}

// ----------------------------------------------------------------- core

function dropQuestion(d: DB, g: GroupRec, now: number) {
  const prev = g.currentId ? d.questions[g.id]?.[g.currentId] : null;
  if (prev && !prev.unlocked) prev.unlocked = true;
  if (prev && !prev.allAnswered) g.streak = 0;

  const day = g.day + 1;
  const qIndex = questionIndexFor(g.seed, day);
  const q: QuestionRec = {
    id: `day-${String(day).padStart(5, "0")}`,
    day,
    text: DEMO_QUESTIONS[qIndex].text,
    qIndex,
    dropAt: now,
    unlockAt: now + UNLOCK_AFTER,
    unlocked: false,
    allAnswered: false,
    answeredBy: [],
    topAnswerId: null,
    topLikes: 0,
  };
  (d.questions[g.id] ??= {})[q.id] = q;
  d.answers[qkey(g.id, q.id)] = [];
  d.comments[qkey(g.id, q.id)] = [];

  // Some friends answer early, the rest wait until you've answered.
  const answerAt: Sim["answerAt"] = {};
  for (const m of g.memberIds.filter(isSample)) {
    answerAt[m] = Math.random() < 0.55 ? now + rand(15 * SEC, 2 * MIN) : null;
  }
  d.sim[qkey(g.id, q.id)] = { answerAt, events: [] };

  g.day = day;
  g.currentId = q.id;
  g.nextDropAt = nextSlotAfter(g.dropSlot, now);
  g.lastActivityAt = now;
}

function friendAnswerText(q: QuestionRec, uid: string, g: GroupRec): string {
  const answers = DEMO_QUESTIONS[q.qIndex]?.answers ?? ["Good question. Still thinking."];
  const friends = g.memberIds.filter(isSample);
  return answers[(friends.indexOf(uid) + q.day) % answers.length];
}

function addAnswer(d: DB, g: GroupRec, q: QuestionRec, uid: string, text: string, now: number) {
  if (q.answeredBy.includes(uid)) return;
  const key = qkey(g.id, q.id);
  const others = g.memberIds.filter((m) => isSample(m) && m !== uid && q.answeredBy.includes(m));
  const likes = isSample(uid) ? others.filter(() => Math.random() < 0.35) : [];
  (d.answers[key] ??= []).push({ id: uid, uid, text, createdAt: now, likes, replyCount: 0 });
  q.answeredBy.push(uid);
  g.lastActivityAt = now;
  refreshTop(d, g.id, q);

  const me = d.me?.uid;
  if (uid === me && d.profile) {
    d.profile.stats = { ...d.profile.stats, answers: (d.profile.stats?.answers ?? 0) + 1 };
  }

  const everyoneIn = g.memberIds.every((m) => q.answeredBy.includes(m));
  if (everyoneIn && !q.allAnswered) {
    q.allAnswered = true;
    q.unlocked = true;
    if (g.currentId === q.id) {
      g.streak += 1;
      g.bestStreak = Math.max(g.bestStreak, g.streak);
      if (d.profile) {
        d.profile.stats = { ...d.profile.stats, bestStreak: Math.max(d.profile.stats?.bestStreak ?? 0, g.streak) };
      }
    }
    planReactions(d, g, q, now);
  }
}

/** After answers unlock, friends like your answer and someone replies. */
function planReactions(d: DB, g: GroupRec, q: QuestionRec, now: number) {
  const me = d.me?.uid;
  if (!me || !q.answeredBy.includes(me)) return;
  const sim = d.sim[qkey(g.id, q.id)];
  if (!sim) return;
  const friends = g.memberIds.filter(isSample);
  for (const f of friends) {
    if (Math.random() < 0.65) sim.events.push({ at: now + rand(4 * SEC, 40 * SEC), kind: "like", uid: f, target: me });
  }
  if (friends.length) {
    sim.events.push({ at: now + rand(8 * SEC, 45 * SEC), kind: "reply", uid: pick(friends), target: me, text: pick(SAMPLE_REPLIES) });
  }
}

function refreshTop(d: DB, groupId: string, q: QuestionRec) {
  const list = d.answers[qkey(groupId, q.id)] ?? [];
  const top = [...list].sort((a, b) => b.likes.length - a.likes.length)[0];
  q.topAnswerId = top && top.likes.length ? top.id : null;
  q.topLikes = top?.likes.length ?? 0;
}

/** Moves time forward: drops, sample friends' answers, reactions and unlocks. */
export function tick(now = Date.now()) {
  const changed = update((d) => {
    let dirty = false;
    for (const g of Object.values(d.groups)) {
      if (g.nextDropAt && g.nextDropAt <= now) {
        dropQuestion(d, g, now);
        dirty = true;
      }
      const q = g.currentId ? d.questions[g.id]?.[g.currentId] : null;
      if (!q) continue;
      const key = qkey(g.id, q.id);
      const sim = d.sim[key];
      if (sim) {
        const due = Object.entries(sim.answerAt)
          .filter(([uid, at]) => at !== null && at <= now && g.memberIds.includes(uid) && !q.answeredBy.includes(uid))
          .sort((a, b) => (a[1] as number) - (b[1] as number));
        for (const [uid, at] of due) {
          addAnswer(d, g, q, uid, friendAnswerText(q, uid, g), at as number);
          dirty = true;
        }
        const ready = sim.events.filter((e) => e.at <= now);
        if (ready.length) {
          sim.events = sim.events.filter((e) => e.at > now);
          for (const e of ready) applyEvent(d, g, q, e);
          dirty = true;
        }
      }
      if (!q.unlocked && now >= q.unlockAt) {
        q.unlocked = true;
        dirty = true;
      }
    }
    return dirty;
  });
  if (changed) scheduleFor(getDB());
}

function applyEvent(d: DB, g: GroupRec, q: QuestionRec, e: { at: number; kind: string; uid: string; target?: string; text?: string }) {
  const key = qkey(g.id, q.id);
  if (e.kind === "like") {
    const a = d.answers[key]?.find((x) => x.id === e.target);
    if (a && !a.likes.includes(e.uid)) a.likes.push(e.uid);
    refreshTop(d, g.id, q);
  } else if (e.kind === "reply" || e.kind === "chat") {
    const answerId = e.kind === "reply" ? (e.target ?? null) : null;
    (d.comments[key] ??= []).push({ id: newId(), uid: e.uid, text: e.text ?? "", answerId, createdAt: e.at });
    if (answerId) {
      const a = d.answers[key]?.find((x) => x.id === answerId);
      if (a) a.replyCount += 1;
    }
  }
  g.lastActivityAt = e.at;
}

// --------------------------------------------------------------- startup

let started = false;
/** Loads saved data and keeps the clock running while the app is open. */
export function startEngine(): () => void {
  if (started) return () => undefined;
  started = true;
  hydrate().then(() => tick());
  const timer = setInterval(() => tick(), 3 * SEC);
  const sub = AppState.addEventListener("change", (s) => s === "active" && tick());
  return () => {
    started = false;
    clearInterval(timer);
    sub.remove();
  };
}

// --------------------------------------------------------------- actions

function requireMe(d: DB): string {
  if (!d.me) throw new AskdayError("Start as a guest first.");
  return d.me.uid;
}

export function createGuest() {
  update((d) => {
    d.me = { uid: `me_${newId()}` };
  });
}

export function saveProfile(uid: string, data: { name: string; photoURL: string | null }, isNew: boolean) {
  update((d) => {
    if (isNew || !d.profile) {
      d.profile = { id: uid, ...data, notif: DEFAULT_NOTIF, stats: { answers: 0, bestStreak: 0 }, createdAt: Date.now() };
    } else {
      d.profile = { ...d.profile, ...data };
    }
  });
}

export function saveNotif(notif: NotifPrefs) {
  update((d) => {
    if (d.profile) d.profile.notif = notif;
  });
  scheduleFor(getDB());
}

export function createGroup(input: { name: string; dropSlot: DropSlot; photoURL?: string | null }): { groupId: string; inviteCode: string } {
  const name = input.name.trim();
  if (!name) throw new AskdayError("Give your group a name.");
  const groupId = newId();
  const inviteCode = Array.from({ length: 6 }, () => pick([..."ABCDEFGHJKLMNPQRSTUVWXYZ23456789"])).join("");
  update((d) => {
    const me = requireMe(d);
    const names = [...SAMPLE_FRIENDS].sort(() => Math.random() - 0.5).slice(0, 4);
    for (const n of names) d.people[friendId(n)] ??= { id: friendId(n), name: n, photoURL: null, sample: true };
    const now = Date.now();
    const g: GroupRec = {
      id: groupId,
      name,
      photoURL: input.photoURL ?? null,
      memberIds: [me, ...names.map(friendId)],
      inviteCode,
      dropSlot: input.dropSlot,
      seed: Math.floor(Math.random() * 2 ** 31),
      day: 0,
      streak: 0,
      bestStreak: 0,
      currentId: null,
      nextDropAt: null,
      createdAt: now,
      lastActivityAt: now,
      nudged: {},
    };
    d.groups[groupId] = g;
    dropQuestion(d, g, now); // A new group gets its first question right away.
  });
  scheduleFor(getDB());
  return { groupId, inviteCode };
}

export function setGroupPhoto(groupId: string, photoURL: string) {
  update((d) => {
    const g = d.groups[groupId];
    if (g) g.photoURL = photoURL;
  });
}

export function joinGroup(_code: string): never {
  throw new AskdayError("Joining with a code needs the online version of Askday. For now, create a group and sample friends will join you.");
}

export function leaveGroup(groupId: string) {
  update((d) => {
    delete d.groups[groupId];
    for (const k of Object.keys(d.questions[groupId] ?? {})) {
      delete d.answers[qkey(groupId, k)];
      delete d.comments[qkey(groupId, k)];
      delete d.sim[qkey(groupId, k)];
    }
    delete d.questions[groupId];
    delete d.seen[groupId];
  });
  scheduleFor(getDB());
}

export function submitAnswer(groupId: string, questionId: string, uid: string, text: string) {
  const clean = text.trim().replace(/\s+/g, " ");
  if (!clean) throw new AskdayError("Write an answer first.");
  update((d) => {
    const g = d.groups[groupId];
    const q = d.questions[groupId]?.[questionId];
    if (!g || !q) throw new AskdayError("This question isn’t available anymore.");
    if (g.currentId !== q.id) throw new AskdayError("You can only answer today’s question.");
    const now = Date.now();
    addAnswer(d, g, q, uid, clean.slice(0, 280), now);
    // Friends who were waiting on you answer over the next minute or so.
    const sim = d.sim[qkey(groupId, questionId)];
    if (sim) {
      let at = now;
      for (const m of Object.keys(sim.answerAt)) {
        if (q.answeredBy.includes(m)) continue;
        at += rand(10 * SEC, 35 * SEC);
        const planned = sim.answerAt[m];
        sim.answerAt[m] = planned === null ? at : Math.min(planned, at);
      }
    }
  });
  scheduleFor(getDB());
}

export function toggleLike(groupId: string, questionId: string, answerId: string, uid: string) {
  update((d) => {
    const q = d.questions[groupId]?.[questionId];
    const a = d.answers[qkey(groupId, questionId)]?.find((x) => x.id === answerId);
    if (!q || !a) return false;
    a.likes = a.likes.includes(uid) ? a.likes.filter((x) => x !== uid) : [...a.likes, uid];
    refreshTop(d, groupId, q);
  });
}

export function postComment(groupId: string, questionId: string, uid: string, text: string, answerId: string | null) {
  const clean = text.trim();
  if (!clean) return;
  update((d) => {
    const key = qkey(groupId, questionId);
    const now = Date.now();
    (d.comments[key] ??= []).push({ id: newId(), uid, text: clean.slice(0, 280), answerId, createdAt: now });
    if (answerId) {
      const a = d.answers[key]?.find((x) => x.id === answerId);
      if (a) a.replyCount += 1;
    }
    // Sometimes a friend writes back.
    const g = d.groups[groupId];
    const sim = d.sim[key];
    const friends = g?.memberIds.filter(isSample) ?? [];
    if (sim && friends.length && Math.random() < 0.6) {
      const replier = answerId && isSample(answerId) ? answerId : pick(friends);
      sim.events.push({
        at: now + rand(5 * SEC, 18 * SEC),
        kind: answerId ? "reply" : "chat",
        uid: replier,
        target: answerId ?? undefined,
        text: pick(SAMPLE_REPLIES),
      });
    }
  });
}

export function nudge(groupId: string, targetUid: string) {
  update((d) => {
    const g = d.groups[groupId];
    const q = g?.currentId ? d.questions[groupId]?.[g.currentId] : null;
    if (!g || !q) throw new AskdayError("There’s no question to answer yet.");
    if (q.answeredBy.includes(targetUid)) throw new AskdayError("They’ve already answered.");
    if (g.nudged[targetUid] === q.id) throw new AskdayError("You’ve already nudged them today.");
    g.nudged[targetUid] = q.id;
    const sim = d.sim[qkey(groupId, q.id)];
    // A nudge works: they answer within a few seconds.
    if (sim) sim.answerAt[targetUid] = Date.now() + rand(4 * SEC, 9 * SEC);
  });
}

export function markSeen(groupId: string, questionId: string, count: number) {
  update((d) => {
    const s = d.seen[groupId];
    if (s && s.questionId === questionId && s.count === count) return false;
    d.seen[groupId] = { questionId, count };
  });
}

/** Demo shortcut: release the next question now instead of waiting for tomorrow. */
export function dropNextNow(groupId: string) {
  update((d) => {
    const g = d.groups[groupId];
    if (!g) return false;
    dropQuestion(d, g, Date.now());
  });
  scheduleFor(getDB());
}

/** Logs the guest out, which erases everything on this device. */
export async function resetEverything() {
  await cancelAll();
  await wipe();
}
