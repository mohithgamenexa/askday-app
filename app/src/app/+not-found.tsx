import { router } from "expo-router";
import { Button, Display, Screen, Txt } from "../components/ui";
import { colors } from "../lib/theme";

export default function NotFound() {
  return (
    <Screen style={{ padding: 24, gap: 16, justifyContent: "center" }}>
      <Display size={28}>This page doesn’t exist</Display>
      <Txt size={15} color={colors.muted}>
        The link may be old or mistyped.
      </Txt>
      <Button label="Go to your groups" onPress={() => router.replace("/")} />
    </Screen>
  );
}
