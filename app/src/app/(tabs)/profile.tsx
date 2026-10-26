import { ReactNode, useState } from "react";
import { Alert, Linking, Platform, ScrollView, View } from "react-native";
import { router } from "expo-router";
import { Avatar, Display, ErrorText, Screen, Tap, Toggle, Txt } from "../../components/ui";
import { ChevronIcon } from "../../components/icons";
import { colors, fonts } from "../../lib/theme";
import { useAuth } from "../../lib/auth";
import { api, DEFAULT_NOTIF, errorMessage, NotifPrefs, saveNotif, useGroups } from "../../lib/data";

const PRIVACY_URL = process.env.EXPO_PUBLIC_PRIVACY_URL ?? "https://askday.app/privacy";

function confirm(title: string, message: string, action: string): Promise<boolean> {
  if (Platform.OS === "web") return Promise.resolve(globalThis.confirm?.(`${title}\n\n${message}`) ?? false);
  return new Promise((resolve) =>
    Alert.alert(title, message, [
      { text: "Cancel", style: "cancel", onPress: () => resolve(false) },
      { text: action, style: "destructive", onPress: () => resolve(true) },
    ]),
  );
}

function Stat({ value, label, color = colors.ink }: { value: number; label: string; color?: string }) {
  return (
    <View style={{ flex: 1, paddingVertical: 14, paddingHorizontal: 10, borderRadius: 16, backgroundColor: colors.card, alignItems: "center", gap: 2 }}>
      <Txt style={{ fontFamily: fonts.display, fontSize: 26, color }}>{value}</Txt>
      <Txt size={12} color={colors.muted}>
        {label}
      </Txt>
    </View>
  );
}

function Row({ children, last, onPress }: { children: ReactNode; last?: boolean; onPress?: () => void }) {
  const style = {
    minHeight: 56,
    paddingHorizontal: 16,
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "space-between" as const,
    borderBottomWidth: last ? 0 : 1,
    borderBottomColor: colors.divider,
  };
  return onPress ? (
    <Tap onPress={onPress} scaleTo={0.99} style={style}>
      {children}
    </Tap>
  ) : (
    <View style={style}>{children}</View>
  );
}

export default function Profile() {
  const { user, profile, signOut } = useAuth();
  const uid = user!.uid;
  const { value: groups } = useGroups(uid);
  const [notif, setNotif] = useState<NotifPrefs>({ ...DEFAULT_NOTIF, ...profile?.notif });
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const setPref = (key: keyof NotifPrefs) => (value: boolean) => {
    const next = { ...notif, [key]: value };
    setNotif(next);
    saveNotif(uid, next).catch((e) => {
      setNotif(notif);
      setError(errorMessage(e));
    });
  };

  const joined = profile?.createdAt && new Date(profile.createdAt).toLocaleDateString([], { month: "long", year: "numeric" });

  const deleteAccount = async () => {
    const ok = await confirm(
      "Delete your account?",
      "This erases your groups, answers and streaks from this phone. It can’t be undone.",
      "Delete account",
    );
    if (!ok) return;
    setDeleting(true);
    try {
      await api.deleteAccount();
    } catch (e) {
      setError(errorMessage(e));
      setDeleting(false);
    }
  };

  const toggles: { key: keyof NotifPrefs; label: string }[] = [
    { key: "daily", label: "Daily question alert" },
    { key: "unlocked", label: "Answers unlocked" },
    { key: "nudges", label: "Nudges from friends" },
    { key: "reminders", label: "Streak reminder" },
  ];

  return (
    <Screen>
      <ScrollView contentContainerStyle={{ paddingHorizontal: 20, paddingTop: 24, paddingBottom: 24, gap: 18 }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 14 }}>
          <Avatar id={uid} name={profile?.name} photoURL={profile?.photoURL} size={72} />
          <View style={{ flex: 1, gap: 2 }}>
            <Display size={26} numberOfLines={1}>
              {profile?.name}
            </Display>
            {joined ? (
              <Txt size={14} color={colors.muted}>
                Joined {joined}
              </Txt>
            ) : null}
          </View>
          <Tap
            onPress={() => router.push("/setup")}
            style={{ height: 40, paddingHorizontal: 14, borderWidth: 1.5, borderColor: colors.border, borderRadius: 12, backgroundColor: colors.card, justifyContent: "center" }}
          >
            <Txt size={13} weight={700}>
              Edit
            </Txt>
          </Tap>
        </View>

        <View style={{ flexDirection: "row", gap: 8 }}>
          <Stat value={profile?.stats?.bestStreak ?? 0} label="Best streak" color={colors.flame} />
          <Stat value={profile?.stats?.answers ?? 0} label="Answers" />
          <Stat value={groups.length} label="Groups" />
        </View>

        <Txt size={12} weight={700} color={colors.muted} style={{ letterSpacing: 1, textTransform: "uppercase" }}>
          Notifications
        </Txt>
        <View style={{ borderRadius: 18, backgroundColor: colors.card }}>
          {toggles.map((t, i) => (
            <Row key={t.key} last={i === toggles.length - 1}>
              <Txt size={15} weight={500} style={{ flex: 1 }}>
                {t.label}
              </Txt>
              <Toggle label={t.label} value={notif[t.key]} onChange={setPref(t.key)} />
            </Row>
          ))}
        </View>
        {Platform.OS === "web" ? (
          <Txt size={13} color={colors.muted} style={{ marginTop: -8 }}>
            Notifications arrive on the Android and iOS apps.
          </Txt>
        ) : null}

        <Txt size={12} weight={700} color={colors.muted} style={{ letterSpacing: 1, textTransform: "uppercase" }}>
          Account
        </Txt>
        <View style={{ borderRadius: 18, backgroundColor: colors.card }}>
          <Row onPress={() => router.push("/manage-groups")}>
            <Txt size={15} weight={500}>
              Manage groups
            </Txt>
            <ChevronIcon />
          </Row>
          <Row onPress={() => Linking.openURL(PRIVACY_URL)}>
            <Txt size={15} weight={500}>
              Privacy policy
            </Txt>
            <ChevronIcon />
          </Row>
          <Row
            onPress={async () => {
              const ok = await confirm(
                "Log out of this guest account?",
                "Guest accounts live on this phone only. Logging out erases your groups, answers and streaks.",
                "Log out",
              );
              if (ok) signOut();
            }}
          >
            <Txt size={15} weight={500}>
              Log out
            </Txt>
          </Row>
          <Row last onPress={deleting ? undefined : deleteAccount}>
            <Txt size={15} weight={700} color={colors.danger}>
              {deleting ? "Deleting…" : "Delete account"}
            </Txt>
          </Row>
        </View>
        <ErrorText message={error} />
      </ScrollView>
    </Screen>
  );
}
