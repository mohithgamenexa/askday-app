import Svg, { Path, Rect, Circle } from "react-native-svg";

type IconProps = { size?: number; color?: string; strokeWidth?: number };

const base = (size: number, color: string, strokeWidth: number) => ({
  width: size,
  height: size,
  viewBox: "0 0 24 24",
  fill: "none" as const,
  stroke: color,
  strokeWidth,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
});

export function BackIcon({ size = 22, color = "#1B1830", strokeWidth = 2 }: IconProps) {
  return <Svg {...base(size, color, strokeWidth)}><Path d="M15 18l-6-6 6-6" /></Svg>;
}
export function ChevronIcon({ size = 16, color = "#5E5A72", strokeWidth = 2 }: IconProps) {
  return <Svg {...base(size, color, strokeWidth)}><Path d="M9 6l6 6-6 6" /></Svg>;
}
export function PlusIcon({ size = 22, color = "#FFFFFF", strokeWidth = 2.2 }: IconProps) {
  return <Svg {...base(size, color, strokeWidth)}><Path d="M12 5v14M5 12h14" /></Svg>;
}
export function FlameIcon({ size = 16, color = "#B84A00", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Path d="M12 3c1 3.5 5 5.5 5 10a5 5 0 0 1-10 0c0-2 1-3.5 2-4.5 0 2 1 3 2 3 0-3-1-5.5 1-8.5z" />
    </Svg>
  );
}
export function LockIcon({ size = 16, color = "#FFFFFF", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Rect x="5" y="11" width="14" height="10" rx="2" />
      <Path d="M8 11V7a4 4 0 0 1 8 0v4" />
    </Svg>
  );
}
export function PhoneIcon({ size = 20, color = "#FFFFFF", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Rect x="6" y="2" width="12" height="20" rx="3" />
      <Path d="M11 18h2" />
    </Svg>
  );
}
export function CameraIcon({ size = 20, color = "#FFFFFF", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Path d="M4 8h3l2-3h6l2 3h3v11H4z" />
      <Circle cx="12" cy="13" r="3.5" />
    </Svg>
  );
}
export function CheckIcon({ size = 14, color = "#0B6B3A", strokeWidth = 2.4 }: IconProps) {
  return <Svg {...base(size, color, strokeWidth)}><Path d="M5 12l5 5 9-10" /></Svg>;
}
export function HeartIcon({ size = 14, color = "#4A4660", filled = false }: IconProps & { filled?: boolean }) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill={filled ? color : "none"} stroke={color} strokeWidth={2} strokeLinejoin="round">
      <Path d="M12 21s-7.5-4.6-9.5-9.3C1 8 3.4 4.5 7 4.5c2 0 3.6 1.1 5 3 1.4-1.9 3-3 5-3 3.6 0 6 3.5 4.5 7.2C19.5 16.4 12 21 12 21z" />
    </Svg>
  );
}
export function ChatIcon({ size = 14, color = "#4A4660", strokeWidth = 2 }: IconProps) {
  return <Svg {...base(size, color, strokeWidth)}><Path d="M4 5h16v11H9l-5 4z" /></Svg>;
}
export function BellIcon({ size = 16, color = "#1B1830", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <Path d="M10 21h4" />
    </Svg>
  );
}
export function SendIcon({ size = 20, color = "#FFFFFF", strokeWidth = 2.2 }: IconProps) {
  return <Svg {...base(size, color, strokeWidth)}><Path d="M5 12h14M13 6l6 6-6 6" /></Svg>;
}
export function GroupsIcon({ size = 24, color = "#5E5A72", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Circle cx="9" cy="8" r="3.5" />
      <Path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
      <Path d="M16 4.5a3.5 3.5 0 0 1 0 7M18 14.5a6.5 6.5 0 0 1 3.5 5.5" />
    </Svg>
  );
}
export function ClockIcon({ size = 24, color = "#5E5A72", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Circle cx="12" cy="12" r="9" />
      <Path d="M12 7v5l3 2" />
    </Svg>
  );
}
export function UserIcon({ size = 24, color = "#5E5A72", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Circle cx="12" cy="8" r="4" />
      <Path d="M4 21a8 8 0 0 1 16 0" />
    </Svg>
  );
}
export function WhatsAppIcon({ size = 22, color = "#1B1830", strokeWidth = 2 }: IconProps) {
  return <Svg {...base(size, color, strokeWidth)}><Path d="M21 12a8.5 8.5 0 0 1-12.5 7.5L3 21l1.5-5A8.5 8.5 0 1 1 21 12z" /></Svg>;
}
export function InstagramIcon({ size = 22, color = "#1B1830", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Rect x="3" y="3" width="18" height="18" rx="5" />
      <Circle cx="12" cy="12" r="4" />
    </Svg>
  );
}
export function MessageIcon({ size = 22, color = "#1B1830", strokeWidth = 2 }: IconProps) {
  return <Svg {...base(size, color, strokeWidth)}><Path d="M4 5h16v11H9l-5 4z" /></Svg>;
}
export function MoreIcon({ size = 22, color = "#1B1830", strokeWidth = 2 }: IconProps) {
  return (
    <Svg {...base(size, color, strokeWidth)}>
      <Circle cx="5" cy="12" r="1.5" />
      <Circle cx="12" cy="12" r="1.5" />
      <Circle cx="19" cy="12" r="1.5" />
    </Svg>
  );
}
