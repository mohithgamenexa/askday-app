import type { DB } from "./store";

// Web: browsers don't get Askday notifications. The phone version is notify.native.ts.
export function scheduleFor(_db: DB): void {}
export async function cancelAll(): Promise<void> {}
export async function askNotificationPermission(): Promise<void> {}
