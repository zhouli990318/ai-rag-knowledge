import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Box } from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { chatApi } from '../api/chatApi';
import { useChatStore } from '../stores/chatStore';
import { useChatConfigStore } from '../stores/chatConfigStore';
import { ChatMessage, Conversation } from '../api/types';
import { useSnackbar } from 'notistack';
import ConversationList from './chat/ConversationList';
import ChatInput from './chat/ChatInput';
import MessageArea from './chat/MessageArea';

type DisplayMessage = Pick<ChatMessage, 'role' | 'content'> & {
  id: number | string;
  createdAt?: string;
};

export default function ChatPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const { activeConversationId, setActiveConversation } = useChatStore();
  const { selectedProvider, selectedKb, setSelectedProvider, setSelectedKb, resetConfig, toolMode, selectedMcpServers } = useChatConfigStore();

  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [streamContent, setStreamContent] = useState('');
  const [optimisticMessages, setOptimisticMessages] = useState<DisplayMessage[]>([]);
  const abortControllerRef = useRef<AbortController | null>(null);
  const mountedRef = useRef(true);
  const rafRef = useRef(0);

  // Cleanup stream on unmount
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      cancelAnimationFrame(rafRef.current);
      abortControllerRef.current?.abort();
    };
  }, []);

  // Abort stream and clear state when switching conversations
  useEffect(() => {
    abortControllerRef.current?.abort();
    setStreaming(false);
    setStreamContent('');
    setOptimisticMessages([]);
  }, [activeConversationId]);

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

  useEffect(() => {
    if (!streaming && activeConv?.messages?.length) {
      setOptimisticMessages([]);
      if (streamContent) setStreamContent('');
    }
  }, [activeConv?.messages, streaming, streamContent]);

  const deleteMutation = useMutation({
    mutationFn: chatApi.deleteConversation,
    onSuccess: () => {
      setOptimisticMessages([]);
      setStreamContent('');
      setActiveConversation(null);
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      enqueueSnackbar('对话已删除', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });

  const resetDraft = useCallback(() => {
    setOptimisticMessages([]);
    setStreamContent('');
    resetConfig();
    setActiveConversation(null);
  }, [setActiveConversation, resetConfig]);

  const handleSend = async () => {
    if (streaming) return;
    if (!input.trim()) return;
    if (!selectedProvider) {
      enqueueSnackbar('请先在右侧配置中选择一个已启用的模型', { variant: 'warning' });
      return;
    }
    const message = input.trim();
    const localMsgId = `local-user-${Date.now()}`;
    setInput('');
    setStreaming(true);
    setStreamContent('');
    setOptimisticMessages((cur) => [...cur, { id: localMsgId, role: 'USER', content: message }]);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      const res = await chatApi.streamChat({
        conversationId: activeConversationId || undefined,
        providerId: selectedProvider,
        knowledgeBaseId: selectedKb || undefined,
        message,
        systemPrompt: selectedKb > 0 ? '请优先根据知识库内容回答。' : undefined,
        toolMode,
        mcpServerIds: toolMode === 'SPECIFIC' ? selectedMcpServers : undefined,
      }, controller.signal);

      if (!res.ok) throw new Error('Stream failed');

      const reader = res.body!.getReader();
      const decoder = new TextDecoder();
      let fullContent = '';
      let pendingUpdate = false;
      let buffer = '';

      const flushContent = () => {
        if (mountedRef.current) setStreamContent(fullContent);
        pendingUpdate = false;
      };

      // 解析单个 SSE data 行：仅去除协议分隔符，不裁剪内容空白
      const parseDataLine = (line: string): string | null => {
        if (!line.startsWith('data:')) return null;
        // SSE 规范允许 "data:xxx" 或 "data: xxx"，仅移除一个可选前导空格
        let content = line.slice(5);
        if (content.startsWith(' ')) content = content.slice(1);
        return content;
      };

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const parts = buffer.split('\n');
        buffer = parts.pop() || '';
        for (const line of parts) {
          // 跳过事件分隔空行和其他 SSE 字段（event:、id:、retry:）
          if (line === '' || line.startsWith(':')) continue;
          const data = parseDataLine(line);
          if (data === null) continue;
          if (data === '[DONE]') continue;
          fullContent += data;
          if (!pendingUpdate) {
            pendingUpdate = true;
            rafRef.current = requestAnimationFrame(flushContent);
          }
        }
      }
      // process remaining buffer
      const tailData = parseDataLine(buffer);
      if (tailData !== null && tailData !== '[DONE]') {
        fullContent += tailData;
      }
      cancelAnimationFrame(rafRef.current);
      if (mountedRef.current) setStreamContent(fullContent);

      if (!activeConversationId) {
        await queryClient.refetchQueries({ queryKey: ['conversations'] });
        const updatedConvs = queryClient.getQueryData<Conversation[]>(['conversations']);
        if (updatedConvs && updatedConvs.length > 0) {
          setActiveConversation(updatedConvs[0].id);
        }
      } else {
        queryClient.invalidateQueries({ queryKey: ['conversations'] });
        queryClient.invalidateQueries({ queryKey: ['conversation', activeConversationId] });
      }
    } catch (e: any) {
      if (e.name !== 'AbortError') {
        setOptimisticMessages((cur) => cur.filter((m) => m.id !== localMsgId));
        setStreamContent('');
        enqueueSnackbar(e.message || '发送失败', { variant: 'error' });
      }
    } finally {
      setStreaming(false);
      abortControllerRef.current = null;
    }
  };

  const handleStop = useCallback(() => {
    abortControllerRef.current?.abort();
    setStreaming(false);
  }, []);

  const handleSelectConversation = useCallback((id: number) => {
    setActiveConversation(id);
  }, [setActiveConversation]);

  const handleDelete = useCallback((id: number) => deleteMutation.mutate(id), [deleteMutation]);

  const messages: DisplayMessage[] = useMemo(
    () => [...(activeConv?.messages || []), ...optimisticMessages],
    [activeConv?.messages, optimisticMessages],
  );

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

      {/* Message area */}
      <MessageArea messages={messages} streamContent={streamContent} streaming={streaming} conversationId={activeConversationId} onNewChat={resetDraft} onSuggestedClick={setInput} />

      {/* Input */}
      <ChatInput value={input} onChange={setInput} onSend={handleSend} streaming={streaming} onStop={handleStop} />
    </Box>
  );
}
