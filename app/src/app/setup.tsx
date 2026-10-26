import { useState } from "react";
import { KeyboardAvoidingView, Platform, View } from "react-native";
import { router } from "expo-router";
import { Avatar, BackButton, Button, Display, ErrorText, Field, Screen, Tap, Txt } from "../components/ui";
import { CameraIcon } from "../components/icons";
import { colors } from "../lib/theme";
import { useAuth } from "../lib/auth";
import { errorMessage, saveProfile } from "../lib/data";
import { pickSquarePhoto, savePhoto } from "../lib/upload";
import { pendingInvite } from "../lib/invite";

export default function Setup() {
  const { user, profile } = useAuth();
  const isNew = !profile?.name;
  const [name, setName] = useState(profile?.name ?? "");
  const [localPhoto, setLocalPhoto] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user) return null;
  const photo = localPhoto ?? profile?.photoURL ?? null;

  const choosePhoto = async () => {
    try {
      const uri = await pickSquarePhoto();
      if (uri) setLocalPhoto(uri);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const save = async () => {
    setError(null);
    setBusy(true);
    try {
      const photoURL = localPhoto ? await savePhoto(localPhoto, `avatar-${Date.now()}`) : photo;
      await saveProfile(user.uid, { name: name.trim(), photoURL }, isNew);
      if (isNew) {
        const code = await pendingInvite.take();
        router.replace(code ? `/j/${code}` : "/groups");
      } else {
        router.back();
      }
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  };

  return (
    <Screen edges={["top", "bottom"]}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        style={{ flex: 1, paddingHorizontal: 24, paddingTop: 24, paddingBottom: 16, gap: 32 }}
      >
        <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
          {isNew ? <View /> : <BackButton onPress={() => router.back()} />}

        </View>

        <View style={{ gap: 8 }}>
          <Display size={32} style={{ lineHeight: 35 }}>
            {isNew ? "Let your friends know it’s you" : "Edit your profile"}
          </Display>
          <Txt size={15} color={colors.muted} style={{ lineHeight: 22 }}>
            Your name and photo show next to your answers in every group.
          </Txt>
        </View>

        <View style={{ alignItems: "center", gap: 12 }}>
          <View style={{ width: 128, height: 128 }}>
            <Avatar id={user.uid} name={name || "?"} photoURL={photo} size={128} />
            <Tap
              accessibilityLabel="Add photo"
              onPress={choosePhoto}
              style={{
                position: "absolute",
                right: 0,
                bottom: 0,
                width: 44,
                height: 44,
                borderRadius: 22,
                borderWidth: 3,
                borderColor: colors.bg,
                backgroundColor: colors.accent,
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <CameraIcon />
            </Tap>
          </View>
          <Txt size={14} weight={700} color={colors.accent} accessibilityRole="button" onPress={choosePhoto}>
            {photo ? "Change photo" : "Add a photo"}
          </Txt>
        </View>

        <Field
          label="Display name"
          value={name}
          onChangeText={(t) => setName(t.slice(0, 30))}
          hint="You can change this any time in settings."
          autoComplete="name"
          textContentType="givenName"
          returnKeyType="done"
          onSubmitEditing={() => name.trim() && save()}
        />

        <View style={{ marginTop: "auto", gap: 12 }}>
          <ErrorText message={error} />
          <Button label={isNew ? "Continue" : "Save changes"} onPress={save} loading={busy} disabled={!name.trim()} />
        </View>
      </KeyboardAvoidingView>
    </Screen>
  );
}
