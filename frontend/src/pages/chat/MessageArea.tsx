import { useRef, useEffect, useState, useCallback } from 'react';
import { Box, Fab, useTheme } from '@mui/material';
import { KeyboardArrowDown, SmartToy } from '@mui/icons-material';
import { ChatMessage } from '../../api/types';
import MessageBubble from './MessageBubble';
import { IOSEmptyState } from '../../components/ios';
import { motion, AnimatePresence } from 'framer-motion';

type DisplayMessage = Pick<ChatMessage, 'role' | 'content'> & {
  id: number | string;
  createdAt?: string;
};

interface Props {
  messages: DisplayMessage[];
  streamContent: string;
  streaming: boolean;
  onNewChat?: () => void;
}

export default function MessageArea({ messages, streamContent, streaming, onNewChat }: Props) {
  const theme = useTheme();
  const scrollRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const [showScrollDown, setShowScrollDown] = useState(false);
  const userScrolled = useRef(false);

  const scrollToBottom = useCallback((smooth = true) => {
    bottomRef.current?.scrollIntoView({ behavior: smooth ? 'smooth' : 'instant' });
    userScrolled.current = false;
  }, []);

  // Auto-scroll when new messages arrive (unless user scrolled up)
  useEffect(() => {
    if (!userScrolled.current) {
      scrollToBottom(true);
    }
  }, [messages.length, streamContent, scrollToBottom]);

  // Detect user scrolling up
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
        sx={{ height: '100%', overflow: 'auto', px: 2, py: 2 }}
      >
        {isEmpty ? (
          <IOSEmptyState
            icon={<SmartToy />}
            title="开始新对话"
            subtitle="选择一个 AI 供应商，输入消息开始对话"
            action={onNewChat ? { label: '新建对话', onClick: onNewChat } : undefined}
          />
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {messages.map((msg, i) => {
              // Show timestamp if >5min gap from previous
              let showTimestamp: string | undefined;
              if (i === 0 && msg.createdAt) {
                showTimestamp = new Date(msg.createdAt).toLocaleString('zh-CN', {
                  month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit',
                });
              } else if (msg.createdAt && messages[i - 1]?.createdAt) {
                const diff = new Date(msg.createdAt).getTime() - new Date(messages[i - 1].createdAt!).getTime();
                if (diff > 5 * 60 * 1000) {
                  showTimestamp = new Date(msg.createdAt).toLocaleString('zh-CN', {
                    month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit',
                  });
                }
              }
              return (
                <MessageBubble
                  key={msg.id}
                  role={msg.role}
                  content={msg.content}
                  showTimestamp={showTimestamp}
                />
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
                backgroundColor: theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.06)',
                backdropFilter: 'blur(10px)',
                boxShadow: 'none',
                '&:hover': { backgroundColor: theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.1)' },
              }}
            >
              <KeyboardArrowDown sx={{ fontSize: 20, color: 'text.secondary' }} />
            </Fab>
          </motion.div>
        )}
      </AnimatePresence>
    </Box>
  );
}
