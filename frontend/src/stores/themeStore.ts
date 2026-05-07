import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface ThemeState {
  mode: 'light' | 'dark';
  immersive: boolean;
  toggleMode: () => void;
  toggleImmersive: () => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      mode: 'light',
      immersive: false,
      toggleMode: () => set((s) => ({ mode: s.mode === 'light' ? 'dark' : 'light' })),
      toggleImmersive: () => set((s) => ({ immersive: !s.immersive })),
    }),
    { name: 'ink-theme' },
  ),
);
