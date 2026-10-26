import { useEffect } from "react";
import { Platform } from "react-native";
import { Stack, router, useSegments } from "expo-router";
import { StatusBar } from "expo-status-bar";
import * as SplashScreen from "expo-splash-screen";
import * as Notifications from "expo-notifications";
import { useFonts } from "expo-font";
import { BricolageGrotesque_800ExtraBold } from "@expo-google-fonts/bricolage-grotesque";
import { DMSans_400Regular, DMSans_500Medium, DMSans_700Bold } from "@expo-google-fonts/dm-sans";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { AuthProvider, useAuth } from "../lib/auth";
import { pendingInvite } from "../lib/invite";
import { Loading } from "../components/ui";
import { colors } from "../lib/theme";

SplashScreen.preventAutoHideAsync().catch(() => undefined);

const PUBLIC_ROUTES = new Set(["welcome", "j"]);

function Gate() {
  const { user, profile, loading } = useAuth();
  const segments = useSegments() as string[];
  const first = segments[0];

  useEffect(() => {
    if (loading) return;
    if (!user) {
      if (!PUBLIC_ROUTES.has(first)) router.replace("/welcome");
      return;
    }
    if (!profile?.name) {
      if (first !== "setup" && first !== "j") router.replace("/setup");
      return;
    }
    // Signed in and set up: leave the sign-in screens. (Setup handles its own exit.)
    if (first === "welcome" || first === undefined) {
      pendingInvite.take().then((code) => router.replace(code ? `/j/${code}` : "/groups"));
    }
  }, [loading, user, profile?.name, first]);

  // Opening a notification takes you straight to that group.
  useEffect(() => {
    if (Platform.OS === "web") return;
    const sub = Notifications.addNotificationResponseReceivedListener((r) => {
      const url = r.notification.request.content.data?.url;
      if (typeof url === "string") router.push(url as never);
    });
    return () => sub.remove();
  }, []);

  if (loading) return <Loading />;
  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg }, animation: "slide_from_right" }}>
      <Stack.Screen name="welcome" options={{ animation: "fade" }} />
      <Stack.Screen name="(tabs)" options={{ animation: "fade" }} />
    </Stack>
  );
}

export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    BricolageGrotesque_800ExtraBold,
    DMSans_400Regular,
    DMSans_500Medium,
    DMSans_700Bold,
  });

  useEffect(() => {
    if (fontsLoaded) SplashScreen.hideAsync().catch(() => undefined);
  }, [fontsLoaded]);

  if (!fontsLoaded) return null;
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <StatusBar style="dark" />
        <Gate />
      </AuthProvider>
    </SafeAreaProvider>
  );
}
