import { View } from "react-native";
import { router, usePathname } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { colors, MAX_WIDTH } from "../lib/theme";
import { ClockIcon, GroupsIcon, UserIcon } from "./icons";
import { Tap, Txt } from "./ui";

const TABS = [
  { href: "/groups", label: "Groups", Icon: GroupsIcon },
  { href: "/history", label: "History", Icon: ClockIcon },
  { href: "/profile", label: "Profile", Icon: UserIcon },
] as const;

export function TabBar() {
  const path = usePathname();
  const insets = useSafeAreaInsets();
  return (
    <View style={{ backgroundColor: colors.card, borderTopWidth: 1, borderTopColor: colors.hairline, alignItems: "center" }}>
      <View
        accessibilityRole="tablist"
        style={{
          width: "100%",
          maxWidth: MAX_WIDTH,
          flexDirection: "row",
          justifyContent: "space-around",
          alignItems: "center",
          paddingHorizontal: 24,
          paddingTop: 8,
          paddingBottom: Math.max(insets.bottom, 20),
        }}
      >
        {TABS.map(({ href, label, Icon }) => {
          const active = path.startsWith(href);
          const color = active ? colors.accent : colors.muted;
          return (
            <Tap
              key={href}
              accessibilityRole="tab"
              accessibilityState={{ selected: active }}
              accessibilityLabel={label}
              onPress={() => !active && router.navigate(href)}
              scaleTo={0.92}
              style={{ minWidth: 64, height: 52, alignItems: "center", justifyContent: "center", gap: 4 }}
            >
              <Icon color={color} />
              <Txt size={12} weight={active ? 700 : 500} color={color}>
                {label}
              </Txt>
            </Tap>
          );
        })}
      </View>
    </View>
  );
}
