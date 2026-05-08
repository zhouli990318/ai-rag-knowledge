import { useRef, useEffect, useState, useCallback, memo } from 'react';
import { Box, Typography, Fab } from '@mui/material';
import { KeyboardArrowDownOutlined as KeyboardArrowDown, SendOutlined as SendIcon } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import type { ChatMessage } from '@/entities/chat';
import { chatApi } from '@/entities/chat';
import MessageBubble from './MessageBubble';
import { InkBackground } from '@/shared/ui/ink';
import { ink, useInk } from '@/shared/theme/ThemeProvider';
import { useThemeStore } from '@/shared/stores/themeStore';
import { motion, AnimatePresence } from 'framer-motion';

type DisplayMessage = Pick<ChatMessage, 'role' | 'content'> & {
  id: number | string;
  createdAt?: string;
};

interface Props {
  messages: DisplayMessage[];
  streamContent: string;
  streaming: boolean;
  conversationId?: number | null;
  onNewChat?: () => void;
  onSuggestedClick?: (q: string) => void;
}

/** Memoized message item to avoid re-rendering the entire list when a new message arrives */
const MessageItem = memo(function MessageItem({ msg, showTimestamp }: { msg: DisplayMessage; showTimestamp?: string }) {
  return (
    <motion.div
      initial={{ opacity: 0, x: msg.role === 'USER' ? 12 : -12, scale: 0.97 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      transition={{ duration: 0.35, ease: [0.25, 0.46, 0.45, 0.94] }}
    >
      <MessageBubble
        role={msg.role}
        content={msg.content}
        showTimestamp={showTimestamp}
      />
    </motion.div>
  );
});

/* 默认推荐问题（首次对话 / 动态生成失败时的兑底）—— 需与后端 defaultSuggestions() 保持一致 */
const fallbackQuestions = [
  'RAG 与 Fine-tuning 的区别？',
  '如何构建一个 RAG 应用？',
  'RAG 常见问题有哪些？',
];

export default function MessageArea({ messages, streamContent, streaming, conversationId, onNewChat, onSuggestedClick }: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const [showScrollDown, setShowScrollDown] = useState(false);
  const userScrolled = useRef(false);
  const di = useInk();
  const mode = useThemeStore((s) => s.mode);

  const scrollToBottom = useCallback((smooth = true) => {
    bottomRef.current?.scrollIntoView({ behavior: smooth ? 'smooth' : 'instant' });
    userScrolled.current = false;
  }, []);

  useEffect(() => {
    if (!userScrolled.current) scrollToBottom(true);
  }, [messages.length, streamContent, scrollToBottom]);

  // Determine whether suggestions should be fetched
  const lastMsg = messages[messages.length - 1];
  const lastMsgIsAssistant = String(lastMsg?.role || '').toUpperCase() === 'ASSISTANT';
  const suggestionsEnabled = !streaming && !!conversationId && lastMsgIsAssistant;

  const {
    data: suggestionsRaw = [],
    isLoading: suggestionsLoading,
    isFetching: suggestionsFetching,
    isError: suggestionsFailed,
  } = useQuery({
    queryKey: ['suggestions', conversationId, messages.length],
    queryFn: () => chatApi.getSuggestions(conversationId!),
    enabled: suggestionsEnabled,
    staleTime: 5 * 60 * 1000,
    select: (list) => {
      const normalized = Array.isArray(list) ? list.filter((q) => !!q && q.trim().length > 0) : [];
      return normalized;
    },
    refetchInterval: (query) => {
      const data = query.state.data as string[] | undefined;
      if (!data || data.length === 0) return 2000;
      const isDefaults = data.length === fallbackQuestions.length &&
        data.every((q, i) => q === fallbackQuestions[i]);
      return isDefaults ? 2000 : false;
    },
  });

  // Treat backend defaults as "still loading" — show skeleton instead of default questions
  const isBackendDefaults = suggestionsRaw.length === fallbackQuestions.length &&
    suggestionsRaw.every((q, i) => q === fallbackQuestions[i]);
  const suggestions = isBackendDefaults ? [] : suggestionsRaw;
  const stillComputingSuggestions = suggestionsEnabled && isBackendDefaults;

  // Detect whether API messages already contain the stream content (dedup for Bug 1)
  const streamAlreadyInMessages = (() => {
    if (!streamContent) return false;
    const last = messages[messages.length - 1];
    return last && String(last.role).toUpperCase() === 'ASSISTANT' && last.content === streamContent;
  })();

  const handleScroll = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    const distFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    userScrolled.current = distFromBottom > 100;
    setShowScrollDown(distFromBottom > 200);
  }, []);

  const isEmpty = messages.length === 0 && !streamContent;

  return (
    <Box sx={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
      <Box
        ref={scrollRef}
        onScroll={handleScroll}
        sx={{ height: '100%', overflow: 'auto', px: { xs: 1.5, md: 1.5 }, py: 2, pb: 12 }}
      >
        {isEmpty ? (
          /* ═══════ 水墨欢迎页 (匹配设计图) ═══════ */
          <Box sx={{
            display: 'flex', flexDirection: 'column', alignItems: 'flex-start', justifyContent: 'center',
            minHeight: '65vh', position: 'relative', pl: 2,
          }}>
            {/* 水墨山水背景 */}
            <InkBackground sx={{ opacity: 0.05 }} />

            {/* 主标题区域 */}
            <Box sx={{ position: 'relative', zIndex: 1 }}>
              {/* 您好，墨客 */}
              <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1.5, mb: 0.8 }}>
                <Typography
                  sx={{
                    fontFamily: '"Noto Serif SC", serif',
                    fontSize: { xs: 36, md: 52 },
                    fontWeight: 900,
                    color: di.black,
                    letterSpacing: 4,
                    lineHeight: 1.3,
                    textShadow: mode === 'dark' ? '0 2px 12px rgba(0,0,0,0.3)' : '0 2px 8px rgba(0,0,0,0.06)',
                  }}
                >
                  您好，墨客
                </Typography>
                {/* 红色印章 */}
                <Box
                  component={motion.span}
                  initial={{ scale: 1.4, rotate: -8, opacity: 0 }}
                  animate={{ scale: 1, rotate: -5, opacity: 1 }}
                  transition={{ duration: 0.6, delay: 0.4, type: 'spring', damping: 12 }}
                  sx={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontFamily: '"Noto Serif SC", serif',
                    fontWeight: 900,
                    fontSize: 18,
                    color: di.cinnabar,
                    border: `2px solid ${di.cinnabar}`,
                    borderRadius: 3,
                    px: 0.7, py: 0.1,
                    letterSpacing: '0.25em',
                    lineHeight: 1.6,
                    verticalAlign: 'middle',
                    cursor: 'default',
                    userSelect: 'none',
                  }}
                >
                  墨语
                </Box>
              </Box>

              {/* 副标题描述文字 */}
              <Typography
                component={motion.p}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.6 }}
                sx={{
                  fontSize: 15.5,
                  color: di.lightGray,
                  lineHeight: 1.8,
                  maxWidth: 540,
                  mt: 1,
                }}
              >
                我可以帮您检索知识、解答问题、生成内容，亦可调用工具为您完成任务。
              </Typography>
            </Box>

            {/* 示例提问输入框样式占位 */}
            <Box
              component={motion.div}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.9 }}
              sx={{
                position: 'relative',
                zIndex: 1,
                mt: 5,
                ml: 1,
                width: '100%',
                maxWidth: 520,
              }}
            >
              {/* 模拟的示例问题气泡 */}
              <Box sx={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 1.5,
                px: 2.5, py: 1.75,
                backgroundColor: di.cardBg,
                borderRadius: '8px 4px 8px 4px',
                border: `1px solid ${di.border}`,
                color: di.gray,
                fontSize: 14,
                boxShadow: '0 4px 16px rgba(0,0,0,0.04)',
                backdropFilter: 'blur(8px)',
              }}>
                <Typography sx={{ fontSize: 14 }}>什么是RAG？它的工作原理是什么？</Typography>

                {/* 右侧人物头像小图标 */}
                <Box sx={{
                  width: '2.25rem', height: '2.25rem', borderRadius: '50%',
                  bgcolor: di.cardBg,
                  border: `1px solid ${di.border}`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0, overflow: 'hidden',
                }}>
                  <Box component="svg" viewBox="0 0 36 36" sx={{ width: '80%', height: '80%' }}>
                    <path d="M10 14 Q18 6 26 14" fill="none" stroke="#4A4540" strokeWidth={1.2} strokeLinecap="round" />
                    <circle cx="18" cy="16.5" r="2.8" fill="none" stroke="#4A4540" strokeWidth={1} />
                    <path d="M14 19 Q13 24 10 30 L18 28 L26 30 Q23 24 22 19" fill="none" stroke="#4A4540" strokeWidth={1.1} strokeLinejoin="round" />
                    <path d="M14 22 Q9 21 7 24" fill="none" stroke="#4A4540" strokeWidth={0.9} strokeLinecap="round" />
                    <path d="M22 22 Q27 21 29 24" fill="none" stroke="#4A4540" strokeWidth={0.9} strokeLinecap="round" />
                    <path d="M10 30 Q14 32 18 31 Q22 32 26 30" fill="none" stroke="#4A4540" strokeWidth={0.8} />
                  </Box>
                </Box>
              </Box>
              {/* 时间戳 */}
              <Typography sx={{ fontSize: 11, color: di.muted, mt: 1, ml: 1 }}>
                10:23
              </Typography>
            </Box>

          </Box>
        ) : (
          /* ═══════ 正常消息列表 ═══════ */
          <Box role="feed" aria-label="聊天消息" sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {messages.map((msg, i) => {
              let showTimestamp: string | undefined;
              if (i === 0 && msg.createdAt) {
                showTimestamp = new Date(msg.createdAt).toLocaleString('zh-CN', {
                  month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit',
                });
              } else if (msg.createdAt && messages[i - 1]?.createdAt) {
                const diff = new Date(msg.createdAt).getTime() - new Date(messages[i - 1].createdAt as string).getTime();
                if (diff > 5 * 60 * 1000) {
                  showTimestamp = new Date(msg.createdAt).toLocaleString('zh-CN', {
                    month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit',
                  });
                }
              }
              return <MessageItem key={msg.id} msg={msg} showTimestamp={showTimestamp} />;
            })}

            {streamContent && !streamAlreadyInMessages && (
              <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.2 }}
              >
                <MessageBubble
                  role="ASSISTANT"
                  content={streamContent}
                  isStreaming={streaming}
                />
              </motion.div>
            )}

            {/* 思考中占位：已发起但首个 token 尚未到达 */}
            {streaming && !streamContent && (
              <motion.div
                initial={{ opacity: 0, x: -12, scale: 0.97 }}
                animate={{ opacity: 1, x: 0, scale: 1 }}
                transition={{ duration: 0.35, ease: [0.25, 0.46, 0.45, 0.94] }}
              >
                <MessageBubble
                  role="ASSISTANT"
                  content=""
                  isThinking={true}
                />
              </motion.div>
            )}

            {/* 推荐问题（在最后一条AI消息后显示） */}
            {!streaming && lastMsgIsAssistant && (
              <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.35, delay: 0.2 }}
                style={{ marginTop: 4 }}
              >
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, pl: 1 }}>
                  {suggestionsLoading || suggestionsFetching || stillComputingSuggestions ? (
                    // Skeleton pulse pills while loading
                    [0, 1, 2].map((i) => (
                      <Box
                        key={`sk-${i}`}
                        sx={{
                          height: 32,
                          width: 120 + i * 30,
                          maxWidth: '80%',
                          borderRadius: '1.25rem',
                          backgroundColor: mode === 'dark' ? 'rgba(60,58,54,0.5)' : 'rgba(224,220,213,0.5)',
                          animation: 'inkPulse 1.2s ease-in-out infinite',
                          '@keyframes inkPulse': {
                            '0%, 100%': { opacity: 0.5 },
                            '50%': { opacity: 0.85 },
                          },
                        }}
                      />
                    ))
                  ) : (
                    (suggestions.length > 0 ? suggestions : (suggestionsFailed ? fallbackQuestions : [])).map((q, qi) => (
                      <Box
                        key={qi}
                        onClick={() => onSuggestedClick?.(q)}
                        sx={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 0.5,
                          px: '1rem', py: '0.5rem',
                          borderRadius: '1.25rem',
                          backgroundColor: di.cardBg,
                          border: `1px solid ${di.border}`,
                          color: di.navText,
                          fontSize: '0.8125rem',
                          cursor: 'pointer',
                          whiteSpace: 'nowrap',
                          transition: 'all 180ms ease-in-out',
                          '&:hover': {
                            borderColor: di.cinnabar,
                            color: di.cinnabar,
                            backgroundColor: mode === 'dark' ? 'rgba(196,92,92,0.08)' : '#F5F0E8',
                            boxShadow: `0 2px 8px rgba(196,92,92,0.08)`,
                            transform: 'translateY(-1px)',
                          },
                        }}
                      >
                        <span>{q}</span>
                        <span style={{ fontSize: '0.75rem', marginLeft: 4 }}>›</span>
                      </Box>
                    ))
                  )}
                </Box>
              </motion.div>
            )}
          </Box>
        )}
        <div ref={bottomRef} />
      </Box>

      {/* Scroll-to-bottom FAB */}
      <AnimatePresence>
        {showScrollDown && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            transition={{ duration: 0.15 }}
            style={{ position: 'absolute', bottom: 12, right: 16 }}
          >
            <Fab
              size="small"
              onClick={() => scrollToBottom(true)}
              sx={{
                width: 36, height: 36,
                backgroundColor: mode === 'dark' ? 'rgba(42,40,38,0.85)' : 'rgba(255,255,255,0.85)',
                backdropFilter: 'blur(8px)',
                boxShadow: mode === 'dark' ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 12px rgba(0,0,0,0.08)',
                border: `1px solid ${di.glassBorder}`,
                '&:hover': { backgroundColor: mode === 'dark' ? 'rgba(50,48,46,0.95)' : '#FFFFFF' },
              }}
            >
              <KeyboardArrowDown sx={{ fontSize: 20, color: di.lightGray }} />
            </Fab>
          </motion.div>
        )}
      </AnimatePresence>
    </Box>
  );
}
