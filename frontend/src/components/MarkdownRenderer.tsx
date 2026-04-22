import { memo, useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { PrismLight as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import typescript from 'react-syntax-highlighter/dist/esm/languages/prism/typescript';
import javascript from 'react-syntax-highlighter/dist/esm/languages/prism/javascript';
import python from 'react-syntax-highlighter/dist/esm/languages/prism/python';
import java from 'react-syntax-highlighter/dist/esm/languages/prism/java';
import json from 'react-syntax-highlighter/dist/esm/languages/prism/json';
import bash from 'react-syntax-highlighter/dist/esm/languages/prism/bash';
import css from 'react-syntax-highlighter/dist/esm/languages/prism/css';
import sql from 'react-syntax-highlighter/dist/esm/languages/prism/sql';
import yaml from 'react-syntax-highlighter/dist/esm/languages/prism/yaml';
import markdown from 'react-syntax-highlighter/dist/esm/languages/prism/markdown';
import { Box, useTheme } from '@mui/material';

SyntaxHighlighter.registerLanguage('typescript', typescript);
SyntaxHighlighter.registerLanguage('tsx', typescript);
SyntaxHighlighter.registerLanguage('ts', typescript);
SyntaxHighlighter.registerLanguage('javascript', javascript);
SyntaxHighlighter.registerLanguage('jsx', javascript);
SyntaxHighlighter.registerLanguage('js', javascript);
SyntaxHighlighter.registerLanguage('python', python);
SyntaxHighlighter.registerLanguage('java', java);
SyntaxHighlighter.registerLanguage('json', json);
SyntaxHighlighter.registerLanguage('bash', bash);
SyntaxHighlighter.registerLanguage('shell', bash);
SyntaxHighlighter.registerLanguage('sh', bash);
SyntaxHighlighter.registerLanguage('css', css);
SyntaxHighlighter.registerLanguage('sql', sql);
SyntaxHighlighter.registerLanguage('yaml', yaml);
SyntaxHighlighter.registerLanguage('yml', yaml);
SyntaxHighlighter.registerLanguage('markdown', markdown);
SyntaxHighlighter.registerLanguage('md', markdown);

const REMARK_PLUGINS = [remarkGfm];

interface Props {
  content: string;
}

export default memo(function MarkdownRenderer({ content }: Props) {
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
            customStyle={{ borderRadius: 8, fontSize: 13, margin: '8px 0' }}
          >
            {code}
          </SyntaxHighlighter>
        ) : (
          <code
            style={{
              background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.06)',
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
      <ReactMarkdown remarkPlugins={REMARK_PLUGINS} components={components}>
        {content}
      </ReactMarkdown>
    </Box>
  );
});
