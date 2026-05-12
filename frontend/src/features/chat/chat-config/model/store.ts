import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type ToolMode = 'OFF' | 'AUTO' | 'SPECIFIC';

interface ChatConfigState {
  selectedProvider: number;
  selectedKb: number;
  toolMode: ToolMode;
  selectedMcpServers: number[];
  filterExpression: string;
  setSelectedProvider: (id: number) => void;
  setSelectedKb: (id: number) => void;
  setToolMode: (mode: ToolMode) => void;
  setSelectedMcpServers: (ids: number[]) => void;
  setFilterExpression: (value: string) => void;
  resetConfig: () => void;
}

export const useChatConfigStore = create<ChatConfigState>()(
  persist(
    (set) => ({
      selectedProvider: 0,
      selectedKb: 0,
      toolMode: 'AUTO',
      selectedMcpServers: [],
      filterExpression: '',
      setSelectedProvider: (id) => set({ selectedProvider: id }),
      setSelectedKb: (id) => set({ selectedKb: id }),
      setToolMode: (mode) => set({ toolMode: mode }),
      setSelectedMcpServers: (ids) => set({ selectedMcpServers: ids }),
      setFilterExpression: (value) => set({ filterExpression: value }),
      resetConfig: () => set({ selectedKb: 0, filterExpression: '' }),
    }),
    { name: 'chat-config' }
  )
);
