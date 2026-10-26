import { useState } from "react";
import { ScrollView, View } from "react-native";
import { router } from "expo-router";
import { Display, Loading, Screen, Tap, Txt } from "../../components/ui";
import { colors } from "../../lib/theme";
import { useAuth } from "../../lib/auth";
import { formatShortDate, isToday, Question, useGroups, useHistory, useProfiles, useTopAnswer } from "../../lib/data";

function Row({ groupId, q, uid, total }: { groupId: string; q: Question; uid: string; total: number }) {
  const top = useTopAnswer(groupId, q, uid);
  const profiles = useProfiles(top ? [top.uid] : []);
  const label = `${isToday(q.dropAt) ? "Today" : formatShortDate(q.dropAt)} · Day ${q.day}`;
  const topName = top ? (top.uid === uid ? "You" : profiles.get(top.uid)?.name || "Friend") : "";
  return (
    <Tap
      accessibilityRole="link"
      scaleTo={0.98}
      onPress={() => router.push({ pathname: "/group/[id]", params: { id: groupId, q: q.id } })}
      style={{ paddingVertical: 14, paddingHorizontal: 16, borderRadius: 18, backgroundColor: colors.card, gap: 8 }}
    >
      <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
        <Txt size={12} weight={700} color={colors.muted} style={{ textTransform: "uppercase" }}>
          {label}
        </Txt>
        <Txt size={12} weight={700} color={colors.muted}>
          {q.answerCount}/{Math.max(total, q.answerCount)} answered
        </Txt>
      </View>
      <Txt size={16} weight={700} style={{ lineHeight: 21 }}>
        {q.text}
      </Txt>
      {top ? (
        <Txt size={14} color={colors.muted2} style={{ lineHeight: 20 }} numberOfLines={2}>
          Top answer · {topName}: “{top.text}”
        </Txt>
      ) : !q.answeredBy.includes(uid) ? (
        <Txt size={14} color={colors.muted}>
          You didn’t answer this one.
        </Txt>
      ) : null}
    </Tap>
  );
}

export default function History() {
  const { user } = useAuth();
  const uid = user!.uid;
  const { value: groups, loading } = useGroups(uid);
  const [selected, setSelected] = useState<string | null>(null);
  const groupId = selected && groups.some((g) => g.id === selected) ? selected : groups[0]?.id;
  const group = groups.find((g) => g.id === groupId);
  const { value: questions, loading: historyLoading } = useHistory(groupId);

  if (loading) return <Loading />;

  return (
    <Screen>
      <ScrollView contentContainerStyle={{ paddingHorizontal: 20, paddingTop: 24, paddingBottom: 24, gap: 16 }}>
        <Display size={32}>History</Display>

        {groups.length ? (
          <View style={{ flexDirection: "row", gap: 8, flexWrap: "wrap" }}>
            {groups.map((g) => {
              const active = g.id === groupId;
              return (
                <Tap
                  key={g.id}
                  accessibilityRole="tab"
                  accessibilityState={{ selected: active }}
                  onPress={() => setSelected(g.id)}
                  style={{
                    height: 36,
                    paddingHorizontal: 14,
                    borderRadius: 18,
                    justifyContent: "center",
                    backgroundColor: active ? colors.ink : colors.card,
                    borderWidth: active ? 0 : 1.5,
                    borderColor: colors.border,
                  }}
                >
                  <Txt size={13} weight={active ? 700 : 500} color={active ? colors.white : colors.ink}>
                    {g.name}
                  </Txt>
                </Tap>
              );
            })}
          </View>
        ) : (
          <Txt size={15} color={colors.muted}>
            Join or create a group and its questions will collect here.
          </Txt>
        )}

        {historyLoading ? null : (
          <View style={{ gap: 10 }}>
            {questions.map((q) => (
              <Row key={q.id} groupId={groupId!} q={q} uid={uid} total={group?.memberCount ?? 0} />
            ))}
          </View>
        )}
      </ScrollView>
    </Screen>
  );
}
