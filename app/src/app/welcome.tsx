import { useState } from "react";
import { Linking, View } from "react-native";
import Animated, { FadeInDown, FadeIn } from "react-native-reanimated";
import { Avatar, Button, Display, ErrorText, Screen, Txt } from "../components/ui";
import { LockIcon } from "../components/icons";
import { colors, fonts, shadow } from "../lib/theme";
import { errorMessage } from "../lib/data";
import { createGuest } from "../lib/engine";

const TERMS_URL = process.env.EXPO_PUBLIC_TERMS_URL ?? "https://askday.app/terms";
const PRIVACY_URL = process.env.EXPO_PUBLIC_PRIVACY_URL ?? "https://askday.app/privacy";

function PreviewCard({ initials, bg, name, text, rotate }: { initials: string; bg: string; name: string; text: string; rotate: string }) {
  return (
    <View
      style={[
        {
          width: 250,
          paddingVertical: 14,
          paddingHorizontal: 16,
          backgroundColor: colors.card,
          borderRadius: 18,
          flexDirection: "row",
          gap: 10,
          transform: [{ rotate }],
        },
        shadow.float,
      ]}
    >
      <Avatar id={name} name={name} label={initials} bg={bg} size={32} />
      <View style={{ flex: 1, gap: 2 }}>
        <Txt size={13} weight={700}>
          {name}
        </Txt>
        <Txt size={14} style={{ lineHeight: 19 }}>
          {text}
        </Txt>
      </View>
    </View>
  );
}

export default function Welcome() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Guest sign-in for now: a guest account that lives on this device. The gate
  // in _layout sends new guests to profile setup.
  const continueAsGuest = async () => {
    setError(null);
    setBusy(true);
    try {
      createGuest();
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  };

  return (
    <Screen edges={["top", "bottom"]} style={{ paddingHorizontal: 24, paddingTop: 24, paddingBottom: 16, gap: 28 }}>
      <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
        <View
          style={{
            width: 40,
            height: 40,
            borderRadius: 12,
            backgroundColor: colors.accent,
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Txt style={{ fontFamily: fonts.display, fontSize: 24, color: colors.white }}>?</Txt>
        </View>
        <Txt style={{ fontFamily: fonts.display, fontSize: 24, letterSpacing: -0.5 }}>Askday</Txt>
      </View>

      {/* The one orchestrated moment: friends' answers drop in, still locked. */}
      <View style={{ height: 230 }} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
        <Animated.View entering={FadeInDown.delay(150).springify().damping(14)} style={{ position: "absolute", left: 0, top: 8 }}>
          <PreviewCard initials="PR" bg="#FFD9C2" name="Priya" text="A single sock. Not even a pair." rotate="-4deg" />
        </Animated.View>
        <Animated.View entering={FadeInDown.delay(380).springify().damping(14)} style={{ position: "absolute", right: 0, top: 82 }}>
          <PreviewCard initials="SM" bg="#D6E4FF" name="Sam" text="A gym membership. From my ex." rotate="3deg" />
        </Animated.View>
        <Animated.View
          entering={FadeIn.delay(700).duration(400)}
          style={{
            position: "absolute",
            left: 24,
            top: 160,
            height: 44,
            paddingHorizontal: 16,
            borderRadius: 22,
            backgroundColor: colors.ink,
            flexDirection: "row",
            alignItems: "center",
            gap: 8,
          }}
        >
          <LockIcon />
          <Txt size={14} weight={500} color={colors.white}>
            4 more answers unlock when you reply
          </Txt>
        </Animated.View>
      </View>

      <View style={{ gap: 12 }}>
        <Display size={40} style={{ lineHeight: 41, letterSpacing: -1.2 }}>
          {"One question.\nEvery day.\nYour people."}
        </Display>
        <Txt size={16} color={colors.muted} style={{ lineHeight: 24 }}>
          Answer today’s question with your friends. Everyone’s answers unlock once the whole group is in.
        </Txt>
      </View>

      <View style={{ marginTop: "auto", gap: 10 }}>
        <ErrorText message={error} />
        <Button label="Continue as guest" onPress={continueAsGuest} loading={busy} />
        <Txt size={12} color={colors.muted} style={{ textAlign: "center", marginTop: 6, lineHeight: 18 }}>
          By continuing you agree to our{" "}
          <Txt size={12} color={colors.accent} accessibilityRole="link" onPress={() => Linking.openURL(TERMS_URL)}>
            Terms
          </Txt>{" "}
          and{" "}
          <Txt size={12} color={colors.accent} accessibilityRole="link" onPress={() => Linking.openURL(PRIVACY_URL)}>
            Privacy Policy
          </Txt>
          .
        </Txt>
      </View>
    </Screen>
  );
}
