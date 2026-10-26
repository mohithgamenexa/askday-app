import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { db, FieldValue, REGION, pushToUsers } from "./lib";

const ANSWER_PATH = "groups/{groupId}/questions/{questionId}/answers/{uid}";
const COMMENT_PATH = "groups/{groupId}/questions/{questionId}/comments/{commentId}";

/**
 * When someone answers: record it on the question, unlock everyone's answers
 * once the whole group is in, and extend the group streak.
 */
export const onAnswerCreated = onDocumentCreated({ document: ANSWER_PATH, region: REGION }, async (event) => {
  const { groupId, questionId, uid } = event.params;
  const groupRef = db.doc(`groups/${groupId}`);
  const questionRef = groupRef.collection("questions").doc(questionId);

  let result: { unlockedNow: boolean; answeredBy: string[]; groupName: string; streak: number; members: string[] } | null =
    null;

  await db.runTransaction(async (tx) => {
    const [gSnap, qSnap] = await Promise.all([tx.get(groupRef), tx.get(questionRef)]);
    if (!gSnap.exists || !qSnap.exists) return;
    const g = gSnap.data()!;
    const q = qSnap.data()!;
    const members: string[] = g.memberIds ?? [];
    if (!members.includes(uid)) return;

    const answeredBy: string[] = q.answeredBy ?? [];
    if (answeredBy.includes(uid)) return;
    answeredBy.push(uid);

    const allAnswered = members.every((m) => answeredBy.includes(m));
    const unlockedNow = allAnswered && !q.unlocked;
    const isCurrent = g.current?.id === questionId;
    const streakUp = allAnswered && !q.allAnswered && isCurrent;
    const streak = streakUp ? (g.streak ?? 0) + 1 : g.streak ?? 0;

    tx.update(questionRef, {
      answeredBy,
      answerCount: answeredBy.length,
      allAnswered: allAnswered || q.allAnswered === true,
      ...(unlockedNow ? { unlocked: true, unlockedAt: FieldValue.serverTimestamp() } : {}),
    });

    const groupUpdate: Record<string, unknown> = { lastActivityAt: FieldValue.serverTimestamp() };
    if (isCurrent) {
      groupUpdate["current.answeredBy"] = answeredBy;
      groupUpdate["current.answerCount"] = answeredBy.length;
      if (allAnswered) groupUpdate["current.allAnswered"] = true;
      if (unlockedNow) {
        groupUpdate["current.unlocked"] = true;
        groupUpdate.pendingUnlockAt = null;
        groupUpdate.pendingReminderAt = null;
      }
    }
    if (streakUp) {
      groupUpdate.streak = streak;
      groupUpdate.bestStreak = Math.max(g.bestStreak ?? 0, streak);
    }
    tx.update(groupRef, groupUpdate);

    tx.set(db.doc(`users/${uid}`), { stats: { answers: FieldValue.increment(1) } }, { merge: true });

    result = { unlockedNow, answeredBy, groupName: g.name, streak: streakUp ? streak : -1, members };
  });

  const r = result as {
    unlockedNow: boolean;
    answeredBy: string[];
    groupName: string;
    streak: number;
    members: string[];
  } | null;
  if (!r) return;

  // Personal best streaks for everyone in the group.
  if (r.streak > 0) {
    const snaps = await db.getAll(...r.members.map((m) => db.doc(`users/${m}`)));
    const batch = db.batch();
    for (const s of snaps) {
      if ((s.get("stats.bestStreak") ?? 0) < r.streak) {
        batch.set(s.ref, { stats: { bestStreak: r.streak } }, { merge: true });
      }
    }
    await batch.commit();
  }

  if (r.unlockedNow) {
    await pushToUsers(
      r.answeredBy.filter((m) => m !== uid),
      "unlocked",
      {
        title: `Everyone’s in at ${r.groupName}`,
        body: r.streak > 0 ? `Answers are unlocked. Your streak is now ${r.streak} days.` : "Answers are unlocked. Go see what everyone said.",
        data: { url: `/group/${groupId}` },
      },
    );
  }
});

/** Keeps track of the most-liked answer, shown in History. */
export const onAnswerUpdated = onDocumentUpdated({ document: ANSWER_PATH, region: REGION }, async (event) => {
  const after = event.data?.after.data();
  const before = event.data?.before.data();
  if (!after || !before) return;
  const likes = (after.likes ?? []).length;
  if (likes === (before.likes ?? []).length) return;

  const { groupId, questionId, uid } = event.params;
  const questionRef = db.doc(`groups/${groupId}/questions/${questionId}`);
  await db.runTransaction(async (tx) => {
    const q = await tx.get(questionRef);
    if (!q.exists) return;
    if (likes > (q.get("topLikes") ?? 0) || q.get("topAnswerId") === uid) {
      tx.update(questionRef, { topAnswerId: uid, topLikes: likes });
    }
  });
});

/** Counts replies on an answer. */
export const onCommentCreated = onDocumentCreated({ document: COMMENT_PATH, region: REGION }, async (event) => {
  const data = event.data?.data();
  if (!data?.answerId) return;
  const { groupId, questionId } = event.params;
  await db
    .doc(`groups/${groupId}/questions/${questionId}/answers/${data.answerId}`)
    .update({ replyCount: FieldValue.increment(1) })
    .catch(() => undefined);
});
