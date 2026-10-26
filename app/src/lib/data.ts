import { useMemo } from "react";
import { Answer, Comment, DB, DropSlot, GroupRec, NotifPrefs, Profile, qkey, QuestionRec, useDB } from "./store";
import * as engine from "./engine";

export type { Answer, Comment, DropSlot, NotifPrefs, Profile };
export { DEFAULT_NOTIF, errorMessage } from "./engine";

export type Question = QuestionRec & { answerCount: number; memberCount: number };
export type Group = Omit<GroupRec, "currentId" | "nudged" | "seed"> & { memberCount: number; current: Question | null };

export const SLOTS: { id: DropSlot; label: string; time: string }[] = [
  { id: "morning", label: "Morning", time: "9:00 AM" },
  { id: "evening", label: "Evening", time: "7:00 PM" },
  { id: "night", label: "Night", time: "10:00 PM" },
];

function toQuestion(q: QuestionRec, memberCount: number): Question {
  return { ...q, answerCount: q.answeredBy.length, memberCount };
}

function toGroup(db: DB, g: GroupRec): Group {
  const cur = g.currentId ? db.questions[g.id]?.[g.currentId] : null;
  const { currentId: _c, nudged: _n, seed: _s, ...rest } = g;
  return { ...rest, memberCount: g.memberIds.length, current: cur ? toQuestion(cur, g.memberIds.length) : null };
}

const done = <T,>(value: T) => ({ value, loading: false });

// ------------------------------------------------------------------- hooks

export function useGroups(uid: string | undefined) {
  const db = useDB();
  return useMemo(() => {
    if (!uid) return done<Group[]>([]);
    const groups = Object.values(db.groups)
      .filter((g) => g.memberIds.includes(uid))
      .map((g) => toGroup(db, g))
      .sort((a, b) => b.lastActivityAt - a.lastActivityAt);
    return done(groups);
  }, [db, uid]);
}

export function useGroup(id: string | undefined) {
  const db = useDB();
  return useMemo(() => done(id && db.groups[id] ? toGroup(db, db.groups[id]) : null), [db, id]);
}

export function useQuestion(groupId: string | undefined, questionId: string | undefined) {
  const db = useDB();
  return useMemo(() => {
    const q = groupId && questionId ? db.questions[groupId]?.[questionId] : null;
    return done(q ? toQuestion(q, db.groups[groupId!]?.memberIds.length ?? 0) : null);
  }, [db, groupId, questionId]);
}

export function useHistory(groupId: string | undefined) {
  const db = useDB();
  return useMemo(() => {
    if (!groupId) return done<Question[]>([]);
    const n = db.groups[groupId]?.memberIds.length ?? 0;
    return done(Object.values(db.questions[groupId] ?? {}).sort((a, b) => b.day - a.day).map((q) => toQuestion(q, n)));
  }, [db, groupId]);
}

export function useMyAnswer(groupId: string | undefined, questionId: string | undefined, uid: string | undefined) {
  const db = useDB();
  return useMemo(() => {
    if (!groupId || !questionId || !uid) return done<Answer | null>(null);
    return done(db.answers[qkey(groupId, questionId)]?.find((a) => a.uid === uid) ?? null);
  }, [db, groupId, questionId, uid]);
}

/** Everyone's answers. Only shown once you've answered and the question has unlocked. */
export function useAnswers(groupId: string | undefined, questionId: string | undefined, enabled: boolean) {
  const db = useDB();
  return useMemo(() => {
    if (!groupId || !questionId || !enabled) return done<Answer[]>([]);
    return done([...(db.answers[qkey(groupId, questionId)] ?? [])].sort((a, b) => a.createdAt - b.createdAt));
  }, [db, groupId, questionId, enabled]);
}

export function useComments(groupId: string | undefined, questionId: string | undefined, enabled: boolean) {
  const db = useDB();
  return useMemo(() => {
    if (!groupId || !questionId || !enabled) return done<Comment[]>([]);
    return done(db.comments[qkey(groupId, questionId)] ?? []);
  }, [db, groupId, questionId, enabled]);
}

export function useProfiles(ids: string[]) {
  const db = useDB();
  const key = [...new Set(ids)].sort().join(",");
  return useMemo(() => {
    const map = new Map<string, Profile>();
    for (const id of key ? key.split(",") : []) {
      const p = db.profile?.id === id ? db.profile : db.people[id];
      map.set(id, p ?? { id, name: "", photoURL: null });
    }
    return map;
  }, [db, key]);
}

export function useSeen(_uid: string | undefined) {
  const db = useDB();
  return done(db.seen);
}

/** The most-liked answer, shown in History once you've answered. */
export function useTopAnswer(groupId: string, q: Question, uid: string) {
  const db = useDB();
  return useMemo(() => {
    if (!q.unlocked || !q.answeredBy.includes(uid) || !q.topAnswerId) return null;
    const a = db.answers[qkey(groupId, q.id)]?.find((x) => x.id === q.topAnswerId);
    return a ? { uid: a.uid, text: a.text } : null;
  }, [db, groupId, q, uid]);
}

// ----------------------------------------------------------------- actions

export const submitAnswer = async (groupId: string, questionId: string, uid: string, text: string) =>
  engine.submitAnswer(groupId, questionId, uid, text);
export const toggleLike = async (groupId: string, questionId: string, answer: Answer, uid: string) =>
  engine.toggleLike(groupId, questionId, answer.id, uid);
export const postComment = async (groupId: string, questionId: string, uid: string, text: string, answerId: string | null) =>
  engine.postComment(groupId, questionId, uid, text, answerId);
export const markSeen = async (_uid: string, groupId: string, questionId: string, count: number) =>
  engine.markSeen(groupId, questionId, count);
export const saveProfile = async (uid: string, data: { name: string; photoURL: string | null }, isNew: boolean) =>
  engine.saveProfile(uid, data, isNew);
export const saveNotif = async (_uid: string, notif: NotifPrefs) => engine.saveNotif(notif);

export const api = {
  createGroup: async (input: { name: string; dropSlot: DropSlot }) => engine.createGroup(input),
  joinGroup: async ({ code }: { code: string }): Promise<{ groupId: string }> => engine.joinGroup(code),
  leaveGroup: async ({ groupId }: { groupId: string }) => engine.leaveGroup(groupId),
  nudge: async ({ groupId, targetUid }: { groupId: string; targetUid: string }) => engine.nudge(groupId, targetUid),
  deleteAccount: async () => engine.resetEverything(),
};

// ------------------------------------------------------------------ format

export function formatTime(ms?: number | null): string {
  if (!ms) return "";
  return new Date(ms).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

export function formatDayHeader(ms?: number | null): string {
  if (!ms) return "";
  return new Date(ms).toLocaleDateString([], { weekday: "long", month: "short", day: "numeric" });
}

export function formatShortDate(ms?: number | null): string {
  if (!ms) return "";
  return new Date(ms).toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" });
}

export function isToday(ms?: number | null): boolean {
  if (!ms) return false;
  return new Date(ms).toDateString() === new Date().toDateString();
}
