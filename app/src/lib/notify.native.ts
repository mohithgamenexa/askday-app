import { Platform } from "react-native";
import * as Notifications from "expo-notifications";
import type { DB } from "./store";
import { qkey } from "./store";
import { DEMO_QUESTIONS, questionIndexFor } from "./sample";

// Notifications are scheduled on the phone itself, so they need no server.

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

export async function askNotificationPermission(): Promise<void> {
  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync("default", {
      name: "Daily questions",
      importance: Notifications.AndroidImportance.HIGH,
    });
  }
  const current = await Notifications.getPermissionsAsync();
  if (!current.granted && current.canAskAgain) await Notifications.requestPermissionsAsync();
}

type Planned = { at: number; title: string; body: string; url: string };

function plan(db: DB): Planned[] {
  const me = db.me?.uid;
  const prefs = { daily: true, unlocked: true, reminders: true, ...db.profile?.notif };
  if (!me || !db.profile) return [];
  const now = Date.now();
  const out: Planned[] = [];

  for (const g of Object.values(db.groups)) {
    const url = `/group/${g.id}`;
    if (prefs.daily && g.nextDropAt) {
      const next = DEMO_QUESTIONS[questionIndexFor(g.seed, g.day + 1)];
      out.push({ at: g.nextDropAt, title: `${g.name}: today’s question`, body: next.text, url });
    }
    const q = g.currentId ? db.questions[g.id]?.[g.currentId] : null;
    if (!q || q.unlocked) continue;
    const answered = q.answeredBy.includes(me);
    if (!answered && prefs.reminders) {
      out.push({ at: q.unlockAt - 30 * 60 * 1000, title: `30 minutes left at ${g.name}`, body: `Answer now to keep your ${g.streak}-day streak alive.`, url });
    }
    if (answered && prefs.unlocked) {
      const times = Object.values(db.sim[qkey(g.id, q.id)]?.answerAt ?? {});
      const allPlanned = times.every((t) => t !== null);
      const everyoneAt = allPlanned && times.length ? Math.max(...(times as number[])) : Infinity;
      const at = Math.min(everyoneAt, q.unlockAt);
      out.push({
        at,
        title: everyoneAt <= q.unlockAt ? `Everyone’s in at ${g.name}` : `Answers unlocked at ${g.name}`,
        body: "Go see what everyone said.",
        url,
      });
    }
  }
  return out.filter((p) => p.at > now + 2000);
}

let lastKey = "";
let timer: ReturnType<typeof setTimeout> | null = null;

/** Keeps the phone's scheduled notifications in line with the data. */
export function scheduleFor(db: DB): void {
  if (timer) clearTimeout(timer);
  timer = setTimeout(async () => {
    const planned = plan(db);
    const key = JSON.stringify(planned);
    if (key === lastKey) return;
    lastKey = key;
    try {
      await Notifications.cancelAllScheduledNotificationsAsync();
      for (const p of planned) {
        await Notifications.scheduleNotificationAsync({
          content: { title: p.title, body: p.body, data: { url: p.url } },
          trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date: new Date(p.at) },
        });
      }
    } catch {
      lastKey = "";
    }
  }, 400);
}

export async function cancelAll(): Promise<void> {
  lastKey = "";
  await Notifications.cancelAllScheduledNotificationsAsync().catch(() => undefined);
}
