import { useEffect, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { Button, Display, Loading, Screen, Txt } from "../../components/ui";
import { colors } from "../../lib/theme";
import { useAuth } from "../../lib/auth";
import { api, errorMessage } from "../../lib/data";
import { pendingInvite } from "../../lib/invite";

/** Opens from an invite link like askday.app/j/7KQ2XM. */
export default function JoinFromLink() {
  const { code } = useLocalSearchParams<{ code: string }>();
  const { user, profile } = useAuth();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!code) return;
    if (!user || !profile?.name) {
      // Remember the invite and finish joining once they've signed in.
      pendingInvite.set(code).then(() => router.replace(user ? "/setup" : "/welcome"));
      return;
    }
    api
      .joinGroup({ code })
      .then(({ groupId }) => router.replace(`/group/${groupId}`))
      .catch((e) => setError(errorMessage(e)));
  }, [code, user, profile?.name]);

  if (!error) return <Loading />;
  return (
    <Screen style={{ padding: 24, gap: 16, justifyContent: "center" }}>
      <Display size={28}>We couldn’t add you to that group</Display>
      <Txt size={15} color={colors.muted}>
        {error}
      </Txt>
      <Button label="Go to your groups" onPress={() => router.replace("/groups")} />
    </Screen>
  );
}
