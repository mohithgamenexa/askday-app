import { onCall, HttpsError } from "firebase-functions/v2/https";
import {
  db,
  FieldValue,
  Timestamp,
  REGION,
  SLOT_HOUR,
  UNLOCK_AFTER_MS,
  REMINDER_BEFORE_UNLOCK_MS,
  DropSlot,
  nextLocalHour,
  questionForDay,
  requireAuth,
  cleanText,
  randomCode,
  safeTimeZone,
  pushToUsers,
} from "./lib";

const MAX_MEMBERS = 30;
const MAX_GROUPS_PER_USER = 20;

export function questionId(day: number): string {
  return `day-${String(day).padStart(5, "0")}`;
}

/**
 * Releases the next question for a group. Resets the streak if the previous
 * question wasn't answered by everyone, and schedules the next drop.
 */
export async function dropQuestion(groupId: string, now = new Date()): Promise<void> {
  const groupRef = db.doc(`groups/${groupId}`);
  let notify: { members: string[]; name: string; text: string; qid: string } | null = null;

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(groupRef);
    if (!snap.exists) return;
    const g = snap.data()!;
    const nextDropAt: Timestamp | undefined = g.nextDropAt;
    // Another run already handled this drop.
    if (g.current && nextDropAt && nextDropAt.toMillis() > now.getTime()) return;

    const prev = g.current as { id: string; allAnswered: boolean; unlocked: boolean } | null;
    const day = (g.day ?? 0) + 1;
    const text = questionForDay(g.seed ?? 1, day);
    const qid = questionId(day);
    const dropAt = Timestamp.fromDate(now);
    const unlockAt = Timestamp.fromMillis(now.getTime() + UNLOCK_AFTER_MS);
    const slot = (g.dropSlot as DropSlot) ?? "evening";
    const tz = safeTimeZone(g.timeZone);

    if (prev && !prev.unlocked) {
      tx.update(groupRef.collection("questions").doc(prev.id), { unlocked: true, unlockedAt: dropAt });
    }

    tx.set(groupRef.collection("questions").doc(qid), {
      day,
      text,
      dropAt,
      unlockAt,
      unlocked: false,
      unlockedAt: null,
      answeredBy: [],
      answerCount: 0,
      memberCount: g.memberCount ?? (g.memberIds ?? []).length,
      allAnswered: false,
      topAnswerId: null,
      topLikes: 0,
    });

    tx.update(groupRef, {
      day,
      streak: prev && !prev.allAnswered ? 0 : g.streak ?? 0,
      current: { id: qid, day, text, dropAt, unlockAt, unlocked: false, answeredBy: [], answerCount: 0, allAnswered: false },
      nextDropAt: Timestamp.fromDate(nextLocalHour(tz, SLOT_HOUR[slot], now)),
      pendingUnlockAt: unlockAt,
      pendingReminderAt: Timestamp.fromMillis(unlockAt.toMillis() - REMINDER_BEFORE_UNLOCK_MS),
      lastActivityAt: dropAt,
    });

    notify = { members: g.memberIds ?? [], name: g.name, text, qid };
  });

  const n = notify as { members: string[]; name: string; text: string; qid: string } | null;
  if (n) {
    await pushToUsers(n.members, "daily", {
      title: `${n.name}: today’s question`,
      body: n.text,
      data: { url: `/group/${groupId}` },
    });
  }
}

