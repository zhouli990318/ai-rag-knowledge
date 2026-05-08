import { create } from 'zustand';
import type { QueryClient } from '@tanstack/react-query';
import type { Conversation } from '@/entities/chat';
import { chatApi, type StreamChatRequest } from '@/entities/chat';

/** Maximum time (ms) to wait for a stream response before auto-aborting */
const STREAM_TIMEOUT_MS = 60_000;

interface StreamState {
  activeConversationId: number | null;
  setActiveConversation: (id: number | null) => void;

  // Streaming state — persists across route changes
  streaming: boolean;
  streamContent: string;
  streamConversationId: number | null;
  /** User message shown optimistically during streaming */
  optimisticUserMessage: string | null;
  /** Message count snapshot taken before streaming starts, used to detect when refetch brings new data */
  preStreamMessageCount: number;

  /** Internal abort controller — stored in state for proper lifecycle management */
  _abortController: AbortController | null;

  startStream: (
    params: StreamChatRequest,
    queryClient: QueryClient,
    setActiveConversation: (id: number) => void,
    enqueueSnackbar: (msg: string, opts?: any) => void,
  ) => void;
  stopStream: () => void;
  clearStream: () => void;
}

export const useStreamStore = create<StreamState>()((set, get) => ({
  activeConversationId: null,
  setActiveConversation: (id) => set({ activeConversationId: id }),

  streaming: false,
  streamContent: '',
  streamConversationId: null,
  optimisticUserMessage: null,
  preStreamMessageCount: 0,
  _abortController: null,

  startStream: (params, queryClient, setActiveConv, enqueueSnackbar) => {
    // Abort any existing stream
    get()._abortController?.abort();

    const controller = new AbortController();
    // Auto-abort after timeout to prevent indefinite hangs
    const timeoutId = setTimeout(() => controller.abort(), STREAM_TIMEOUT_MS);
    set({ _abortController: controller });

    // Snapshot current API message count so we can detect when refetch brings the new assistant message
    const convData = params.conversationId
      ? queryClient.getQueryData<Conversation>(['conversation', params.conversationId])
      : null;
    const currentMsgCount = convData?.messages?.length ?? 0;

    set({
      streaming: true,
      streamContent: '',
      streamConversationId: params.conversationId ?? null,
      optimisticUserMessage: params.message,
      preStreamMessageCount: currentMsgCount,
    });

    (async () => {
      try {
        const res = await chatApi.streamChat(params, controller.signal);
        if (!res.ok) throw new Error('Stream failed');

        const reader = res.body!.getReader();
        const decoder = new TextDecoder();
        let fullContent = '';
        let buffer = '';

        const parseDataLine = (line: string): string | null => {
          if (!line.startsWith('data:')) return null;
          let content = line.slice(5);
          if (content.startsWith(' ')) content = content.slice(1);
          return content;
        };

        // SSE spec: multiple `data:` lines within a single event (before blank line)
        // must be joined with '\n'. We accumulate them and flush on event boundary.
        let eventDataLines: string[] = [];

        const flushEvent = () => {
          if (eventDataLines.length === 0) return;
          const eventData = eventDataLines.join('\n');
          eventDataLines = [];
          if (eventData === '[DONE]') return;
          fullContent += eventData;
          set({ streamContent: fullContent });
        };

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const parts = buffer.split('\n');
          buffer = parts.pop() || '';
          for (const line of parts) {
            if (line.startsWith(':')) continue; // SSE comment
            if (line === '') {
              // Empty line = event boundary → flush accumulated data lines
              flushEvent();
              continue;
            }
            const data = parseDataLine(line);
            if (data !== null) {
              eventDataLines.push(data);
            }
          }
        }
        // Handle remaining buffer
        if (buffer) {
          const data = parseDataLine(buffer);
          if (data !== null && data !== '[DONE]') {
            eventDataLines.push(data);
          }
        }
        // Flush any un-terminated event
        flushEvent();
        set({ streamContent: fullContent });

        // Stream complete — refresh conversation list and data
        const activeId = get().streamConversationId || params.conversationId;
        if (!activeId) {
          await queryClient.refetchQueries({ queryKey: ['conversations'] });
          const updatedConvs = queryClient.getQueryData<Conversation[]>(['conversations']);
          if (updatedConvs && updatedConvs.length > 0) {
            set({ activeConversationId: updatedConvs[0].id, streamConversationId: updatedConvs[0].id });
          }
        } else {
          queryClient.invalidateQueries({ queryKey: ['conversations'] });
          queryClient.invalidateQueries({ queryKey: ['conversation', activeId] });
        }
      } catch (e: any) {
        if (e.name !== 'AbortError') {
          set({ optimisticUserMessage: null, streamContent: '' });
          enqueueSnackbar(e.message || '发送失败', { variant: 'error' });
        }
      } finally {
        clearTimeout(timeoutId);
        set({ streaming: false, _abortController: null });
      }
    })();
  },

  stopStream: () => {
    get()._abortController?.abort();
    set({ streaming: false, _abortController: null });
  },

  clearStream: () => {
    set({ streamContent: '', optimisticUserMessage: null, streamConversationId: null, preStreamMessageCount: 0 });
  },
}));
