import { create } from 'zustand';

interface ThemeState {
  mode: 'light';
}

export const useThemeStore = create<ThemeState>()(() => ({
  mode: 'light' as const,
}));