export const createGroup = onCall({ region: REGION }, async (req) => {
  const uid = requireAuth(req.auth?.uid);
  const name = cleanText(req.data?.name, 40, "Group name");
  const slot: DropSlot = ["morning", "evening", "night"].includes(req.data?.dropSlot) ? req.data.dropSlot : "evening";
  const timeZone = safeTimeZone(req.data?.timeZone);

  const mine = await db.collection("groups").where("memberIds", "array-contains", uid).count().get();
  if (mine.data().count >= MAX_GROUPS_PER_USER) {
    throw new HttpsError("resource-exhausted", `You can be in up to ${MAX_GROUPS_PER_USER} groups.`);
  }

  const groupRef = db.collection("groups").doc();
  let code = "";
  for (let attempt = 0; attempt < 6 && !code; attempt++) {
    const candidate = randomCode();
    try {
      await db.doc(`inviteCodes/${candidate}`).create({ groupId: groupRef.id, createdAt: FieldValue.serverTimestamp() });
      code = candidate;
    } catch {
      // Code already taken, try another.
    }
  }
  if (!code) throw new HttpsError("internal", "Couldn’t create an invite code. Try again.");

  await groupRef.set({
    name,
    photoURL: null,
    ownerId: uid,
    memberIds: [uid],
    memberCount: 1,
    inviteCode: code,
    dropSlot: slot,
    timeZone,
    seed: Math.floor(Math.random() * 2 ** 31),
    day: 0,
    streak: 0,
    bestStreak: 0,
    current: null,
    nextDropAt: null,
    pendingUnlockAt: null,
    pendingReminderAt: null,
    createdAt: FieldValue.serverTimestamp(),
    lastActivityAt: FieldValue.serverTimestamp(),
  });

  // Give a brand-new group its first question right away.
  await dropQuestion(groupRef.id);
  return { groupId: groupRef.id, inviteCode: code };
});

export const joinGroup = onCall({ region: REGION }, async (req) => {
  const uid = requireAuth(req.auth?.uid);
  const code = String(req.data?.code ?? "").trim().toUpperCase();
  if (!/^[A-Z0-9]{4,10}$/.test(code)) throw new HttpsError("invalid-argument", "Enter a valid invite code.");

  const invite = await db.doc(`inviteCodes/${code}`).get();
  if (!invite.exists) throw new HttpsError("not-found", "That invite code doesn’t match any group.");
  const groupRef = db.doc(`groups/${invite.get("groupId")}`);

  await db.runTransaction(async (tx) => {
    const g = await tx.get(groupRef);
    if (!g.exists) throw new HttpsError("not-found", "This group no longer exists.");
    const members: string[] = g.get("memberIds") ?? [];
    if (members.includes(uid)) return;
    if (members.length >= MAX_MEMBERS) throw new HttpsError("resource-exhausted", "This group is full.");
    tx.update(groupRef, {
      memberIds: FieldValue.arrayUnion(uid),
      memberCount: members.length + 1,
    });
  });

  return { groupId: groupRef.id };
});

export const leaveGroup = onCall({ region: REGION }, async (req) => {
  const uid = requireAuth(req.auth?.uid);
  const groupId = String(req.data?.groupId ?? "");
  if (!groupId) throw new HttpsError("invalid-argument", "groupId is required.");
  await removeMember(groupId, uid);
  return { ok: true };
});

export async function removeMember(groupId: string, uid: string): Promise<void> {
  const groupRef = db.doc(`groups/${groupId}`);
  let deleteGroup = false;
  let inviteCode = "";

  await db.runTransaction(async (tx) => {
    const g = await tx.get(groupRef);
    if (!g.exists) return;
    const members: string[] = (g.get("memberIds") ?? []).filter((m: string) => m !== uid);
    inviteCode = g.get("inviteCode");
    if (!members.length) {
      deleteGroup = true;
      return;
    }
    const update: Record<string, unknown> = {
      memberIds: members,
      memberCount: members.length,
      ownerId: g.get("ownerId") === uid ? members[0] : g.get("ownerId"),
    };
    const current = g.get("current");
    if (current) {
      const answered: string[] = current.answeredBy ?? [];
      const everyoneIn = members.every((m) => answered.includes(m));
      if (everyoneIn && !current.allAnswered) {
        update["current.allAnswered"] = true;
        update["current.unlocked"] = true;
        update.pendingUnlockAt = null;
        update.pendingReminderAt = null;
        tx.update(groupRef.collection("questions").doc(current.id), { allAnswered: true, unlocked: true });
      }
    }
    tx.update(groupRef, update);
  });

  if (deleteGroup) {
    await db.recursiveDelete(groupRef);
    if (inviteCode) await db.doc(`inviteCodes/${inviteCode}`).delete().catch(() => undefined);
  }
}
