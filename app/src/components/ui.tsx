import { ReactNode, useEffect } from "react";
import {
  ActivityIndicator,
  Image,
  Platform,
  Pressable,
  PressableProps,
  StyleProp,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  TextProps,
  TextStyle,
  View,
  ViewStyle,
} from "react-native";
import Animated, { useAnimatedStyle, useSharedValue, withSpring, withTiming } from "react-native-reanimated";
import { SafeAreaView, Edge } from "react-native-safe-area-context";
import * as Haptics from "expo-haptics";
import { avatarColors, colors, fonts, MAX_WIDTH } from "../lib/theme";
import { BackIcon, FlameIcon } from "./icons";

// ------------------------------------------------------------------ haptics

export const haptic = {
  tap: () => Platform.OS !== "web" && Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => undefined),
  success: () =>
    Platform.OS !== "web" && Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => undefined),
  select: () => Platform.OS !== "web" && Haptics.selectionAsync().catch(() => undefined),
};

// --------------------------------------------------------------------- text

type Weight = 400 | 500 | 700;
const familyFor: Record<Weight, string> = { 400: fonts.regular, 500: fonts.medium, 700: fonts.bold };

export function Txt({
  size = 15,
  weight = 400,
  color = colors.ink,
  style,
  ...rest
}: TextProps & { size?: number; weight?: Weight; color?: string }) {
  return <Text {...rest} style={[{ fontFamily: familyFor[weight], fontSize: size, color }, style]} />;
}

export function Display({
  size = 32,
  color = colors.ink,
  style,
  ...rest
}: TextProps & { size?: number; color?: string }) {
  return (
    <Text
      accessibilityRole="header"
      {...rest}
      style={[{ fontFamily: fonts.display, fontSize: size, color, letterSpacing: -size * 0.025, lineHeight: size * 1.08 }, style]}
    />
  );
}

// ------------------------------------------------------------------- layout

/** The phone-width column. On web it's centered so every platform looks the same. */
export function Screen({
  children,
  edges = ["top"],
  style,
}: {
  children: ReactNode;
  edges?: Edge[];
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <View style={styles.outer}>
      <SafeAreaView edges={edges} style={styles.column}>
        <View style={[{ flex: 1 }, style]}>{children}</View>
      </SafeAreaView>
    </View>
  );
}

// ----------------------------------------------------------------- pressable

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

/** A pressable that springs down slightly when touched. */
export function Tap({
  style,
  children,
  onPress,
  scaleTo = 0.97,
  hapticOnPress = true,
  ...rest
}: Omit<PressableProps, "style" | "children"> & {
  style?: StyleProp<ViewStyle>;
  children?: ReactNode;
  scaleTo?: number;
  hapticOnPress?: boolean;
}) {
  const scale = useSharedValue(1);
  const animated = useAnimatedStyle(() => ({ transform: [{ scale: scale.value }] }));
  return (
    <AnimatedPressable
      accessibilityRole="button"
      {...rest}
      onPressIn={(e) => {
        scale.value = withSpring(scaleTo, { damping: 20, stiffness: 400 });
        rest.onPressIn?.(e);
      }}
      onPressOut={(e) => {
        scale.value = withSpring(1, { damping: 14, stiffness: 300 });
        rest.onPressOut?.(e);
      }}
      onPress={(e) => {
        if (hapticOnPress) haptic.tap();
        onPress?.(e);
      }}
      style={[animated, style, rest.disabled ? { opacity: 0.5 } : null]}
    >
      {children}
    </AnimatedPressable>
  );
}

type ButtonVariant = "primary" | "dark" | "outline";

