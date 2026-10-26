import { useEffect } from "react";
import { Alert, Platform, Pressable, View } from "react-native";
import { router, useLocalSearchParams } from "expo-router";
import { BackButton, Button, Display, Loading, Screen, StreakChip, Txt } from "../../components/ui";
import { QuestionView } from "../../components/QuestionView";
import { AnswersView } from "../../components/AnswersView";
import { colors } from "../../lib/theme";
import { useAuth } from "../../lib/auth";
import {
  formatDayHeader,
  markSeen,
  useAnswers,
  useComments,
  useGroup,
  useMyAnswer,
  useProfiles,
  useQuestion,
} from "../../lib/data";
import { dropNextNow } from "../../lib/engine";

export default function GroupScreen() {
  const { id, q } = useLocalSearchParams<{ id: string; q?: string }>();
  const { user } = useAuth();
  const uid = user!.uid;

  const { value: group, loading: groupLoading } = useGroup(id);
  const questionId = q ?? group?.current?.id;
  const { value: question, loading: questionLoading } = useQuestion(id, questionId);
  const { value: myAnswer, loading: answerLoading } = useMyAnswer(id, questionId, uid);
  const canSeeAll = !!myAnswer && !!question?.unlocked;
  const { value: answers } = useAnswers(id, questionId, canSeeAll);
  const { value: comments } = useComments(id, questionId, canSeeAll);
  const profiles = useProfiles([...(group?.memberIds ?? []), ...answers.map((a) => a.uid), ...comments.map((c) => c.uid)]);

  const isCurrent = !!group?.current && questionId === group.current.id;

  // Remember how many answers you've seen so the groups list can show "3 new answers".
  useEffect(() => {
    if (canSeeAll && isCurrent && questionId && answers.length) {
      markSeen(uid, id, questionId, answers.length).catch(() => undefined);
    }
  }, [canSeeAll, isCurrent, questionId, answers.length, uid, id]);

  const goBack = () => (router.canGoBack() ? router.back() : router.replace("/groups"));

  if (groupLoading || (questionId && (questionLoading || answerLoading))) return <Loading />;

  if (!group) {
    return (
      <Screen style={{ padding: 24, gap: 16 }}>
        <BackButton onPress={goBack} />
        <Display size={26}>This group isn’t available</Display>
        <Txt size={15} color={colors.muted}>
          You may have left it, or it was deleted.
        </Txt>
        <Button label="Back to your groups" onPress={() => router.replace("/groups")} />
      </Screen>
    );
  }

  return (
    <Screen edges={canSeeAll ? ["top"] : ["top", "bottom"]}>
      <View style={{ flexDirection: "row", alignItems: "center", gap: 12, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 20 }}>
        <BackButton onPress={goBack} />
        {/* Demo shortcut: long-press the group name to get the next question now. */}
        <Pressable
          style={{ flex: 1 }}
          delayLongPress={600}
          accessibilityHint="Long-press to skip to the next question (demo)"
          onLongPress={() => {
            const go = () => router.setParams({ q: undefined });
            const run = () => {
              dropNextNow(group.id);
              go();
            };
            if (Platform.OS === "web") {
              if (globalThis.confirm?.("Skip to the next question? (Demo shortcut)")) run();
            } else {
              Alert.alert("Skip to the next question?", "Demo shortcut: tomorrow’s question arrives now.", [
                { text: "Cancel", style: "cancel" },
                { text: "Skip ahead", onPress: run },
              ]);
            }
          }}
        >
          <Txt size={17} weight={700} numberOfLines={1}>
            {group.name}
          </Txt>
          <Txt size={13} color={colors.muted}>
            {question ? `Day ${question.day} · ${formatDayHeader(question.dropAt)}` : "Your first question is on its way"}
          </Txt>
        </Pressable>
        <StreakChip streak={group.streak} />
      </View>

      {!question ? (
        <View style={{ padding: 20 }}>
          <Txt size={15} color={colors.muted}>
            Questions drop once a day. Pull your friends in while you wait.
          </Txt>
        </View>
      ) : canSeeAll ? (
        <AnswersView group={group} question={question} answers={answers} comments={comments} uid={uid} profiles={profiles} />
      ) : (
        <QuestionView group={group} question={question} myAnswer={myAnswer} uid={uid} profiles={profiles} canAnswer={isCurrent} />
      )}
    </Screen>
  );
}
