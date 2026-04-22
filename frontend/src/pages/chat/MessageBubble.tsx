import { memo } from 'react';
import { Box, Typography, useTheme } from '@mui/material';
import MarkdownRenderer from '../../components/MarkdownRenderer';
import fixIncompleteMarkdown from '../../utils/fixIncompleteMarkdown';

interface Props {
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  isStreaming?: boolean;
  showTimestamp?: string;
}

export default memo(function MessageBubble({ role, content, isStreaming, showTimestamp }: Props) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const isUser = role === 'USER';

  const displayContent = isStreaming ? fixIncompleteMarkdown(content) : content;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: isUser ? 'flex-end' : 'flex-start' }}>
      {showTimestamp && (
        <Typography sx={{
          fontSize: 12, color: 'text.secondary', textAlign: 'center',
          width: '100%', my: 1.5,
        }}>
          {showTimestamp}
        </Typography>
      )}
      <Box
        sx={{
          maxWidth: { xs: '88%', md: '72%' },
          minWidth: 40,
          px: 2, py: 1.25,
          borderRadius: isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
          backgroundColor: isUser
            ? '#007AFF'
            : (isDark ? '#26252A' : '#E9E9EB'),
          color: isUser ? '#FFFFFF' : (isDark ? '#FFFFFF' : '#000000'),
          wordBreak: 'break-word',
          position: 'relative',
          // override markdown link colors inside user bubble
          ...(isUser && {
            '& a': { color: '#FFFFFF', textDecoration: 'underline' },
            '& code': { backgroundColor: 'rgba(255,255,255,0.2)' },
          }),
        }}
      >
        {isUser ? (
          <Typography sx={{ fontSize: 16, lineHeight: 1.45, whiteSpace: 'pre-wrap' }}>{content}</Typography>
        ) : (
          <MarkdownRenderer content={displayContent} isStreaming={isStreaming} />
        )}
      </Box>
    </Box>
  );
});
