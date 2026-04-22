import { memo, useMemo, useState, useCallback } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import 'katex/dist/katex.min.css';
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
import { Box, IconButton, useTheme } from '@mui/material';
import { ContentCopy, Check } from '@mui/icons-material';

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

const REMARK_PLUGINS = [remarkGfm, remarkMath];
const REHYPE_PLUGINS = [rehypeKatex];

/* ---- Copy button for code blocks ---- */
function CopyButton({ code }: { code: string }) {
  const [copied, setCopied] = useState(false);
  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(code).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  }, [code]);

  return (
    <IconButton
      size="small"
      onClick={handleCopy}
      sx={{
        position: 'absolute', top: 6, right: 6,
        color: 'rgba(255,255,255,0.5)',
        opacity: 0,
        transition: 'opacity 0.15s',
        '.code-block-wrapper:hover &': { opacity: 1 },
        '&:hover': { color: 'rgba(255,255,255,0.85)' },
      }}
    >
      {copied ? <Check sx={{ fontSize: 16 }} /> : <ContentCopy sx={{ fontSize: 16 }} />}
    </IconButton>
  );
}

/* ---- Streaming cursor character ---- */
const CURSOR_CHAR = '▋';

interface Props {
  content: string;
  isStreaming?: boolean;
}

export default memo(function MarkdownRenderer({ content, isStreaming }: Props) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const displayContent = isStreaming ? content + CURSOR_CHAR : content;

  const components = useMemo(
    () => ({
      code({ className, children, ...props }: any) {
        const match = /language-(\w+)/.exec(className || '');
        const code = String(children).replace(/\n$/, '');
        return match ? (
          <Box
            className="code-block-wrapper"
            sx={{ position: 'relative', my: '8px' }}
          >
            <SyntaxHighlighter
              style={oneDark}
              language={match[1]}
              PreTag="div"
              customStyle={{ borderRadius: 8, fontSize: 13, margin: 0 }}
            >
              {code}
            </SyntaxHighlighter>
            <CopyButton code={code} />
          </Box>
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
      '& .katex-display': { my: 1, overflowX: 'auto' },
      '@keyframes cursor-blink': { '50%': { opacity: 0 } },
    }}>
      <ReactMarkdown
        remarkPlugins={REMARK_PLUGINS}
        rehypePlugins={REHYPE_PLUGINS}
        components={components}
      >
        {displayContent}
      </ReactMarkdown>
    </Box>
  );
});
