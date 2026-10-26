import { useEffect, useMemo, useRef, useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, ScrollViewInstance, TextInput, TextInputInstance, View } from "react-native";
import Animated, { FadeInDown, useAnimatedStyle, useSharedValue, withSequence, withSpring } from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { colors, fonts, MAX_WIDTH } from "../lib/theme";
import { Answer, Comment, errorMessage, formatTime, Group, Profile, Question, postComment, toggleLike } from "../lib/data";
import { Avatar, Display, ErrorText, haptic, Tap, Txt, webNoOutline } from "./ui";
import { ChatIcon, CheckIcon, HeartIcon, SendIcon } from "./icons";

function LikeButton({ liked, count, onPress }: { liked: boolean; count: number; onPress: () => void }) {
  const pop = useSharedValue(1);
  const style = useAnimatedStyle(() => ({ transform: [{ scale: pop.value }] }));
  return (
    <Tap
      accessibilityLabel={liked ? `Unlike, ${count} likes` : `Like, ${count} likes`}
      accessibilityState={{ selected: liked }}
      hapticOnPress={false}
      onPress={() => {
        if (!liked) {
          pop.value = withSequence(withSpring(1.35, { damping: 6, stiffness: 400 }), withSpring(1));
          haptic.select();
        }
        onPress();
      }}
      style={{
        height: 32,
        paddingHorizontal: 10,
        borderRadius: 16,
        backgroundColor: liked ? colors.accentSoft : colors.bg,
        flexDirection: "row",
        alignItems: "center",
        gap: 5,
      }}
    >
      <Animated.View style={style}>
        <HeartIcon color={liked ? colors.accentPressed : colors.muted2} filled={liked} />
      </Animated.View>
      <Txt size={13} weight={700} color={liked ? colors.accentPressed : colors.muted2}>
        {count}
      </Txt>
    </Tap>
  );
}