export function Button({
  label,
  onPress,
  variant = "primary",
  icon,
  loading,
  disabled,
  style,
}: {
  label: string;
  onPress?: () => void;
  variant?: ButtonVariant;
  icon?: ReactNode;
  loading?: boolean;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
}) {
  const bg = variant === "primary" ? colors.accent : variant === "dark" ? colors.ink : colors.card;
  const fg = variant === "outline" ? colors.ink : colors.white;
  return (
    <Tap
      onPress={onPress}
      disabled={disabled || loading}
      accessibilityLabel={label}
      style={[
        styles.button,
        { backgroundColor: bg },
        variant === "outline" && { borderWidth: 1.5, borderColor: colors.border },
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={fg} />
      ) : (
        <>
          {icon}
          <Txt size={16} weight={700} color={fg}>
            {label}
          </Txt>
        </>
      )}
    </Tap>
  );
}

export function IconButton({
  onPress,
  label,
  children,
  bg = colors.card,
  style,
}: {
  onPress?: () => void;
  label: string;
  children: ReactNode;
  bg?: string;
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <Tap onPress={onPress} accessibilityLabel={label} style={[styles.iconButton, { backgroundColor: bg }, style]}>
      {children}
    </Tap>
  );
}

export function BackButton({ onPress }: { onPress: () => void }) {
  return (
    <IconButton label="Back" onPress={onPress}>
      <BackIcon />
    </IconButton>
  );
}

// ------------------------------------------------------------------- inputs

export function Field({
  label,
  hint,
  focusedBorder = true,
  style,
  ...input
}: TextInputProps & { label: string; hint?: string; focusedBorder?: boolean }) {
  return (
    <View style={{ gap: 8 }}>
      <Txt size={14} weight={700} nativeID={`${label}-label`}>
        {label}
      </Txt>
      <TextInput
        accessibilityLabel={label}
        placeholderTextColor={colors.muted}
        {...input}
        style={[
          styles.input,
          { borderColor: focusedBorder ? colors.accent : colors.border, borderWidth: focusedBorder ? 2 : 1.5 },
          webNoOutline,
          style as StyleProp<TextStyle>,
        ]}
      />
      {hint ? (
        <Txt size={13} color={colors.muted}>
          {hint}
        </Txt>
      ) : null}
    </View>
  );
}

export const webNoOutline = Platform.OS === "web" ? ({ outlineStyle: "none" } as unknown as TextStyle) : null;

export function Toggle({ value, onChange, label }: { value: boolean; onChange: (v: boolean) => void; label: string }) {
  const x = useSharedValue(value ? 20 : 0);
  useEffect(() => {
    x.value = withTiming(value ? 20 : 0, { duration: 180 });
  }, [value, x]);
  const knob = useAnimatedStyle(() => ({ transform: [{ translateX: x.value }] }));
  return (
    <Pressable
      accessibilityRole="switch"
      accessibilityLabel={label}
      accessibilityState={{ checked: value }}
      onPress={() => {
        haptic.select();
        onChange(!value);
      }}
      hitSlop={8}
      style={[styles.toggle, { backgroundColor: value ? colors.accent : colors.toggleOff }]}
    >
      <Animated.View style={[styles.knob, knob]} />
    </Pressable>
  );
}

// ------------------------------------------------------------------ avatars

export function initials(name?: string | null): string {
  const clean = (name ?? "").trim();
  if (!clean) return "?";
  const words = clean.split(/\s+/);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
}

export function colorFor(id: string): string {
  let h = 0;
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) | 0;
  return avatarColors[Math.abs(h) % avatarColors.length];
}

export function Avatar({
  id,
  name,
  photoURL,
  size = 30,
  ring,
  dashed,
  label,
  bg,
  style,
}: {
  id: string;
  name?: string | null;
  photoURL?: string | null;
  size?: number;
  ring?: boolean;
  dashed?: boolean;
  label?: string;
  bg?: string;
  style?: StyleProp<ViewStyle>;
}) {
  const box: ViewStyle = {
    width: size,
    height: size,
    borderRadius: size / 2,
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
    backgroundColor: dashed ? "transparent" : bg ?? colorFor(id),
    ...(ring ? { borderWidth: 2, borderColor: colors.white } : null),
    ...(dashed ? { borderWidth: 1.5, borderStyle: "dashed", borderColor: colors.dashed } : null),
  };
  return (
    <View style={[box, style]} accessibilityLabel={name ?? undefined}>
      {photoURL && !dashed ? (
        <Image source={{ uri: photoURL }} style={{ width: size, height: size }} />
      ) : (
        <Txt size={Math.max(11, size * 0.36)} weight={700} color={dashed ? colors.muted2 : colors.ink}>
          {label ?? initials(name)}
        </Txt>
      )}
    </View>
  );
}

