import { Box, Typography, IconButton, Skeleton } from '@mui/material';
import { AddOutlined as Add, CloseOutlined as Close } from '@mui/icons-material';
import { motion, AnimatePresence } from 'framer-motion';
import type { Conversation } from '@/entities/chat';
import { memo, useCallback } from 'react';
import { ui } from '@/shared/theme/semanticColors';

interface Props {
  conversations: Conversation[];
  activeId: number | null;
  onSelect: (id: number) => void;
  onDelete: (id: number) => void;
  onNew: () => void;
  isLoading?: boolean;
}

export default memo(function ConversationList({ conversations, activeId, onSelect, onDelete, onNew, isLoading }: Props) {
  const handleDelete = useCallback((e: React.MouseEvent, id: number) => {
    e.stopPropagation();
    onDelete(id);
  }, [onDelete]);

  return (
    <Box sx={{
      display: 'flex', alignItems: 'center',
      px: 1.5, minHeight: 48,
      gap: 0.5,
    }}>
      {/* New conversation button - 毛玻璃风格 */}
      <IconButton
        onClick={onNew}
        size="small"
        aria-label="新建对话"
        sx={{
          width: 28, height: 28, flexShrink: 0,
          bgcolor: 'rgba(74,74,74,0.08)', color: ui.textSecondary,
          borderRadius: '4px',
          transition: 'all 180ms ease-in-out',
          '&:hover': { bgcolor: ui.accentRed, color: '#FFFFFF' },
          '&:active': { transform: 'scale(0.9)' },
        }}
      >
        <Add sx={{ fontSize: 16 }} />
      </IconButton>

      {/* Horizontal scrollable tabs */}
      <Box sx={{
        display: 'flex', alignItems: 'center', gap: 0.35,
        flex: 1, overflow: 'auto', minWidth: 0,
        /* hide scrollbar */
        '&::-webkit-scrollbar': { display: 'none' },
        scrollbarWidth: 'none',
      }}>
        {isLoading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} variant="rounded" width={100} height={30} sx={{ flexShrink: 0, borderRadius: '4px' }} />
          ))
        ) : (
          <AnimatePresence mode="popLayout">
            {conversations.map((c) => {
              const isActive = c.id === activeId;
              return (
                <motion.div
                  key={c.id}
                  layout
                  initial={{ opacity: 0, x: -12, scale: 0.95 }}
                  animate={{ opacity: 1, x: 0, scale: 1 }}
                  exit={{ opacity: 0, x: -20, scale: 0.9 }}
                  transition={{ duration: 0.2, ease: [0.25, 0.46, 0.45, 0.94] }}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.97 }}
                  style={{ flexShrink: 0 }}
                >
              <Box
                onClick={() => onSelect(c.id)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 0.5,
                  px: 1.3, py: 0.55,
                  borderRadius: '4px', cursor: 'pointer',
                  maxWidth: 160,
                  bgcolor: isActive ? 'rgba(200,75,49,0.07)' : 'transparent',
                  border: `1px solid ${isActive ? 'rgba(200,75,49,0.18)' : 'transparent'}`,
                  transition: 'all 180ms ease-in-out',
                  '&:hover': {
                    bgcolor: isActive ? 'rgba(200,75,49,0.09)' : 'rgba(74,74,74,0.04)',
                    borderColor: isActive ? 'rgba(200,75,49,0.22)' : 'rgba(224,221,216,0.3)',
                  },
                }}
              >
                <Typography sx={{
                  fontSize: 13, fontWeight: isActive ? 600 : 400,
                  color: isActive ? ui.accentRed : ui.textMuted,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                  fontFamily: '"Noto Sans SC", sans-serif',
                }}>
                  {c.title || '新对话'}
                </Typography>
                <IconButton
                  size="small"
                  aria-label={`删除对话: ${c.title || '新对话'}`}
                  onClick={(e) => handleDelete(e, c.id)}
                  sx={{
                    p: 0, width: 15, height: 15, opacity: 0,
                    color: '#B0ADA6', transition: 'opacity 100ms',
                    '.MuiBox-root:hover > &': { opacity: 1 },
                    '&:hover': { color: ui.accentRed },
                  }}
                >
                  <Close sx={{ fontSize: 11 }} />
                </IconButton>
              </Box>
                </motion.div>
              );
            })}
          </AnimatePresence>
        )}
      </Box>
    </Box>
  );
});
