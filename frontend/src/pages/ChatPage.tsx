import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Box, useTheme, useMediaQuery } from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { chatApi } from '../api/chatApi';
import { mcpGatewayApi } from '../api/mcpApi';
import { providerApi } from '../api/providerApi';
import { knowledgeApi } from '../api/knowledgeApi';
import { useChatStore } from '../stores/chatStore';
import { ChatMessage, Conversation, KnowledgeBase, McpApiSource, Provider } from '../api/types';
import { useSnackbar } from 'notistack';
import ConversationList from './chat/ConversationList';
import ChatConfig from './chat/ChatConfig';
import ChatInput from './chat/ChatInput';
import MessageArea from './chat/MessageArea';

type DisplayMessage = Pick<ChatMessage, 'role' | 'content'> & {
  id: number | string;
  createdAt?: string;
};

export default function ChatPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const isDark = theme.palette.mode === 'dark';

  const { activeConversationId, setActiveConversation } = useChatStore();
  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [streamContent, setStreamContent] = useState('');
  const [selectedProvider, setSelectedProvider] = useState<number>(0);
  const [selectedKb, setSelectedKb] = useState<number>(0);
  const [selectedMcpServers, setSelectedMcpServers] = useState<number[]>([]);
  const [optimisticMessages, setOptimisticMessages] = useState<DisplayMessage[]>([]);
  const [showList, setShowList] = useState(true); // mobile: toggle list vs chat
  const abortControllerRef = useRef<AbortController | null>(null);

  // Cleanup stream on unmount
  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort();
    };
  }, []);

  const { data: conversations = [], isLoading: convsLoading } = useQuery({ queryKey: ['conversations'], queryFn: chatApi.getConversations });
  const { data: providers = [] } = useQuery({ queryKey: ['providers'], queryFn: providerApi.list });
  const { data: kbs = [] } = useQuery({ queryKey: ['knowledgeBases'], queryFn: knowledgeApi.list });
  const { data: mcpSources = [] } = useQuery({ queryKey: ['mcp-sources'], queryFn: mcpGatewayApi.listSources });
  const { data: activeConv } = useQuery({
    queryKey: ['conversation', activeConversationId],
    queryFn: () => chatApi.getConversation(activeConversationId!),
    enabled: !!activeConversationId,
  });

  const enabledProviders = useMemo(() => providers.filter((p: Provider) => p.enabled), [providers]);
  const activeMcpSources = useMemo(() => mcpSources.filter((s: McpApiSource) => s.active), [mcpSources]);

  useEffect(() => {
    if (enabledProviders.length > 0 && !selectedProvider) {
      setSelectedProvider(enabledProviders[0].id);
    }
  }, [enabledProviders.length, enabledProviders, selectedProvider]);

  useEffect(() => {
    if (!streaming && activeConv?.messages?.length) {
      setOptimisticMessages([]);
      if (streamContent) setStreamContent('');
    }
  }, [activeConv?.messages, streaming, streamContent]);

  useEffect(() => {
    if (!activeConv) return;
    if (activeConv.providerId) setSelectedProvider(activeConv.providerId);
    setSelectedKb(activeConv.knowledgeBaseId ?? 0);
    setSelectedMcpServers(activeConv.mcpServerIds ?? []);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeConv?.id, activeConv?.providerId, activeConv?.knowledgeBaseId]);

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
    setSelectedKb(0);
    setSelectedMcpServers([]);
    setActiveConversation(null);
  }, [setActiveConversation]);

  const handleSend = async () => {
    if (!input.trim() || !selectedProvider || streaming) return;
    const message = input.trim();
    setInput('');
    setStreaming(true);
    setStreamContent('');
    setOptimisticMessages((cur) => [...cur, { id: `local-user-${Date.now()}`, role: 'USER', content: message }]);

    if (isMobile) setShowList(false);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      const res = await chatApi.streamChat({
        conversationId: activeConversationId || undefined,
        providerId: selectedProvider,
        knowledgeBaseId: selectedKb || undefined,
        message,
        systemPrompt: selectedKb > 0 ? '请优先根据知识库内容回答。' : undefined,
        mcpServerIds: selectedMcpServers,
      }, controller.signal);

      if (!res.ok) throw new Error('Stream failed');

      const reader = res.body!.getReader();
      const decoder = new TextDecoder();
      let fullContent = '';
      let rafId = 0;
      let pendingUpdate = false;

      const flushContent = () => {
        setStreamContent(fullContent);
        pendingUpdate = false;
      };

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (data === '[DONE]') continue;
            fullContent += data;
            if (!pendingUpdate) {
              pendingUpdate = true;
              rafId = requestAnimationFrame(flushContent);
            }
          }
        }
      }
      cancelAnimationFrame(rafId);
      setStreamContent(fullContent);

      if (!activeConversationId) {
        // New conversation created by backend during streaming — fetch and link
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
        setOptimisticMessages((cur) => cur.filter((m) => m.content !== message || m.role !== 'USER'));
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
    if (isMobile) setShowList(false);
  }, [setActiveConversation, isMobile]);

  const handleDelete = useCallback((id: number) => deleteMutation.mutate(id), [deleteMutation]);

  const messages: DisplayMessage[] = useMemo(
    () => [...(activeConv?.messages || []), ...optimisticMessages],
    [activeConv?.messages, optimisticMessages],
  );

  // ---------- MOBILE ----------
  if (isMobile) {
    if (showList) {
      return (
        <Box sx={{ height: '100%' }}>
          <ConversationList
            conversations={conversations}
            activeId={activeConversationId}
            onSelect={handleSelectConversation}
            onDelete={handleDelete}
            onNew={() => { resetDraft(); setShowList(false); }}
            isLoading={convsLoading}
          />
        </Box>
      );
    }

    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {/* Back button */}
        <Box
          onClick={() => setShowList(true)}
          sx={{
            px: 1, py: 0.75, cursor: 'pointer', display: 'flex', alignItems: 'center',
            color: '#007AFF', fontSize: 17,
            '&:active': { opacity: 0.6 },
          }}
        >
          ‹ 对话列表
        </Box>
        <ChatConfig
          selectedProvider={selectedProvider} setSelectedProvider={setSelectedProvider}
          selectedKb={selectedKb} setSelectedKb={setSelectedKb}
          selectedMcpServers={selectedMcpServers} setSelectedMcpServers={setSelectedMcpServers}
          providers={enabledProviders} kbs={kbs} mcpSources={activeMcpSources}
        />
        <MessageArea messages={messages} streamContent={streamContent} streaming={streaming} />
        <ChatInput value={input} onChange={setInput} onSend={handleSend} streaming={streaming} onStop={handleStop} />
      </Box>
    );
  }

  // ---------- DESKTOP ----------
  return (
    <Box sx={{ display: 'flex', height: '100%' }}>
      {/* Left panel */}
      <Box sx={{
        width: 280, flexShrink: 0,
        borderRight: `0.5px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'}`,
        backgroundColor: isDark ? 'rgba(28,28,30,0.4)' : 'rgba(242,242,247,0.5)',
      }}>
        <ConversationList
          conversations={conversations}
          activeId={activeConversationId}
          onSelect={handleSelectConversation}
          onDelete={handleDelete}
          onNew={resetDraft}
          isLoading={convsLoading}
        />
      </Box>

      {/* Right panel - chat */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <ChatConfig
          selectedProvider={selectedProvider} setSelectedProvider={setSelectedProvider}
          selectedKb={selectedKb} setSelectedKb={setSelectedKb}
          selectedMcpServers={selectedMcpServers} setSelectedMcpServers={setSelectedMcpServers}
          providers={enabledProviders} kbs={kbs} mcpSources={activeMcpSources}
        />
        <MessageArea messages={messages} streamContent={streamContent} streaming={streaming} onNewChat={resetDraft} />
        <ChatInput value={input} onChange={setInput} onSend={handleSend} streaming={streaming} onStop={handleStop} />
      </Box>
    </Box>
  );
}
