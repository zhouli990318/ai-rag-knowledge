import { useState, useRef, useEffect, useCallback } from 'react';
import {
  Box, TextField, IconButton, Paper, Typography, Select, MenuItem,
  FormControl, InputLabel, Chip, List, ListItemButton, ListItemText,
  Divider, Button, CircularProgress, Stack, Avatar,
} from '@mui/material';
import { Send, Add, Delete, SmartToy, Person } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { chatApi } from '../api/chatApi';
import { mcpGatewayApi } from '../api/mcpApi';
import { providerApi } from '../api/providerApi';
import { knowledgeApi } from '../api/knowledgeApi';
import { useChatStore } from '../stores/chatStore';
import { ChatMessage, Conversation, KnowledgeBase, McpApiSource, Provider } from '../api/types';
import MarkdownRenderer from '../components/MarkdownRenderer';
import { useSnackbar } from 'notistack';

type DisplayMessage = Pick<ChatMessage, 'role' | 'content'> & {
  id: number | string;
  createdAt?: string;
};

export default function ChatPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const { activeConversationId, setActiveConversation } = useChatStore();
  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [streamContent, setStreamContent] = useState('');
  const [selectedProvider, setSelectedProvider] = useState<number>(0);
  const [selectedKb, setSelectedKb] = useState<number>(0);
  const [selectedMcpServers, setSelectedMcpServers] = useState<number[]>([]);
  const [optimisticMessages, setOptimisticMessages] = useState<DisplayMessage[]>([]);
  const [pendingConversationLink, setPendingConversationLink] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const { data: conversations = [] } = useQuery({ queryKey: ['conversations'], queryFn: chatApi.getConversations });
  const { data: providers = [] } = useQuery({ queryKey: ['providers'], queryFn: providerApi.list });
  const { data: kbs = [] } = useQuery({ queryKey: ['knowledgeBases'], queryFn: knowledgeApi.list });
  const { data: mcpSources = [] } = useQuery({ queryKey: ['mcp-sources'], queryFn: mcpGatewayApi.listSources });
  const { data: activeConv } = useQuery({
    queryKey: ['conversation', activeConversationId],
    queryFn: () => chatApi.getConversation(activeConversationId!),
    enabled: !!activeConversationId,
  });

  const enabledProviders = providers.filter((p: Provider) => p.enabled);
  const activeMcpSources = mcpSources.filter((source: McpApiSource) => source.active);

  useEffect(() => {
    if (enabledProviders.length > 0 && !selectedProvider) {
      setSelectedProvider(enabledProviders[0].id);
    }
  }, [enabledProviders, selectedProvider]);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(scrollToBottom, [activeConv?.messages, optimisticMessages, streamContent, scrollToBottom]);

  useEffect(() => {
    if (pendingConversationLink && !activeConversationId && conversations.length > 0) {
      setActiveConversation(conversations[0].id);
      setPendingConversationLink(false);
    }
  }, [pendingConversationLink, activeConversationId, conversations, setActiveConversation]);

  useEffect(() => {
    if (!streaming && activeConv?.messages?.length) {
      setOptimisticMessages([]);
      if (streamContent) {
        setStreamContent('');
      }
    }
  }, [activeConv?.messages, streaming, streamContent]);

  useEffect(() => {
    if (!activeConv) {
      return;
    }

    if (activeConv.providerId) {
      setSelectedProvider(activeConv.providerId);
    }
    setSelectedKb(activeConv.knowledgeBaseId ?? 0);
    setSelectedMcpServers(activeConv.mcpServerIds ?? []);
  }, [activeConv]);

  const deleteMutation = useMutation({
    mutationFn: chatApi.deleteConversation,
    onSuccess: () => {
      setOptimisticMessages([]);
      setStreamContent('');
      setActiveConversation(null);
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });

  const handleSend = async () => {
    if (!input.trim() || !selectedProvider || streaming) return;
    const message = input.trim();
    setInput('');
    setStreaming(true);
    setStreamContent('');
    setOptimisticMessages((current) => ([
      ...current,
      {
        id: `local-user-${Date.now()}`,
        role: 'USER',
        content: message,
      },
    ]));

    if (!activeConversationId) {
      setPendingConversationLink(true);
    }

    try {
      const res = await chatApi.streamChat({
        conversationId: activeConversationId || undefined,
        providerId: selectedProvider,
        knowledgeBaseId: selectedKb || undefined,
        message,
        systemPrompt: selectedKb > 0 ? '请优先根据知识库内容回答。' : undefined,
        mcpServerIds: selectedMcpServers,
      });

      if (!res.ok) throw new Error('Stream failed');

      const reader = res.body!.getReader();
      const decoder = new TextDecoder();
      let fullContent = '';

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
            setStreamContent(fullContent);
          }
        }
      }

      // Refresh conversations
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      if (activeConversationId) {
        queryClient.invalidateQueries({ queryKey: ['conversation', activeConversationId] });
      }
    } catch (e: any) {
      setOptimisticMessages((current) => current.filter((item) => item.content !== message || item.role !== 'USER'));
      setStreamContent('');
      enqueueSnackbar(e.message || '发送失败', { variant: 'error' });
    } finally {
      setStreaming(false);
    }
  };

  const resetDraftConversation = () => {
    setOptimisticMessages([]);
    setStreamContent('');
    setPendingConversationLink(false);
    setSelectedKb(0);
    setSelectedMcpServers([]);
    setActiveConversation(null);
  };

  const messages: DisplayMessage[] = [...(activeConv?.messages || []), ...optimisticMessages];

  const renderMessage = (message: DisplayMessage, isStreamingMessage = false) => {
    const isAssistant = message.role === 'ASSISTANT';

    return (
      <Box
        key={message.id}
        sx={{
          display: 'flex',
          flexDirection: isAssistant ? 'row-reverse' : 'row',
          gap: 1.5,
          alignItems: 'flex-start',
        }}
      >
        <Avatar sx={{ width: 32, height: 32, bgcolor: isAssistant ? 'secondary.main' : 'primary.main' }}>
          {isAssistant ? <SmartToy fontSize="small" /> : <Person fontSize="small" />}
        </Avatar>
        <Paper
          variant="outlined"
          sx={{
            p: 1.5,
            maxWidth: { xs: '100%', md: '80%' },
            minWidth: 0,
            bgcolor: isAssistant ? 'action.hover' : 'background.paper',
          }}
        >
          <Box sx={{ minWidth: 0 }}>
            <MarkdownRenderer content={message.content} />
            {isStreamingMessage && streaming && (
              <Box
                component="span"
                sx={{
                  display: 'inline-block',
                  width: 8,
                  height: '1em',
                  ml: 0.5,
                  verticalAlign: 'text-bottom',
                  bgcolor: 'secondary.main',
                  animation: 'chat-cursor-blink 1s step-end infinite',
                  '@keyframes chat-cursor-blink': {
                    '50%': { opacity: 0 },
                  },
                }}
              />
            )}
          </Box>
        </Paper>
      </Box>
    );
  };

  return (
    <Box sx={{ display: 'flex', height: 'calc(100vh - 80px)', gap: 2 }}>
      {/* Sidebar */}
      <Paper sx={{ width: 260, display: { xs: 'none', md: 'flex' }, flexDirection: 'column', overflow: 'hidden' }}>
        <Box sx={{ p: 1.5 }}>
          <Button fullWidth variant="contained" startIcon={<Add />} onClick={resetDraftConversation}>
            新建对话
          </Button>
        </Box>
        <Divider />
        <List sx={{ flex: 1, overflow: 'auto' }}>
          {conversations.map((c: Conversation) => (
            <ListItemButton
              key={c.id}
              selected={c.id === activeConversationId}
              onClick={() => setActiveConversation(c.id)}
            >
              <ListItemText primary={c.title} primaryTypographyProps={{ noWrap: true, fontSize: 14 }} />
              <IconButton size="small" onClick={(e) => { e.stopPropagation(); deleteMutation.mutate(c.id); }}>
                <Delete fontSize="small" />
              </IconButton>
            </ListItemButton>
          ))}
        </List>
      </Paper>

      {/* Chat area */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {/* Config bar */}
        <Paper sx={{ p: 1.5, mb: 1, display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>AI 供应商</InputLabel>
            <Select value={selectedProvider} onChange={(e) => setSelectedProvider(Number(e.target.value))} label="AI 供应商">
              {enabledProviders.map((p: Provider) => (
                <MenuItem key={p.id} value={p.id}>{p.name} ({p.providerType})</MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>知识库 (RAG)</InputLabel>
            <Select value={selectedKb} onChange={(e) => setSelectedKb(Number(e.target.value))} label="知识库 (RAG)">
              <MenuItem value={0}>不使用</MenuItem>
              {kbs.map((kb: KnowledgeBase) => (
                <MenuItem key={kb.id} value={kb.id}>{kb.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 220 }}>
            <InputLabel>MCP 服务器</InputLabel>
            <Select
              multiple
              value={selectedMcpServers}
              onChange={(e) => setSelectedMcpServers(e.target.value as number[])}
              label="MCP 服务器"
              renderValue={(selected) => {
                const ids = selected as number[];
                if (ids.length === 0) {
                  return '不使用';
                }
                return activeMcpSources
                  .filter((source) => ids.includes(source.id))
                  .map((source) => source.name)
                  .join(', ');
              }}
            >
              {activeMcpSources.map((source: McpApiSource) => (
                <MenuItem key={source.id} value={source.id}>{source.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
          {selectedKb > 0 && <Chip label="RAG 已启用" color="secondary" size="small" />}
          {selectedMcpServers.length > 0 && <Chip label={`MCP ${selectedMcpServers.length} 个`} color="primary" size="small" />}
        </Paper>

        {/* Messages */}
        <Paper sx={{ flex: 1, overflow: 'auto', p: 2 }}>
          {messages.length === 0 && !streamContent && (
            <Box sx={{ textAlign: 'center', mt: 8 }}>
              <SmartToy sx={{ fontSize: 64, color: 'text.disabled' }} />
              <Typography color="text.secondary" mt={2}>开始一段新对话</Typography>
            </Box>
          )}
          <Stack spacing={2}>
            {messages.map((message) => renderMessage(message))}
            {streamContent && (
              renderMessage({
                id: 'stream-assistant',
                role: 'ASSISTANT',
                content: streamContent,
              }, true)
            )}
          </Stack>
          <div ref={messagesEndRef} />
        </Paper>

        {/* Input */}
        <Paper sx={{ p: 1.5, mt: 1, display: 'flex', gap: 1, alignItems: 'flex-end' }}>
          <TextField
            fullWidth
            multiline
            maxRows={4}
            placeholder="输入消息..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); } }}
            disabled={streaming}
            size="small"
          />
          <IconButton color="primary" onClick={handleSend} disabled={streaming || !input.trim()}>
            {streaming ? <CircularProgress size={24} /> : <Send />}
          </IconButton>
        </Paper>
      </Box>
    </Box>
  );
}
