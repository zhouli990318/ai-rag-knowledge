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

  const components = useMemo(
    () => ({
      code({ className, children, ...props }: any) {
        const match = /language-(\w+)/.exec(className || '');
        const code = String(children).replace(/\n$/, '');
        return match ? (
          <SyntaxHighlighter style={oneDark} language={match[1]} PreTag="div">
            {code}
          </SyntaxHighlighter>
        ) : (
          <code
            style={{
              background: theme.palette.action.hover,
              borderRadius: 4,
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
    [theme],
  );

  return (
    <Box sx={{ '& p': { m: 0, mb: 1 }, '& pre': { m: 0 }, '& ul,ol': { pl: 2.5 } }}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {content}
      </ReactMarkdown>
    </Box>
  );
}
