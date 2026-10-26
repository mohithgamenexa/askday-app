import { initializeApp, getApps } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { QUESTIONS } from "./questions";

if (!getApps().length) initializeApp();

export const db = getFirestore();
export const adminAuth = getAuth();
export { FieldValue, Timestamp };

export const REGION = "us-central1";

// ---------------------------------------------------------------- schedule

export type DropSlot = "morning" | "evening" | "night";
export const SLOT_HOUR: Record<DropSlot, number> = { morning: 9, evening: 19, night: 22 };

/** How long after the drop answers unlock even if someone hasn't answered. */
export const UNLOCK_AFTER_MS = 2 * 60 * 60 * 1000;
/** Reminder goes out this long before the unlock. */
export const REMINDER_BEFORE_UNLOCK_MS = 30 * 60 * 1000;

function tzOffsetMs(timeZone: string, at: Date): number {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    hourCycle: "h23",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).formatToParts(at);
  const get = (t: string) => Number(parts.find((p) => p.type === t)?.value);
  const asUtc = Date.UTC(get("year"), get("month") - 1, get("day"), get("hour"), get("minute"), get("second"));
  return asUtc - at.getTime();
}

export function safeTimeZone(tz: unknown): string {
  if (typeof tz !== "string") return "UTC";
  try {
    new Intl.DateTimeFormat("en-US", { timeZone: tz });
    return tz;
  } catch {
    return "UTC";
  }
}

/** Next moment strictly after `after` when the local clock in `timeZone` reads `hour`:00. */
export function nextLocalHour(timeZone: string, hour: number, after: Date): Date {
  for (let addDays = 0; addDays < 3; addDays++) {
    const probe = new Date(after.getTime() + addDays * 86_400_000);
    const local = new Date(probe.getTime() + tzOffsetMs(timeZone, probe));
    const guessUtc = Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate(), hour);
    const candidate = new Date(guessUtc - tzOffsetMs(timeZone, new Date(guessUtc)));
    if (candidate.getTime() > after.getTime() + 60_000) return candidate;
  }
  return new Date(after.getTime() + 86_400_000);
}

// --------------------------------------------------------------- questions

function mulberry32(seed: number) {
  return () => {
    seed |= 0;
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** Deterministic per-group shuffle: day N always maps to the same question. */
export function questionForDay(seed: number, day: number): string {
  const order = QUESTIONS.map((_, i) => i);
  const rand = mulberry32(seed + Math.floor((day - 1) / QUESTIONS.length));
  for (let i = order.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [order[i], order[j]] = [order[j], order[i]];
  }
  return QUESTIONS[order[(day - 1) % QUESTIONS.length]];
}

// -------------------------------------------------------------------- misc

export function requireAuth(uid: string | undefined): string {
  if (!uid) throw new HttpsError("unauthenticated", "Sign in to continue.");
  return uid;
}

export function cleanText(value: unknown, max: number, field: string): string {
  if (typeof value !== "string") throw new HttpsError("invalid-argument", `${field} is required.`);
  const text = value.trim().replace(/\s+/g, " ");
  if (!text) throw new HttpsError("invalid-argument", `${field} can’t be empty.`);
  if (text.length > max) throw new HttpsError("invalid-argument", `${field} must be ${max} characters or fewer.`);
  return text;
}

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
export function randomCode(length = 6): string {
  let out = "";
  for (let i = 0; i < length; i++) out += CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)];
  return out;
}

// -------------------------------------------------------------------- push

export type NotifKind = "daily" | "unlocked" | "nudges" | "reminders";

type PushMessage = { title: string; body: string; data?: Record<string, string> };

/**
 * Sends a push notification through Expo's push service to every device of
 * the given users who have that kind of notification switched on.
 */
export async function pushToUsers(uids: string[], kind: NotifKind, message: PushMessage): Promise<void> {
  const unique = [...new Set(uids)].filter(Boolean);
  if (!unique.length) return;

  const userSnaps = await db.getAll(...unique.map((u) => db.doc(`users/${u}`)));
  const allowed = userSnaps
    .filter((s) => s.exists && s.get(`notif.${kind}`) !== false)
    .map((s) => s.id);
  if (!allowed.length) return;

  const tokenSnaps = await db.getAll(...allowed.map((u) => db.doc(`users/${u}/private/push`)));
  const tokenOwner = new Map<string, string>();
  for (const snap of tokenSnaps) {
    const tokens: string[] = snap.exists ? snap.get("tokens") ?? [] : [];
    const uid = snap.ref.parent.parent!.id;
    for (const t of tokens) tokenOwner.set(t, uid);
  }
  const tokens = [...tokenOwner.keys()];
  if (!tokens.length) return;

  for (let i = 0; i < tokens.length; i += 100) {
    const batch = tokens.slice(i, i + 100);
    try {
      const res = await fetch("https://exp.host/--/api/v2/push/send", {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(
          batch.map((to) => ({ to, sound: "default", title: message.title, body: message.body, data: message.data ?? {} })),
        ),
      });
      const json = (await res.json()) as { data?: Array<{ status: string; details?: { error?: string } }> };
      // Clean up tokens for uninstalled apps so we stop sending to them.
      const dead = (json.data ?? [])
        .map((r, idx) => (r.status === "error" && r.details?.error === "DeviceNotRegistered" ? batch[idx] : null))
        .filter((t): t is string => !!t);
      await Promise.all(
        dead.map((t) =>
          db.doc(`users/${tokenOwner.get(t)}/private/push`).set({ tokens: FieldValue.arrayRemove(t) }, { merge: true }),
        ),
      );
    } catch (err) {
      logger.warn("Push send failed", err);
    }
  }
}
