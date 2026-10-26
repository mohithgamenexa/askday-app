import { useState } from "react";
import { Alert, Platform, ScrollView, View } from "react-native";
import { router } from "expo-router";
import { AvatarStack, BackButton, Display, ErrorText, Screen, Tap, Txt } from "../components/ui";
import { colors } from "../lib/theme";
import { useAuth } from "../lib/auth";
import { api, errorMessage, useGroups, useProfiles } from "../lib/data";

function confirmLeave(name: string): Promise<boolean> {
  const message = "This deletes the group and its answers from this phone.";
  if (Platform.OS === "web") return Promise.resolve(globalThis.confirm?.(`Leave ${name}?\n\n${message}`) ?? false);
  return new Promise((resolve) =>
    Alert.alert(`Leave ${name}?`, message, [
      { text: "Cancel", style: "cancel", onPress: () => resolve(false) },
      { text: "Leave group", style: "destructive", onPress: () => resolve(true) },
    ]),
  );
}

export default function ManageGroups() {
  const { user } = useAuth();
  const { value: groups } = useGroups(user!.uid);
  const profiles = useProfiles(groups.flatMap((g) => g.memberIds.slice(0, 4)));
  const [error, setError] = useState<string | null>(null);

  return (
    <Screen edges={["top", "bottom"]}>
      <ScrollView contentContainerStyle={{ paddingHorizontal: 20, paddingTop: 24, paddingBottom: 24, gap: 18 }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 12 }}>
          <BackButton onPress={() => router.back()} />
          <Display size={26}>Manage groups</Display>
        </View>
        <ErrorText message={error} />
        {groups.map((g) => (
          <View key={g.id} style={{ padding: 16, borderRadius: 18, backgroundColor: colors.card, gap: 12 }}>
            <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
              <Txt size={17} weight={700} style={{ flex: 1 }} numberOfLines={1}>
                {g.name}
              </Txt>
              <AvatarStack people={g.memberIds.map((id) => profiles.get(id) ?? { id, name: "", photoURL: null })} />
            </View>
            <Txt size={13} color={colors.muted}>
              {g.memberCount} members, including sample friends
            </Txt>
            <View style={{ flexDirection: "row", gap: 8 }}>
              <Tap
                onPress={async () => {
                  if (!(await confirmLeave(g.name))) return;
                  try {
                    await api.leaveGroup({ groupId: g.id });
                  } catch (e) {
                    setError(errorMessage(e));
                  }
                }}
                style={{ flex: 1, height: 44, borderRadius: 12, borderWidth: 1.5, borderColor: colors.border, alignItems: "center", justifyContent: "center" }}
              >
                <Txt size={14} weight={700} color={colors.danger}>
                  Leave group
                </Txt>
              </Tap>
            </View>
          </View>
        ))}
        {!groups.length ? (
          <Txt size={15} color={colors.muted}>
            You’re not in any groups yet.
          </Txt>
        ) : null}
      </ScrollView>
    </Screen>
  );
}
