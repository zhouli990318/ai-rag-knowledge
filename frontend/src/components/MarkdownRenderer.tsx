import { useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Box, useTheme } from '@mui/material';

interface Props {
  content: string;
}

export default function MarkdownRenderer({ content }: Props) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const components = useMemo(
    () => ({
      code({ className, children, ...props }: any) {
        const match = /language-(\w+)/.exec(className || '');
        const code = String(children).replace(/\n$/, '');
        return match ? (
          <SyntaxHighlighter
            style={oneDark}
            language={match[1]}
            PreTag="div"
            customStyle={{ borderRadius: 12, fontSize: 13, margin: '8px 0' }}
          >
            {code}
          </SyntaxHighlighter>
        ) : (
          <code
            style={{
              background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.06)',
              borderRadius: 6,
              padding: '2px 6px',
              fontSize: '0.875em',
            }}
            {...props}
          >
            {children}
          </code>
        );
      },
    }),
    [isDark],
  );

  return (
    <Box sx={{
      fontSize: 16, lineHeight: 1.5,
      '& p': { m: 0, mb: 0.75 },
      '& p:last-child': { mb: 0 },
      '& pre': { m: 0 },
      '& ul,ol': { pl: 2.5, my: 0.5 },
      '& li': { mb: 0.25 },
      '& a': { color: '#007AFF', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } },
      '& blockquote': {
        borderLeft: '3px solid',
        borderColor: isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.15)',
        pl: 1.5, ml: 0, my: 0.75,
        color: 'text.secondary',
      },
      '& table': { borderCollapse: 'collapse', my: 1, width: '100%' },
      '& th, & td': {
        border: `0.5px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'}`,
        px: 1, py: 0.5, fontSize: 14,
      },
      '& th': { fontWeight: 600 },
    }}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {content}
      </ReactMarkdown>
    </Box>
  );
}
