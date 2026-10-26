import { ScrollView, View } from "react-native";
import { router } from "expo-router";
import Animated, { FadeInDown } from "react-native-reanimated";
import { AvatarStack, Button, Display, IconButton, Loading, Pill, Screen, StreakChip, Tap, Txt } from "../../components/ui";
import { PlusIcon } from "../../components/icons";
import { colors, fonts, shadow } from "../../lib/theme";
import { useAuth } from "../../lib/auth";
import { formatTime, Group, useGroups, useProfiles, useSeen } from "../../lib/data";

type Status = { label: string; bg: string; fg: string; needsAnswer: boolean };

function statusFor(g: Group, uid: string, seen?: { questionId: string; count: number }): Status {
  const q = g.current;
  if (!q) return { label: "Starting soon", bg: colors.track, fg: colors.muted2, needsAnswer: false };
  const answered = q.answeredBy.includes(uid);
  if (!answered) return { label: "Answer now", bg: colors.accent, fg: colors.white, needsAnswer: true };
  if (!q.unlocked) {
    const waiting = Math.max(g.memberCount - q.answeredBy.length, 0);
    return { label: `Waiting on ${waiting}`, bg: colors.track, fg: colors.muted2, needsAnswer: false };
  }
  const seenCount = seen?.questionId === q.id ? seen.count : 1; // you've always seen your own
  const fresh = q.answerCount - seenCount;
  if (fresh > 0) {
    return { label: `${fresh} new answer${fresh === 1 ? "" : "s"}`, bg: colors.successBg, fg: colors.successInk, needsAnswer: false };
  }
  return { label: "Answered", bg: colors.track, fg: colors.muted2, needsAnswer: false };
}

export default function Groups() {
  const { user } = useAuth();
  const uid = user!.uid;
  const { value: groups, loading } = useGroups(uid);
  const { value: seen } = useSeen(uid);
  const profiles = useProfiles(groups.flatMap((g) => g.memberIds.slice(0, 4)));

  if (loading) return <Loading />;

  const statuses = groups.map((g) => statusFor(g, uid, seen[g.id]));
  const waiting = statuses.filter((s) => s.needsAnswer).length;
  const nextDrop = groups
    .map((g) => g.nextDropAt)
    .filter((t): t is number => !!t)
    .sort((a, b) => a - b)[0];

  return (
    <Screen>
      <ScrollView contentContainerStyle={{ paddingHorizontal: 20, paddingTop: 24, paddingBottom: 24, gap: 18 }}>
        <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
          <Display size={32}>Your groups</Display>
          <IconButton label="Create or join a group" bg={colors.accent} onPress={() => router.push("/group/new")}>
            <PlusIcon />
          </IconButton>
        </View>

        {groups.length ? (
          <View style={{ padding: 16, borderRadius: 18, backgroundColor: colors.ink, flexDirection: "row", alignItems: "center", gap: 14 }}>
            <View style={{ width: 44, height: 44, borderRadius: 12, backgroundColor: colors.accent, alignItems: "center", justifyContent: "center" }}>
              <Txt style={{ fontFamily: fonts.display, fontSize: 22, color: colors.white }}>?</Txt>
            </View>
            <View style={{ flex: 1, gap: 2 }}>
              <Txt size={15} weight={700} color={colors.white}>
                {waiting ? "Today’s questions are here" : "You’re all caught up"}
              </Txt>
              <Txt size={13} color={colors.darkMuted}>
                {waiting
                  ? `${waiting} group${waiting === 1 ? " is" : "s are"} waiting on your answer`
                  : nextDrop
                    ? `Next question drops at ${formatTime(nextDrop)}`
                    : "New questions drop every day"}
              </Txt>
            </View>
          </View>
        ) : null}

        {groups.length === 0 ? (
          <View style={[{ padding: 24, borderRadius: 22, backgroundColor: colors.card, gap: 14 }, shadow.card]}>
            <Display size={24} style={{ lineHeight: 28 }}>
              Start a group with your people
            </Display>
            <Txt size={15} color={colors.muted} style={{ lineHeight: 22 }}>
              Create a group and invite friends, or join one with an invite code. Your first question arrives right away.
            </Txt>
            <Button label="Create or join a group" onPress={() => router.push("/group/new")} />
          </View>
        ) : null}

        <View style={{ gap: 12 }}>
          {groups.map((g, i) => {
            const s = statuses[i];
            const people = g.memberIds.map((id) => profiles.get(id) ?? { id, name: "", photoURL: null });
            return (
              <Animated.View key={g.id} entering={FadeInDown.delay(i * 40).duration(260)}>
                <Tap
                  accessibilityRole="link"
                  accessibilityLabel={`${g.name}, ${s.label}, ${g.streak}-day streak`}
                  onPress={() => router.push(`/group/${g.id}`)}
                  scaleTo={0.98}
                  style={[{ padding: 16, borderRadius: 18, backgroundColor: colors.card, gap: 12 }, shadow.card]}
                >
                  <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
                    <Txt size={17} weight={700} numberOfLines={1} style={{ flex: 1, marginRight: 12 }}>
                      {g.name}
                    </Txt>
                    <StreakChip streak={g.streak} compact />
                  </View>
                  <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
                    <AvatarStack people={people} />
                    <Pill label={s.label} bg={s.bg} fg={s.fg} />
                  </View>
                </Tap>
              </Animated.View>
            );
          })}
        </View>
      </ScrollView>
    </Screen>
  );
}
