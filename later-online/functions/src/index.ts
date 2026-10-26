import { onSchedule } from "firebase-functions/v2/scheduler";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { getStorage } from "firebase-admin/storage";
import { db, adminAuth, FieldValue, Timestamp, REGION, requireAuth, pushToUsers } from "./lib";
import { dropQuestion, removeMember } from "./groups";

export { createGroup, joinGroup, leaveGroup } from "./groups";
export { onAnswerCreated, onAnswerUpdated, onCommentCreated } from "./answers";

// ------------------------------------------------------------------ ticker

/**
 * Runs every 15 minutes: drops new questions, unlocks answers whose time is up,
 * and reminds people who haven't answered yet.
 */
export const tick = onSchedule({ schedule: "every 15 minutes", region: REGION, timeoutSeconds: 540 }, async () => {
  const now = Timestamp.now();

  const due = await db.collection("groups").where("nextDropAt", "<=", now).limit(500).get();
  for (const g of due.docs) {
    await dropQuestion(g.id, now.toDate()).catch((e) => logger.error("drop failed", g.id, e));
  }

  const toUnlock = await db.collection("groups").where("pendingUnlockAt", "<=", now).limit(500).get();
  for (const g of toUnlock.docs) {
    const current = g.get("current");
    if (!current) continue;
    const batch = db.batch();
    batch.update(g.ref, { "current.unlocked": true, pendingUnlockAt: null, pendingReminderAt: null });
    batch.update(g.ref.collection("questions").doc(current.id), { unlocked: true, unlockedAt: now });
    await batch.commit();
    const answered: string[] = current.answeredBy ?? [];
    const total = g.get("memberCount") ?? answered.length;
    await pushToUsers(answered, "unlocked", {
      title: `Answers unlocked at ${g.get("name")}`,
      body: `${answered.length} of ${total} answered. Go see what they said.`,
      data: { url: `/group/${g.id}` },
    });
  }

  const toRemind = await db.collection("groups").where("pendingReminderAt", "<=", now).limit(500).get();
  for (const g of toRemind.docs) {
    await g.ref.update({ pendingReminderAt: null });
    const current = g.get("current");
    if (!current || current.unlocked) continue;
    const answered: string[] = current.answeredBy ?? [];
    const missing = (g.get("memberIds") ?? []).filter((m: string) => !answered.includes(m));
    await pushToUsers(missing, "reminders", {
      title: `30 minutes left at ${g.get("name")}`,
      body: `Answer now to keep your ${g.get("streak") ?? 0}-day streak alive.`,
      data: { url: `/group/${g.id}` },
    });
  }
});

// ------------------------------------------------------------------- nudge

export const nudge = onCall({ region: REGION }, async (req) => {
  const uid = requireAuth(req.auth?.uid);
  const groupId = String(req.data?.groupId ?? "");
  const target = String(req.data?.targetUid ?? "");
  const g = await db.doc(`groups/${groupId}`).get();
  const members: string[] = g.get("memberIds") ?? [];
  if (!g.exists || !members.includes(uid) || !members.includes(target) || target === uid) {
    throw new HttpsError("permission-denied", "You can only nudge people in your group.");
  }
  const current = g.get("current");
  if (!current) throw new HttpsError("failed-precondition", "There’s no question to answer yet.");
  if ((current.answeredBy ?? []).includes(target)) {
    throw new HttpsError("failed-precondition", "They’ve already answered.");
  }

  try {
    await db
      .doc(`groups/${groupId}/questions/${current.id}/nudges/${uid}_${target}`)
      .create({ from: uid, to: target, at: FieldValue.serverTimestamp() });
  } catch {
    throw new HttpsError("already-exists", "You’ve already nudged them today.");
  }

  const me = await db.doc(`users/${uid}`).get();
  await pushToUsers([target], "nudges", {
    title: `${me.get("name") ?? "A friend"} nudged you`,
    body: `${g.get("name")} is waiting on your answer.`,
    data: { url: `/group/${groupId}` },
  });
  return { ok: true };
});

// ------------------------------------------------------------------ account

export const deleteAccount = onCall({ region: REGION }, async (req) => {
  const uid = requireAuth(req.auth?.uid);
  const groups = await db.collection("groups").where("memberIds", "array-contains", uid).get();
  for (const g of groups.docs) await removeMember(g.id, uid);
  for (const kind of ["answers", "comments"]) {
    const mine = await db.collectionGroup(kind).where("uid", "==", uid).get();
    for (let i = 0; i < mine.docs.length; i += 400) {
      const batch = db.batch();
      mine.docs.slice(i, i + 400).forEach((d) => batch.delete(d.ref));
      await batch.commit();
    }
  }
  await db.recursiveDelete(db.doc(`users/${uid}`));
  await getStorage().bucket().deleteFiles({ prefix: `avatars/${uid}/` }).catch(() => undefined);
  await adminAuth.deleteUser(uid);
  return { ok: true };
});
