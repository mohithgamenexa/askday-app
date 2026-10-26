import { useMemo, useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, TextInput, View } from "react-native";
import Animated, { FadeIn, FadeInUp, ZoomIn } from "react-native-reanimated";
import { colors, fonts } from "../lib/theme";
import { Answer, api, errorMessage, formatTime, Group, Profile, Question, submitAnswer } from "../lib/data";
import { Avatar, Button, Display, ErrorText, haptic, Tap, Txt, webNoOutline } from "./ui";
import { BellIcon, LockIcon } from "./icons";

export function QuestionView({
  group,
  question,
  myAnswer,
  uid,
  profiles,
  canAnswer,
}: {
  group: Group;
  question: Question;
  myAnswer: Answer | null;
  uid: string;
  profiles: Map<string, Profile>;
  canAnswer: boolean;
}) {
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [nudged, setNudged] = useState<Record<string, boolean>>({});

  const answered = question.answeredBy;
  const total = group.memberIds.length;
  const missing = group.memberIds.filter((m) => !answered.includes(m));
  const nudgeable = missing.filter((m) => m !== uid);
  const [nudgeTarget, setNudgeTarget] = useState<string | null>(null);
  const target = nudgeTarget && nudgeable.includes(nudgeTarget) ? nudgeTarget : nudgeable[0];

  // Answered people first, then the ones we're waiting on.
  const ordered = useMemo(
    () => [...group.memberIds].sort((a, b) => Number(!answered.includes(a)) - Number(!answered.includes(b))),
    [group.memberIds, answered],
  );

  const submit = async () => {
    setError(null);
    setBusy(true);
    try {
      await submitAnswer(group.id, question.id, uid, text);
      haptic.success();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const nudge = async () => {
    if (!target) return;
    setError(null);
    try {
      await api.nudge({ groupId: group.id, targetUid: target });
      haptic.success();
      setNudged((n) => ({ ...n, [target]: true }));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const progress = total ? answered.length / total : 0;
  const lockLabel = question.unlocked
    ? myAnswer
      ? "Unlocked"
      : "Answer to see them"
    : `Unlocks at ${formatTime(question.unlockAt)}`;

  return (
    <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
      <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ flexGrow: 1, paddingHorizontal: 20, paddingBottom: 16, gap: 20 }}>
        <Animated.View
          entering={FadeInUp.springify().damping(16)}
          style={{ paddingVertical: 28, paddingHorizontal: 24, borderRadius: 26, backgroundColor: colors.accent, gap: 16, minHeight: 200 }}
        >
          <Txt size={13} weight={700} color={colors.accentOnAccent} style={{ letterSpacing: 1, textTransform: "uppercase" }}>
            {canAnswer ? "Today’s question" : `Day ${question.day}`}
          </Txt>
          <Display size={32} color={colors.white} style={{ lineHeight: 35 }}>
            {question.text}
          </Display>
        </Animated.View>

        {myAnswer ? (
          <Animated.View entering={ZoomIn.springify().damping(14)} style={{ padding: 16, borderRadius: 18, backgroundColor: colors.card, gap: 8 }}>
            <Txt size={14} weight={700}>
              Your answer
            </Txt>
            <Txt size={16} style={{ lineHeight: 23 }}>
              {myAnswer.text}
            </Txt>
          </Animated.View>
        ) : canAnswer ? (
          <View style={{ gap: 8 }}>
            <Txt size={14} weight={700}>
              Your answer
            </Txt>
            <TextInput
              accessibilityLabel="Your answer"
              multiline
              value={text}
              onChangeText={(t) => setText(t.slice(0, 280))}
              placeholder="Be honest. Your friends can’t see it until they answer too."
              placeholderTextColor={colors.muted}
              textAlignVertical="top"
              style={[
                {
                  minHeight: 116,
                  paddingVertical: 14,
                  paddingHorizontal: 16,
                  borderRadius: 16,
                  borderWidth: 1.5,
                  borderColor: text ? colors.accent : colors.border,
                  backgroundColor: colors.card,
                  fontFamily: fonts.regular,
                  fontSize: 16,
                  lineHeight: 23,
                  color: colors.ink,
                },
                webNoOutline,
              ]}
            />
            {text.length > 240 ? (
              <Txt size={12} color={colors.muted} style={{ textAlign: "right" }}>
                {280 - text.length} characters left
              </Txt>
            ) : null}
          </View>
        ) : (
          <View style={{ padding: 16, borderRadius: 18, backgroundColor: colors.card }}>
            <Txt size={15} color={colors.muted}>
              You didn’t answer this one, so its answers stay locked.
            </Txt>
          </View>
        )}

        <View style={{ paddingVertical: 14, paddingHorizontal: 16, borderRadius: 18, backgroundColor: colors.card, gap: 12 }}>
          <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
            <Txt size={14} weight={700}>
              {answered.length} of {total} have answered
            </Txt>
            <View style={{ flexDirection: "row", alignItems: "center", gap: 4 }}>
              <LockIcon size={14} color={colors.muted} />
              <Txt size={13} color={colors.muted}>
                {lockLabel}
              </Txt>
            </View>
          </View>
          <View style={{ height: 8, borderRadius: 4, backgroundColor: colors.track, overflow: "hidden" }}>
            <Animated.View entering={FadeIn} style={{ width: `${Math.round(progress * 100)}%`, height: 8, borderRadius: 4, backgroundColor: colors.accent }} />
          </View>
          <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
            <View style={{ flexDirection: "row", gap: 6, flexWrap: "wrap", flex: 1 }}>
              {ordered.slice(0, 7).map((m) => {
                const p = profiles.get(m);
                const done = answered.includes(m);
                const selectable = !done && m !== uid;
                const avatar = (
                  <Avatar
                    id={m}
                    name={p?.name}
                    photoURL={p?.photoURL}
                    dashed={!done}
                    label={!done && m === uid ? "You" : undefined}
                    style={selectable && m === target ? { borderColor: colors.accent, borderWidth: 2 } : null}
                  />
                );
                return selectable ? (
                  <Tap key={m} accessibilityLabel={`Choose ${p?.name ?? "friend"} to nudge`} onPress={() => setNudgeTarget(m)} hapticOnPress={false}>
                    {avatar}
                  </Tap>
                ) : (
                  <View key={m}>{avatar}</View>
                );
              })}
            </View>
            {canAnswer && target && !question.unlocked ? (
              <Tap
                onPress={nudge}
                disabled={nudged[target]}
                style={{
                  height: 36,
                  paddingHorizontal: 12,
                  borderWidth: 1.5,
                  borderColor: colors.border,
                  borderRadius: 12,
                  backgroundColor: colors.card,
                  flexDirection: "row",
                  alignItems: "center",
                  gap: 6,
                }}
              >
                <BellIcon />
                <Txt size={13} weight={700}>
                  {nudged[target] ? "Nudged" : `Nudge ${(profiles.get(target)?.name ?? "").split(" ")[0] || "them"}`}
                </Txt>
              </Tap>
            ) : null}
          </View>
        </View>

        <View style={{ marginTop: "auto", gap: 12 }}>
          <ErrorText message={error} />
          {canAnswer && !myAnswer ? (
            <Button label="Share my answer" variant="dark" onPress={submit} loading={busy} disabled={!text.trim()} />
          ) : myAnswer && !question.unlocked ? (
            <Txt size={14} color={colors.muted} style={{ textAlign: "center" }}>
              {missing.length === 1 ? "Waiting on 1 more person." : `Waiting on ${missing.length} more people.`} We’ll let you know when answers unlock.
            </Txt>
          ) : null}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
