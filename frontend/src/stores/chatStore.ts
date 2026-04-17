import { create } from 'zustand';

interface ChatState {
  activeConversationId: number | null;
  setActiveConversation: (id: number | null) => void;
}

export const useChatStore = create<ChatState>()((set) => ({
  activeConversationId: null,
  setActiveConversation: (id) => set({ activeConversationId: id }),
}));
