import { useState, useEffect, useCallback, useMemo } from 'react';
import { Box } from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { chatApi } from '@/entities/chat/api/chatApi';
import { useStreamStore } from '@/features/stream';
import { useChatConfigStore } from '@/features/chat';
import type { ChatMessage, Conversation } from '@/entities/chat/model/types';
import { useSnackbar } from 'notistack';
import { ConversationList, ChatInput, MessageArea } from '@/widgets/chat';

type DisplayMessage = Pick<ChatMessage, 'role' | 'content'> & {
  id: number | string;
  createdAt?: string;
};

export default function ChatPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const {
    activeConversationId, setActiveConversation,
    streaming, streamContent, streamConversationId, optimisticUserMessage,
    preStreamMessageCount,
    startStream, stopStream, clearStream,
  } = useStreamStore();
  const { selectedProvider, selectedKb, setSelectedProvider, setSelectedKb, resetConfig, toolMode, selectedMcpServers } = useChatConfigStore();

  const [input, setInput] = useState('');

  // Abort stream on unmount to prevent memory leaks
  useEffect(() => {
    return () => {
      if (useStreamStore.getState().streaming) {
        stopStream();
      }
    };
  }, [stopStream]);

  // Clear stream state when switching conversations (different from current stream)
  useEffect(() => {
    if (activeConversationId !== streamConversationId && !streaming) {
      clearStream();
    }
  }, [activeConversationId, streamConversationId, streaming, clearStream]);

  const { data: conversations = [], isLoading: convsLoading } = useQuery({ queryKey: ['conversations'], queryFn: chatApi.getConversations });
  const { data: activeConv } = useQuery({
    queryKey: ['conversation', activeConversationId],
    queryFn: () => chatApi.getConversation(activeConversationId!),
    enabled: !!activeConversationId,
  });

  // Sync config from active conversation
  useEffect(() => {
    if (!activeConv) return;
    if (activeConv.providerId) setSelectedProvider(activeConv.providerId);
    setSelectedKb(activeConv.knowledgeBaseId ?? 0);
  }, [activeConv?.id, setSelectedProvider, setSelectedKb]);

  // Clear optimistic state once real messages arrive from API
  // Only clear when the refetched messages actually include new data (length increased),
  // preventing premature clearing that causes the answer to vanish (Bug 2).
  useEffect(() => {
    if (!streaming && optimisticUserMessage && activeConv?.messages?.length && activeConv.messages.length > preStreamMessageCount) {
      clearStream();
    }
  }, [activeConv?.messages?.length, streaming, optimisticUserMessage, preStreamMessageCount, clearStream]);

  const deleteMutation = useMutation({
    mutationFn: chatApi.deleteConversation,
    onSuccess: () => {
      clearStream();
      setActiveConversation(null);
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      enqueueSnackbar('对话已删除', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });

  const resetDraft = useCallback(() => {
    clearStream();
    resetConfig();
    setActiveConversation(null);
  }, [setActiveConversation, resetConfig, clearStream]);

  const handleSend = () => {
    if (streaming) return;
    if (!input.trim()) return;
    if (!selectedProvider) {
      enqueueSnackbar('请先在右侧配置中选择一个已启用的模型', { variant: 'warning' });
      return;
    }
    const message = input.trim();
    setInput('');

    startStream(
      {
        conversationId: activeConversationId || undefined,
        providerId: selectedProvider,
        knowledgeBaseId: selectedKb || undefined,
        message,
        systemPrompt: selectedKb > 0 ? '请优先根据知识库内容回答。' : undefined,
        toolMode,
        mcpServerIds: toolMode === 'SPECIFIC' ? selectedMcpServers : undefined,
      },
      queryClient,
      (id: number) => setActiveConversation(id),
      enqueueSnackbar,
    );
  };

  const handleStop = useCallback(() => {
    stopStream();
  }, [stopStream]);

  const handleSelectConversation = useCallback((id: number) => {
    if (streaming) stopStream();
    setActiveConversation(id);
  }, [setActiveConversation, streaming, stopStream]);

  const handleDelete = useCallback((id: number) => deleteMutation.mutate(id), [deleteMutation]);

  // Build display messages: API messages + optimistic user message during streaming
  const messages: DisplayMessage[] = useMemo(() => {
    const apiMsgs = activeConv?.messages || [];
    const isStreamingThisConv =
      streaming && (streamConversationId === activeConversationId || (!streamConversationId && !activeConversationId));
    if (isStreamingThisConv && optimisticUserMessage) {
      return [...apiMsgs, { id: 'local-user-stream', role: 'USER' as const, content: optimisticUserMessage }];
    }
    return [...apiMsgs];
  }, [activeConv?.messages, streaming, streamConversationId, activeConversationId, optimisticUserMessage]);

  // Only show stream content if streaming for the currently viewed conversation
  const visibleStreamContent =
    (streamConversationId === activeConversationId || (!streamConversationId && !activeConversationId))
      ? streamContent
      : '';

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Top conversation tabs */}
      <ConversationList
        conversations={conversations}
        activeId={activeConversationId}
        onSelect={handleSelectConversation}
        onDelete={handleDelete}
        onNew={resetDraft}
        isLoading={convsLoading}
      />

      {/* Message area — centered with max width */}
      <Box sx={{ flex: 1, overflow: 'auto', minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ maxWidth: 900, mx: 'auto', width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
          <MessageArea messages={messages} streamContent={visibleStreamContent} streaming={streaming && visibleStreamContent !== ''} conversationId={activeConversationId} onNewChat={resetDraft} onSuggestedClick={setInput} />
        </Box>
      </Box>

      {/* Input — centered with max width */}
      <Box sx={{ maxWidth: 900, mx: 'auto', width: '100%' }}>
        <ChatInput value={input} onChange={setInput} onSend={handleSend} streaming={streaming} onStop={handleStop} />
      </Box>
    </Box>
  );
}
