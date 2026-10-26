import { useSyncExternalStore } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";

// Everything Askday knows lives in this one object, saved on the device.
// When the app goes online, Firebase replaces this file.

export type NotifPrefs = { daily: boolean; unlocked: boolean; nudges: boolean; reminders: boolean };
export type DropSlot = "morning" | "evening" | "night";

export type Profile = {
  id: string;
  name: string;
  photoURL: string | null;
  notif?: Partial<NotifPrefs>;
  stats?: { answers?: number; bestStreak?: number };
  createdAt?: number;
  sample?: boolean;
};

export type GroupRec = {
  id: string;
  name: string;
  photoURL: string | null;
  memberIds: string[];
  inviteCode: string;
  dropSlot: DropSlot;
  seed: number;
  day: number;
  streak: number;
  bestStreak: number;
  currentId: string | null;
  nextDropAt: number | null;
  createdAt: number;
  lastActivityAt: number;
  /** friend uid → question id they were last nudged on */
  nudged: Record<string, string>;
};

export type QuestionRec = {
  id: string;
  day: number;
  text: string;
  qIndex: number;
  dropAt: number;
  unlockAt: number;
  unlocked: boolean;
  allAnswered: boolean;
  answeredBy: string[];
  topAnswerId: string | null;
  topLikes: number;
};

export type Answer = { id: string; uid: string; text: string; createdAt: number; likes: string[]; replyCount: number };
export type Comment = { id: string; uid: string; text: string; answerId: string | null; createdAt: number };

export type SimEvent = { at: number; kind: "like" | "reply" | "chat"; uid: string; target?: string; text?: string };
/** What the sample friends will do, and when. */
export type Sim = { answerAt: Record<string, number | null>; events: SimEvent[] };

export type DB = {
  me: { uid: string } | null;
  profile: Profile | null;
  people: Record<string, Profile>;
  groups: Record<string, GroupRec>;
  questions: Record<string, Record<string, QuestionRec>>;
  answers: Record<string, Answer[]>;
  comments: Record<string, Comment[]>;
  seen: Record<string, { questionId: string; count: number }>;
  sim: Record<string, Sim>;
};

export const qkey = (groupId: string, questionId: string) => `${groupId}/${questionId}`;

const KEY = "askday.local.v1";

function empty(): DB {
  return { me: null, profile: null, people: {}, groups: {}, questions: {}, answers: {}, comments: {}, seen: {}, sim: {} };
}

let state: DB = empty();
let hydrated = false;
const listeners = new Set<() => void>();

function emit() {
  listeners.forEach((l) => l());
}

export function getDB(): DB {
  return state;
}

export function isHydrated() {
  return hydrated;
}

export function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export async function hydrate(): Promise<void> {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    state = raw ? { ...empty(), ...JSON.parse(raw) } : { ...state };
  } catch {
    state = { ...state };
  }
  hydrated = true;
  emit();
}

let saveTimer: ReturnType<typeof setTimeout> | null = null;
function persist() {
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    AsyncStorage.setItem(KEY, JSON.stringify(state)).catch(() => undefined);
  }, 200);
}

/**
 * Changes the data. The function edits a copy; return false to discard the
 * copy when nothing changed (so the app doesn't re-render for nothing).
 */
export function update(fn: (draft: DB) => boolean | void): boolean {
  const draft: DB = JSON.parse(JSON.stringify(state));
  if (fn(draft) === false) return false;
  state = draft;
  persist();
  emit();
  return true;
}

export async function wipe(): Promise<void> {
  state = empty();
  await AsyncStorage.removeItem(KEY).catch(() => undefined);
  emit();
}

/** The whole store, re-rendering when anything changes. Small data, so that's fine. */
export function useDB(): DB {
  return useSyncExternalStore(subscribe, getDB, getDB);
}
