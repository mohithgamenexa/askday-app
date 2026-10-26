export const colors = {
  bg: "#F6F5FB",
  card: "#FFFFFF",
  ink: "#1B1830",
  muted: "#5E5A72",
  muted2: "#4A4660",
  border: "#DAD7E6",
  hairline: "#E6E3EF",
  divider: "#EEECF4",
  track: "#ECEAF3",
  dashed: "#9E99B3",
  dashedLight: "#B9B4CC",
  toggleOff: "#CFCBDD",
  accent: "#4B3BFF",
  accentPressed: "#3326D6",
  accentSoft: "#EEEBFF",
  accentOnAccent: "#E4E0FF",
  darkMuted: "#C9C5DA",
  flame: "#B84A00",
  flameBg: "#FFE8D6",
  flameInk: "#8A3700",
  successBg: "#D9F2E3",
  successInk: "#0B6B3A",
  danger: "#B42318",
  white: "#FFFFFF",
};

export const avatarColors = ["#FFD9C2", "#D6E4FF", "#D9F2E3", "#F7D6F0", "#FFF0B3", "#E2DCFF"];

export const fonts = {
  display: "BricolageGrotesque_800ExtraBold",
  regular: "DMSans_400Regular",
  medium: "DMSans_500Medium",
  bold: "DMSans_700Bold",
};

export const shadow = {
  card: { boxShadow: "0 2px 10px rgba(27,24,48,0.05)" },
  float: { boxShadow: "0 6px 20px rgba(27,24,48,0.08)" },
  segment: { boxShadow: "0 1px 4px rgba(27,24,48,0.08)" },
} as const;

/** The phone-width column the whole app is laid out in, also on web. */
export const MAX_WIDTH = 430;
