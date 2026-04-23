import { useRef, useEffect, useState, useCallback } from 'react';
import { Box, Typography, Fab } from '@mui/material';
import { KeyboardArrowDown, Send as SendIcon } from '@mui/icons-material';
import { ChatMessage } from '../../api/types';
import { chatApi } from '../../api/chatApi';
import MessageBubble from './MessageBubble';
import { InkBackground } from '../../components/ink';
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

/* 默认推荐问题（首次对话 / 动态生成失败时的兑底） */
const fallbackQuestions = [
  'RAG 与 Fine-tuning 的区别',
  '如何构建一个 RAG 应用？',
  'RAG 常见问题有哪些？',
];

export default function MessageArea({ messages, streamContent, streaming, conversationId, onNewChat, onSuggestedClick }: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const [showScrollDown, setShowScrollDown] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [loadingSuggestions, setLoadingSuggestions] = useState(false);
  const [suggestionsFailed, setSuggestionsFailed] = useState(false);
  const userScrolled = useRef(false);
  const lastFetchedKey = useRef<string | null>(null);

  const scrollToBottom = useCallback((smooth = true) => {
    bottomRef.current?.scrollIntoView({ behavior: smooth ? 'smooth' : 'instant' });
    userScrolled.current = false;
  }, []);

  useEffect(() => {
    if (!userScrolled.current) scrollToBottom(true);
  }, [messages.length, streamContent, scrollToBottom]);

  // 动态获取推荐问题：在流式结束后，最后一条是 ASSISTANT 时拉取
  useEffect(() => {
    const lastMsg = messages[messages.length - 1];
    const hasAssistantAnswer = String(lastMsg?.role || '').toUpperCase() === 'ASSISTANT' || (!!streamContent && !streaming);
    const shouldFetch = !streaming && conversationId && hasAssistantAnswer;
    if (!shouldFetch) {
      if (!conversationId) {
        setSuggestions(fallbackQuestions);
        setSuggestionsFailed(false);
      }
      setLoadingSuggestions(false);
      return;
    }
    const key = `${conversationId}-${messages.length}`;
    if (lastFetchedKey.current === key) return;
    lastFetchedKey.current = key;

    let cancelled = false;
    setLoadingSuggestions(true);
    chatApi.getSuggestions(conversationId)
      .then((list) => {
        if (cancelled) return;
        const normalized = Array.isArray(list) ? list.filter((q) => !!q && q.trim().length > 0) : [];
        setSuggestions(normalized);
        setSuggestionsFailed(false);
        if (normalized.length === 0) {
          // 空结果不锁死 key，允许后续同版本再次触发拉取。
          lastFetchedKey.current = null;
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSuggestions(fallbackQuestions);
          setSuggestionsFailed(true);
        }
      })
      .finally(() => {
        if (!cancelled) setLoadingSuggestions(false);
      });
    return () => { cancelled = true; };
  }, [streaming, streamContent, conversationId, messages.length]);

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
        sx={{ height: '100%', overflow: 'auto', px: { xs: 1.5, md: 1.5 }, py: 2 }}
      >
        {isEmpty ? (
          /* ═══════ 水墨欢迎页 (匹配设计图) ═══════ */
          <Box sx={{
            display: 'flex', flexDirection: 'column', alignItems: 'flex-start', justifyContent: 'center',
            minHeight: '65vh', position: 'relative', pl: 2,
          }}>
            {/* 水墨山水背景 */}
            <InkBackground sx={{ opacity: 0.08 }} />

            {/* 主标题区域 */}
            <Box sx={{ position: 'relative', zIndex: 1 }}>
              {/* 您好，墨客 */}
              <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1.5, mb: 0.8 }}>
                <Typography
                  sx={{
                    fontFamily: '"Noto Serif SC", serif',
                    fontSize: { xs: 32, md: 40 },
                    fontWeight: 900,
                    color: '#2C2C2C',
                    letterSpacing: 4,
                    lineHeight: 1.3,
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
                    fontSize: 15,
                    color: '#C84B31',
                    border: `2px solid #C84B31`,
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
                  fontSize: 14.5,
                  color: '#8B8B8B',
                  lineHeight: 1.8,
                  maxWidth: 480,
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
                backgroundColor: 'rgba(255,255,255,0.72)',
                backdropFilter: 'blur(10px)',
                WebkitBackdropFilter: 'blur(10px)',
                borderRadius: '8px 4px 8px 4px',
                border: '1px solid rgba(224,221,216,0.6)',
                color: '#4A4A4A',
                fontSize: 14,
                boxShadow: '0 4px 16px rgba(0,0,0,0.04)',
              }}>
                <Typography sx={{ fontSize: 14 }}>什么是RAG？它的工作原理是什么？</Typography>

                {/* 右侧人物头像小图标 */}
                <Box sx={{
                  width: 32, height: 32, borderRadius: '50%',
                  bgcolor: '#2C2C2C',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 13, color: '#FFF', fontFamily: '"Noto Serif SC", serif',
                  fontWeight: 600, flexShrink: 0,
                }}>
                  墨
                </Box>
              </Box>
              {/* 时间戳 */}
              <Typography sx={{ fontSize: 11, color: '#B0ADA6', mt: 1, ml: 1 }}>
                10:23
              </Typography>
            </Box>

          </Box>
        ) : (
          /* ═══════ 正常消息列表 ═══════ */
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
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
              return (
                <motion.div
                  key={msg.id}
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
            })}

            {streamContent && (
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
            {!streaming && (String(messages[messages.length - 1]?.role || '').toUpperCase() === 'ASSISTANT' || !!streamContent) && (
              <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.35, delay: 0.2 }}
                style={{ marginTop: 4 }}
              >
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, pl: 1 }}>
                  {loadingSuggestions && suggestions.length === 0 ? (
                    // Skeleton pulse pills
                    [0, 1, 2].map((i) => (
                      <Box
                        key={`sk-${i}`}
                        sx={{
                          height: 28,
                          width: 120 + i * 30,
                          borderRadius: '4px',
                          backgroundColor: 'rgba(224,221,216,0.5)',
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
                          gap: 0.4,
                          px: 2, py: 0.65,
                          borderRadius: '4px',
                          backgroundColor: 'rgba(255,255,255,0.72)',
                          backdropFilter: 'blur(8px)',
                          WebkitBackdropFilter: 'blur(8px)',
                          border: '1px solid rgba(224,221,216,0.55)',
                          color: '#4A4A4A',
                          fontSize: 13,
                          cursor: 'pointer',
                          whiteSpace: 'nowrap',
                          transition: 'all 180ms ease-in-out',
                          '&:hover': {
                            borderColor: '#C84B31',
                            color: '#C84B31',
                            backgroundColor: 'rgba(255,255,255,0.92)',
                            boxShadow: '0 2px 8px rgba(200,75,49,0.08)',
                            transform: 'translateY(-1px)',
                          },
                        }}
                      >
                        <span>{q}</span>
                        <span style={{ fontSize: 14, marginLeft: 2 }}>›</span>
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
                backgroundColor: 'rgba(255,255,255,0.85)',
                backdropFilter: 'blur(8px)',
                boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
                border: '1px solid rgba(224,221,216,0.5)',
                '&:hover': { backgroundColor: '#FFFFFF' },
              }}
            >
              <KeyboardArrowDown sx={{ fontSize: 20, color: '#8B8B8B' }} />
            </Fab>
          </motion.div>
        )}
      </AnimatePresence>
    </Box>
  );
}
