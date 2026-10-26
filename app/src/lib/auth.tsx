import { createContext, ReactNode, useContext, useEffect, useMemo } from "react";
import { isHydrated, Profile, useDB } from "./store";
import { resetEverything, startEngine } from "./engine";
import { askNotificationPermission } from "./notify";

type Guest = { uid: string };
type AuthState = {
  user: Guest | null;
  profile: Profile | null;
  /** True until saved data has loaded from the device. */
  loading: boolean;
  /** Guests can't come back after logging out, so this erases everything. */
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthState>({ user: null, profile: null, loading: true, signOut: async () => {} });

export function AuthProvider({ children }: { children: ReactNode }) {
  const db = useDB();
  useEffect(() => startEngine(), []);

  const hasName = !!db.profile?.name;
  useEffect(() => {
    if (hasName) askNotificationPermission().catch(() => undefined);
  }, [hasName]);

  const value = useMemo<AuthState>(
    () => ({ user: db.me, profile: db.profile, loading: !isHydrated(), signOut: resetEverything }),
    [db.me, db.profile, db],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
