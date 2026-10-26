import { Linking, Platform, Share } from "react-native";
import * as Clipboard from "expo-clipboard";
import AsyncStorage from "@react-native-async-storage/async-storage";

const BASE = (process.env.EXPO_PUBLIC_INVITE_BASE_URL ?? "https://askday.app").replace(/\/$/, "");

export function inviteUrl(code: string) {
  return `${BASE}/j/${code}`;
}
export function inviteDisplay(code: string) {
  return inviteUrl(code).replace(/^https?:\/\//, "");
}
function inviteMessage(groupName: string, code: string) {
  return `Join “${groupName}” on Askday. One question a day, answered together: ${inviteUrl(code)}`;
}

export async function copyInvite(code: string) {
  await Clipboard.setStringAsync(inviteUrl(code));
}

export async function shareInvite(target: "whatsapp" | "instagram" | "messages" | "more", groupName: string, code: string) {
  const text = inviteMessage(groupName, code);
  const encoded = encodeURIComponent(text);

  if (target === "whatsapp") {
    const url = Platform.OS === "web" ? `https://wa.me/?text=${encoded}` : `whatsapp://send?text=${encoded}`;
    if (Platform.OS === "web" || (await Linking.canOpenURL(url))) return Linking.openURL(url);
  }
  if (target === "messages" && Platform.OS !== "web") {
    const url = Platform.OS === "ios" ? `sms:&body=${encoded}` : `sms:?body=${encoded}`;
    return Linking.openURL(url);
  }
  if (target === "instagram") {
    // Instagram has no way to pre-fill a message, so copy the link and open the app.
    await Clipboard.setStringAsync(inviteUrl(code));
    const url = Platform.OS === "web" ? "https://www.instagram.com/direct/inbox/" : "instagram://direct-inbox";
    if (Platform.OS === "web" || (await Linking.canOpenURL(url))) return Linking.openURL(url);
  }
  if (Platform.OS === "web") {
    const nav = globalThis.navigator as Navigator | undefined;
    if (nav?.share) return nav.share({ text });
    return Clipboard.setStringAsync(inviteUrl(code));
  }
  return Share.share({ message: text });
}

// An invite link opened before signing in is remembered and used right after.
const PENDING_KEY = "askday.pendingInvite";
export const pendingInvite = {
  set: (code: string) => AsyncStorage.setItem(PENDING_KEY, code),
  take: async () => {
    const code = await AsyncStorage.getItem(PENDING_KEY);
    if (code) await AsyncStorage.removeItem(PENDING_KEY);
    return code;
  },
};
