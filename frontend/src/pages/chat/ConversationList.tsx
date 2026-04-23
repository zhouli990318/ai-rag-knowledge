import { Box, Typography, IconButton, Skeleton } from '@mui/material';
import { Add, Close } from '@mui/icons-material';
import { Conversation } from '../../api/types';
import { memo, useCallback } from 'react';

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
        sx={{
          width: 28, height: 28, flexShrink: 0,
          bgcolor: 'rgba(74,74,74,0.08)', color: '#4A4A4A',
          borderRadius: '4px',
          transition: 'all 180ms ease-in-out',
          '&:hover': { bgcolor: '#C84B31', color: '#FFFFFF' },
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
          conversations.map((c) => {
            const isActive = c.id === activeId;
            return (
              <Box
                key={c.id}
                onClick={() => onSelect(c.id)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 0.5,
                  px: 1.3, py: 0.55,
                  borderRadius: '4px', cursor: 'pointer',
                  flexShrink: 0, maxWidth: 160,
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
                  color: isActive ? '#C84B31' : '#8B8B8B',
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                  fontFamily: '"Noto Sans SC", sans-serif',
                }}>
                  {c.title || '新对话'}
                </Typography>
                <IconButton
                  size="small"
                  onClick={(e) => handleDelete(e, c.id)}
                  sx={{
                    p: 0, width: 15, height: 15, opacity: 0,
                    color: '#B0ADA6', transition: 'opacity 100ms',
                    '.MuiBox-root:hover > &': { opacity: 1 },
                    '&:hover': { color: '#C84B31' },
                  }}
                >
                  <Close sx={{ fontSize: 11 }} />
                </IconButton>
              </Box>
            );
          })
        )}
      </Box>
    </Box>
  );
});
