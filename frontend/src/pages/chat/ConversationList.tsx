import { Box, Typography, IconButton, Skeleton, useTheme } from '@mui/material';
import { Add, Delete } from '@mui/icons-material';
import { Conversation } from '../../api/types';
import IOSSearchBar from '../../components/ios/IOSSearchBar';
import { memo, useState, useMemo, useCallback } from 'react';

function formatTime(dateStr?: string) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' });
}

interface Props {
  conversations: Conversation[];
  activeId: number | null;
  onSelect: (id: number) => void;
  onDelete: (id: number) => void;
  onNew: () => void;
  isLoading?: boolean;
}

const ConversationItem = memo(function ConversationItem({
  conversation, isActive, isDark, onSelect, onDelete,
}: {
  conversation: Conversation; isActive: boolean; isDark: boolean;
  onSelect: (id: number) => void; onDelete: (id: number) => void;
}) {
  const lastMsg = conversation.messages?.[conversation.messages.length - 1];
  return (
    <Box
      onClick={() => onSelect(conversation.id)}
      sx={{
        display: 'flex', alignItems: 'flex-start',
        px: 1.5, py: 1.5, mb: 0.25,
        borderRadius: 2, cursor: 'pointer', position: 'relative',
        backgroundColor: isActive
          ? (isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,122,255,0.08)')
          : 'transparent',
        transition: 'background-color 200ms',
        '&:active': { backgroundColor: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)' },
        '&:hover .delete-btn': { opacity: 1 },
      }}
    >
      {isActive && (
        <Box sx={{
          position: 'absolute', left: 0, top: '50%', transform: 'translateY(-50%)',
          width: 3, height: 20, borderRadius: 2, backgroundColor: '#007AFF',
        }} />
      )}
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', mb: 0.25 }}>
          <Typography sx={{
            fontSize: 15, fontWeight: isActive ? 600 : 500,
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            flex: 1, mr: 1,
          }}>
            {conversation.title || '新对话'}
          </Typography>
          <Typography sx={{ fontSize: 12, color: 'text.secondary', flexShrink: 0 }}>
            {formatTime(conversation.createdAt)}
          </Typography>
        </Box>
        <Typography sx={{
          fontSize: 13, color: 'text.secondary',
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>
          {lastMsg?.content?.slice(0, 60) || '暂无消息'}
        </Typography>
      </Box>
      <IconButton
        className="delete-btn" size="small"
        onClick={(e) => { e.stopPropagation(); onDelete(conversation.id); }}
        sx={{
          opacity: 0, ml: 0.5, mt: 0.25, transition: 'opacity 150ms',
          color: 'text.secondary', '&:hover': { color: '#FF3B30' },
        }}
      >
        <Delete sx={{ fontSize: 16 }} />
      </IconButton>
    </Box>
  );
});

export default memo(function ConversationList({ conversations, activeId, onSelect, onDelete, onNew, isLoading }: Props) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    if (!search.trim()) return conversations;
    const q = search.toLowerCase();
    return conversations.filter((c) => c.title?.toLowerCase().includes(q));
  }, [conversations, search]);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      <Box sx={{ p: 1.5, display: 'flex', alignItems: 'center', gap: 1 }}>
        <Box sx={{ flex: 1 }}>
          <IOSSearchBar value={search} onChange={setSearch} placeholder="搜索对话" />
        </Box>
        <IconButton
          onClick={onNew}
          sx={{
            width: 32, height: 32,
            backgroundColor: '#007AFF',
            color: '#fff',
            '&:hover': { backgroundColor: '#0071E3' },
            '&:active': { transform: 'scale(0.9)' },
          }}
          size="small"
        >
          <Add sx={{ fontSize: 18 }} />
        </IconButton>
      </Box>

      {/* List */}
      <Box sx={{ flex: 1, overflow: 'auto', px: 1 }}>
        {isLoading ? (
          Array.from({ length: 5 }).map((_, i) => (
            <Box key={i} sx={{ px: 1.5, py: 1.5, display: 'flex', flexDirection: 'column', gap: 0.75 }}>
              <Skeleton variant="text" width="60%" height={18} />
              <Skeleton variant="text" width="90%" height={14} />
            </Box>
          ))
        ) : (
          filtered.map((c) => (
            <ConversationItem
              key={c.id}
              conversation={c}
              isActive={c.id === activeId}
              isDark={isDark}
              onSelect={onSelect}
              onDelete={onDelete}
            />
          ))
        )}
      </Box>
    </Box>
  );
});
