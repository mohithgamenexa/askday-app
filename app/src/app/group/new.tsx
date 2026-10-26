import { useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, TextInput, View } from "react-native";
import { router } from "expo-router";
import { Avatar, BackButton, Button, Display, ErrorText, Field, haptic, Screen, Tap, Txt, webNoOutline } from "../../components/ui";
import { CameraIcon, InstagramIcon, MessageIcon, MoreIcon, WhatsAppIcon } from "../../components/icons";
import { colors, fonts, shadow } from "../../lib/theme";
import { api, DropSlot, errorMessage, SLOTS } from "../../lib/data";
import { setGroupPhoto } from "../../lib/engine";
import { inviteDisplay } from "../../lib/invite";
import { pickSquarePhoto, savePhoto } from "../../lib/upload";

const SHARE_TARGETS = [
  { id: "whatsapp", label: "WhatsApp", Icon: WhatsAppIcon },
  { id: "instagram", label: "Instagram", Icon: InstagramIcon },
  { id: "messages", label: "Messages", Icon: MessageIcon },
  { id: "more", label: "More", Icon: MoreIcon },
] as const;

export default function NewGroup() {
  const [mode, setMode] = useState<"create" | "join">("create");
  const [name, setName] = useState("");
  const [slot, setSlot] = useState<DropSlot>("evening");
  const [photo, setPhoto] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const [created, setCreated] = useState<{ groupId: string; inviteCode: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const create = async () => {
    setError(null);
    setBusy(true);
    try {
      const res = await api.createGroup({ name: name.trim(), dropSlot: slot });
      if (photo) {
        const url = await savePhoto(photo, `group-${res.groupId}`).catch(() => null);
        if (url) setGroupPhoto(res.groupId, url);
      }
      haptic.success();
      setCreated(res);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const join = async () => {
    setError(null);
    setBusy(true);
    try {
      const res = await api.joinGroup({ code: code.trim().toUpperCase() });
      haptic.success();
      router.replace(`/group/${res.groupId}`);
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  };

  const segment = (id: "create" | "join", label: string) => {
    const active = mode === id;
    return (
      <Tap
        accessibilityRole="tab"
        accessibilityState={{ selected: active }}
        disabled={!!created}
        onPress={() => setMode(id)}
        style={[
          { flex: 1, height: 44, borderRadius: 11, alignItems: "center", justifyContent: "center", backgroundColor: active ? colors.card : "transparent" },
          active && shadow.segment,
        ]}
      >
        <Txt size={15} weight={active ? 700 : 500} color={active ? colors.ink : colors.muted2}>
          {label}
        </Txt>
      </Tap>
    );
  };

  return (
    <Screen edges={["top", "bottom"]}>
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ flexGrow: 1, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 16, gap: 22 }}>
          <View style={{ flexDirection: "row", alignItems: "center", gap: 12 }}>
            <BackButton onPress={() => (router.canGoBack() ? router.back() : router.replace("/groups"))} />
            <Display size={26}>{mode === "create" ? "New group" : "Join a group"}</Display>
          </View>

          <View accessibilityRole="tablist" style={{ flexDirection: "row", padding: 4, borderRadius: 14, backgroundColor: colors.track }}>
            {segment("create", "Create")}
            {segment("join", "Join with code")}
          </View>

          {mode === "join" ? (
            <>
              <Field
                label="Invite code"
                value={code}
                onChangeText={(t) => setCode(t.replace(/[^a-zA-Z0-9]/g, "").toUpperCase().slice(0, 10))}
                placeholder="7KQ2XM"
                autoCapitalize="characters"
                autoCorrect={false}
                autoFocus
                hint="It’s the last part of the invite link your friend sent you."
                style={{ letterSpacing: 4 }}
                onSubmitEditing={join}
              />
              <View style={{ marginTop: "auto", gap: 12 }}>
                <ErrorText message={error} />
                <Button label="Join group" onPress={join} loading={busy} disabled={code.length < 4} />
              </View>
            </>
          ) : (
            <>
              <View style={{ flexDirection: "row", alignItems: "flex-end", gap: 14 }}>
                <Tap
                  accessibilityLabel="Add group photo"
                  disabled={!!created}
                  onPress={async () => {
                    const uri = await pickSquarePhoto().catch(() => null);
                    if (uri) setPhoto(uri);
                  }}
                  style={{
                    width: 64,
                    height: 64,
                    borderRadius: 20,
                    borderWidth: photo ? 0 : 2,
                    borderStyle: "dashed",
                    borderColor: colors.dashedLight,
                    backgroundColor: colors.card,
                    alignItems: "center",
                    justifyContent: "center",
                    overflow: "hidden",
                  }}
                >
                  {photo ? <Avatar id="new" photoURL={photo} size={64} style={{ borderRadius: 20 }} /> : <CameraIcon size={24} color={colors.muted} />}
                </Tap>
                <View style={{ flex: 1, gap: 6 }}>
                  <Txt size={14} weight={700}>
                    Group name
                  </Txt>
                  <TextInput
                    accessibilityLabel="Group name"
                    value={name}
                    onChangeText={(t) => setName(t.slice(0, 40))}
                    editable={!created}
                    placeholder="College Crew"
                    placeholderTextColor={colors.muted}
                    style={[
                      {
                        height: 50,
                        paddingHorizontal: 14,
                        borderRadius: 14,
                        borderWidth: 2,
                        borderColor: colors.accent,
                        backgroundColor: colors.card,
                        fontFamily: fonts.regular,
                        fontSize: 16,
                        color: colors.ink,
                      },
                      webNoOutline,
                    ]}
                  />
                </View>
              </View>

              <View style={{ gap: 10 }}>
                <Txt size={14} weight={700}>
                  When should the question drop?
                </Txt>
                <View style={{ flexDirection: "row", gap: 8 }} accessibilityRole="radiogroup">
                  {SLOTS.map((s) => {
                    const active = slot === s.id;
                    return (
                      <Tap
                        key={s.id}
                        accessibilityRole="radio"
                        accessibilityState={{ checked: active }}
                        disabled={!!created}
                        onPress={() => setSlot(s.id)}
                        style={{
                          flex: 1,
                          height: 56,
                          borderRadius: 14,
                          borderWidth: active ? 2 : 1.5,
                          borderColor: active ? colors.accent : colors.border,
                          backgroundColor: active ? colors.accentSoft : colors.card,
                          alignItems: "center",
                          justifyContent: "center",
                          gap: 2,
                        }}
                      >
                        <Txt size={14} weight={700}>
                          {s.label}
                        </Txt>
                        <Txt size={12} color={active ? colors.muted2 : colors.muted}>
                          {s.time}
                        </Txt>
                      </Tap>
                    );
                  })}
                </View>
              </View>

              <View style={{ padding: 16, borderRadius: 18, backgroundColor: colors.card, gap: 14 }}>
                <View style={{ gap: 4 }}>
                  <Txt size={14} weight={700}>
                    Invite friends
                  </Txt>
                  <Txt size={13} color={colors.muted}>
                    {created
                      ? "Real invites work once Askday is online. For now, sample friends have joined your group."
                      : "Your invite link appears here once the group is created."}
                  </Txt>
                </View>
                <View style={{ flexDirection: "row", alignItems: "center", gap: 8, height: 48, paddingLeft: 14, paddingRight: 6, borderRadius: 12, backgroundColor: colors.bg }}>
                  <Txt size={15} weight={500} style={{ flex: 1 }} color={created ? colors.ink : colors.muted} numberOfLines={1} selectable>
                    {created ? inviteDisplay(created.inviteCode) : inviteDisplay("······")}
                  </Txt>
                  <Tap
                    disabled
                    style={{ height: 36, paddingHorizontal: 14, borderRadius: 10, backgroundColor: colors.ink, justifyContent: "center" }}
                  >
                    <Txt size={13} weight={700} color={colors.white}>
                      Copy
                    </Txt>
                  </Tap>
                </View>
                <View style={{ flexDirection: "row", gap: 8 }}>
                  {SHARE_TARGETS.map(({ id, label, Icon }) => (
                    <Tap
                      key={id}
                      accessibilityLabel={`Share on ${label}`}
                      disabled
                      style={{ flex: 1, height: 64, borderRadius: 12, backgroundColor: colors.bg, alignItems: "center", justifyContent: "center", gap: 6 }}
                    >
                      <Icon />
                      <Txt size={12} weight={500}>
                        {label}
                      </Txt>
                    </Tap>
                  ))}
                </View>
              </View>

              <View style={{ marginTop: "auto", gap: 12 }}>
                <ErrorText message={error} />
                {created ? (
                  <Button label="Go to today’s question" onPress={() => router.replace(`/group/${created.groupId}`)} />
                ) : (
                  <Button label="Create group" onPress={create} loading={busy} disabled={!name.trim()} />
                )}
              </View>
            </>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