export function AnswersView({
  group,
  question,
  answers,
  comments,
  uid,
  profiles,
}: {
  group: Group;
  question: Question;
  answers: Answer[];
  comments: Comment[];
  uid: string;
  profiles: Map<string, Profile>;
}) {
  const insets = useSafeAreaInsets();
  const [draft, setDraft] = useState("");
  const [replyTo, setReplyTo] = useState<Answer | null>(null);
  const [open, setOpen] = useState<Record<string, boolean>>({});
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<TextInputInstance>(null);
  const scrollRef = useRef<ScrollViewInstance>(null);

  // Only the first render staggers in; later updates appear without fanfare.
  const firstPaint = useRef(true);
  useEffect(() => {
    if (answers.length) firstPaint.current = false;
  }, [answers.length]);

  const repliesFor = useMemo(() => {
    const map = new Map<string, Comment[]>();
    for (const c of comments) if (c.answerId) map.set(c.answerId, [...(map.get(c.answerId) ?? []), c]);
    return map;
  }, [comments]);
  const groupChat = comments.filter((c) => !c.answerId);

  const nameOf = (id: string) => (id === uid ? "You" : profiles.get(id)?.name || "Friend");
  const total = group.memberIds.length;

  const send = async () => {
    const text = draft.trim();
    if (!text) return;
    setError(null);
    setDraft("");
    try {
      await postComment(group.id, question.id, uid, text, replyTo?.id ?? null);
      if (replyTo) setOpen((o) => ({ ...o, [replyTo.id]: true }));
      else setTimeout(() => scrollRef.current?.scrollToEnd({ animated: true }), 150);
      setReplyTo(null);
    } catch (e) {
      setDraft(text);
      setError(errorMessage(e));
    }
  };

  return (
    <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
      <ScrollView ref={scrollRef} keyboardShouldPersistTaps="handled" contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: 20, gap: 16 }}>
        <View style={{ gap: 8 }}>
          <View
            style={{
              alignSelf: "flex-start",
              height: 28,
              paddingHorizontal: 10,
              borderRadius: 14,
              backgroundColor: question.allAnswered ? colors.successBg : colors.track,
              flexDirection: "row",
              alignItems: "center",
              gap: 6,
            }}
          >
            {question.allAnswered ? <CheckIcon /> : null}
            <Txt size={12} weight={700} color={question.allAnswered ? colors.successInk : colors.muted2}>
              {question.allAnswered ? "Everyone answered · streak kept" : `${question.answerCount} of ${total} answered`}
            </Txt>
          </View>
          <Display size={24} style={{ lineHeight: 28 }}>
            {question.text}
          </Display>
        </View>

        <View style={{ gap: 10 }}>
          {answers.map((a, i) => {
            const p = profiles.get(a.uid);
            const replies = repliesFor.get(a.id) ?? [];
            const expanded = open[a.id];
            return (
              <Animated.View
                key={a.id}
                entering={firstPaint.current ? FadeInDown.delay(120 + i * 110).springify().damping(15) : undefined}
                style={{ padding: 14, borderRadius: 18, backgroundColor: colors.card, gap: 10 }}
              >
                <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
                  <Avatar id={a.uid} name={p?.name} photoURL={p?.photoURL} size={34} />
                  <Txt size={14} weight={700} style={{ flex: 1 }}>
                    {nameOf(a.uid)}
                  </Txt>
                  <Txt size={12} color={colors.muted}>
                    {formatTime(a.createdAt)}
                  </Txt>
                </View>
                <Txt size={15} style={{ lineHeight: 22 }}>
                  {a.text}
                </Txt>
                <View style={{ flexDirection: "row", gap: 8 }}>
                  <LikeButton
                    liked={a.likes.includes(uid)}
                    count={a.likes.length}
                    onPress={() => toggleLike(group.id, question.id, a, uid).catch((e) => setError(errorMessage(e)))}
                  />
                  <Tap
                    onPress={() => {
                      if (replies.length && !expanded) {
                        setOpen((o) => ({ ...o, [a.id]: true }));
                      } else {
                        setReplyTo(a);
                        inputRef.current?.focus();
                      }
                    }}
                    style={{ height: 32, paddingHorizontal: 10, borderRadius: 16, backgroundColor: colors.bg, flexDirection: "row", alignItems: "center", gap: 5 }}
                  >
                    <ChatIcon />
                    <Txt size={13} weight={700} color={colors.muted2}>
                      {replies.length && !expanded ? `${replies.length} repl${replies.length === 1 ? "y" : "ies"}` : "Reply"}
                    </Txt>
                  </Tap>
                </View>
                {expanded && replies.length ? (
                  <View style={{ gap: 8, paddingLeft: 12, borderLeftWidth: 2, borderLeftColor: colors.track }}>
                    {replies.map((c) => (
                      <View key={c.id} style={{ gap: 2 }}>
                        <Txt size={13} weight={700}>
                          {nameOf(c.uid)}
                        </Txt>
                        <Txt size={14} style={{ lineHeight: 20 }}>
                          {c.text}
                        </Txt>
                      </View>
                    ))}
                  </View>
                ) : null}
              </Animated.View>
            );
          })}
        </View>

        {groupChat.length ? (
          <View style={{ gap: 10 }}>
            <Txt size={14} weight={700} color={colors.muted}>
              Group chat
            </Txt>
            {groupChat.map((c) => {
              const mine = c.uid === uid;
              const p = profiles.get(c.uid);
              return (
                <View key={c.id} style={{ flexDirection: mine ? "row-reverse" : "row", alignItems: "flex-end", gap: 8 }}>
                  {!mine ? <Avatar id={c.uid} name={p?.name} photoURL={p?.photoURL} size={28} /> : null}
                  <View
                    style={{
                      maxWidth: "78%",
                      paddingVertical: 10,
                      paddingHorizontal: 14,
                      borderRadius: 18,
                      backgroundColor: mine ? colors.accent : colors.card,
                      gap: 2,
                    }}
                  >
                    {!mine ? (
                      <Txt size={12} weight={700} color={colors.muted}>
                        {nameOf(c.uid)}
                      </Txt>
                    ) : null}
                    <Txt size={15} color={mine ? colors.white : colors.ink} style={{ lineHeight: 21 }}>
                      {c.text}
                    </Txt>
                  </View>
                </View>
              );
            })}
          </View>
        ) : null}
        <ErrorText message={error} />
      </ScrollView>

      <View style={{ backgroundColor: colors.card, borderTopWidth: 1, borderTopColor: colors.hairline, alignItems: "center" }}>
        <View style={{ width: "100%", maxWidth: MAX_WIDTH, paddingHorizontal: 20, paddingTop: 12, paddingBottom: Math.max(insets.bottom, 16) + 12, gap: 8 }}>
          {replyTo ? (
            <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
              <Txt size={13} color={colors.muted}>
                Replying to {nameOf(replyTo.uid)}
              </Txt>
              <Txt size={13} weight={700} color={colors.accent} accessibilityRole="button" onPress={() => setReplyTo(null)}>
                Cancel
              </Txt>
            </View>
          ) : null}
          <View style={{ flexDirection: "row", gap: 10, alignItems: "center" }}>
            <TextInput
              ref={inputRef}
              accessibilityLabel={replyTo ? `Reply to ${nameOf(replyTo.uid)}` : "Reply to the group"}
              value={draft}
              onChangeText={(t) => setDraft(t.slice(0, 280))}
              placeholder={replyTo ? `Reply to ${nameOf(replyTo.uid)}…` : "Say something to the group…"}
              placeholderTextColor={colors.muted}
              returnKeyType="send"
              onSubmitEditing={send}
              style={[
                {
                  flex: 1,
                  height: 48,
                  paddingHorizontal: 16,
                  borderRadius: 24,
                  borderWidth: 1.5,
                  borderColor: colors.border,
                  backgroundColor: colors.bg,
                  fontFamily: fonts.regular,
                  fontSize: 15,
                  color: colors.ink,
                },
                webNoOutline,
              ]}
            />
            <Tap
              accessibilityLabel="Send"
              onPress={send}
              disabled={!draft.trim()}
              style={{ width: 48, height: 48, borderRadius: 24, backgroundColor: colors.accent, alignItems: "center", justifyContent: "center" }}
            >
              <SendIcon />
            </Tap>
          </View>
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}