export function AvatarStack({
  people,
  max = 3,
  size = 30,
}: {
  people: { id: string; name?: string | null; photoURL?: string | null }[];
  max?: number;
  size?: number;
}) {
  const shown = people.slice(0, max);
  const extra = people.length - shown.length;
  return (
    <View style={{ flexDirection: "row" }}>
      {shown.map((p, i) => (
        <Avatar key={p.id} {...p} size={size} ring style={i ? { marginLeft: -8 } : null} />
      ))}
      {extra > 0 ? (
        <View
          style={{
            width: size,
            height: size,
            borderRadius: size / 2,
            marginLeft: -8,
            borderWidth: 2,
            borderColor: colors.white,
            backgroundColor: colors.track,
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Txt size={11} weight={700}>
            +{extra}
          </Txt>
        </View>
      ) : null}
    </View>
  );
}

// -------------------------------------------------------------------- bits

export function Pill({ label, bg, fg }: { label: string; bg: string; fg: string }) {
  return (
    <View style={{ height: 30, paddingHorizontal: 12, borderRadius: 15, backgroundColor: bg, justifyContent: "center" }}>
      <Txt size={13} weight={700} color={fg}>
        {label}
      </Txt>
    </View>
  );
}

export function StreakChip({ streak, compact }: { streak: number; compact?: boolean }) {
  if (compact) {
    return (
      <View style={{ flexDirection: "row", alignItems: "center", gap: 4 }} accessibilityLabel={`${streak}-day streak`}>
        <FlameIcon color={streak > 0 ? colors.flame : colors.muted} />
        <Txt size={14} weight={700} color={streak > 0 ? colors.flame : colors.muted}>
          {streak}
        </Txt>
      </View>
    );
  }
  return (
    <View
      accessibilityLabel={`${streak}-day streak`}
      style={{
        height: 34,
        paddingHorizontal: 12,
        borderRadius: 17,
        backgroundColor: colors.flameBg,
        flexDirection: "row",
        alignItems: "center",
        gap: 4,
      }}
    >
      <FlameIcon color={colors.flameInk} />
      <Txt size={14} weight={700} color={colors.flameInk}>
        {streak}
      </Txt>
    </View>
  );
}

export function Loading() {
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", backgroundColor: colors.bg }}>
      <ActivityIndicator color={colors.accent} />
    </View>
  );
}

export function ErrorText({ message }: { message?: string | null }) {
  if (!message) return null;
  return (
    <Txt size={14} weight={500} color={colors.danger} accessibilityLiveRegion="polite">
      {message}
    </Txt>
  );
}

const styles = StyleSheet.create({
  outer: { flex: 1, backgroundColor: colors.bg, alignItems: "center" },
  column: { flex: 1, width: "100%", maxWidth: MAX_WIDTH },
  button: {
    height: 54,
    borderRadius: 16,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
  },
  iconButton: { width: 44, height: 44, borderRadius: 14, alignItems: "center", justifyContent: "center" },
  input: {
    height: 54,
    paddingHorizontal: 16,
    borderRadius: 14,
    backgroundColor: colors.card,
    fontFamily: fonts.regular,
    fontSize: 17,
    color: colors.ink,
  },
  toggle: { width: 50, height: 30, borderRadius: 15, padding: 3 },
  knob: { width: 24, height: 24, borderRadius: 12, backgroundColor: colors.white },
});
