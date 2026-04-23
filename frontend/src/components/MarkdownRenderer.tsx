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
import { Box, IconButton, Typography } from '@mui/material';
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

/* ── Copy button for code blocks ── */
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
        color: 'rgba(255,255,255,0.4)',
        opacity: 0,
        transition: 'opacity 0.15s',
        '.code-block-wrapper:hover &': { opacity: 1 },
        '&:hover': { color: 'rgba(255,255,255,0.85)', backgroundColor: 'rgba(255,255,255,0.1)' },
        borderRadius: 3,
        p: 0.5,
      }}
    >
      {copied ? <Check sx={{ fontSize: 15 }} /> : <ContentCopy sx={{ fontSize: 15 }} />}
    </IconButton>
  );
}

/* ── Streaming cursor character ── */
const CURSOR_CHAR = '\u258B';

interface Props {
  content: string;
  isStreaming?: boolean;
}

export default memo(function MarkdownRenderer({ content, isStreaming }: Props) {
  const displayContent = content;

  const components = useMemo(
    () => ({
      code({ className, children, ...props }: any) {
        const match = /language-(\w+)/.exec(className || '');
        const code = String(children).replace(/\n$/, '');
        return match ? (
          <Box
            className="code-block-wrapper"
            sx={{
              position: 'relative',
              my: '10px',
              borderRadius: 4,
              overflow: 'hidden',
              border: '1px solid rgba(224,221,216,0.35)',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            {/* Code header bar */}
            <Box sx={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              px: 1.5, py: 0.5,
              backgroundColor: 'rgba(44,44,44,0.95)',
              borderBottom: '1px solid rgba(255,255,255,0.08)',
            }}>
              <Typography sx={{
                fontSize: 11, color: 'rgba(255,255,255,0.45)', fontFamily: '"Noto Sans SC", sans-serif',
                letterSpacing: '0.03em',
              }}>
                {match[1]}
              </Typography>
            </Box>
            <SyntaxHighlighter
              style={oneDark}
              language={match[1]}
              PreTag="div"
              customStyle={{
                borderRadius: 0,
                fontSize: 13,
                margin: 0,
                padding: '12px 16px',
                background: '#282c34',
              }}
            >
              {code}
            </SyntaxHighlighter>
            <CopyButton code={code} />
          </Box>
        ) : (
          <code
            style={{
              background: 'rgba(74,74,74,0.06)',
              borderRadius: 3,
              padding: '2px 7px',
              fontSize: '0.88em',
              fontFamily: '"SF Mono", "Fira Code", "Consolas", monospace',
              border: '0.5px solid rgba(224,221,216,0.4)',
            }}
            {...props}
          >
            {children}
          </code>
        );
      },
    }),
    [],
  );

  return (
    <Box sx={{
      fontSize: 14.5, lineHeight: 1.75,
      color: '#2C2C2C',
      /* Typography */
      '& p': { m: 0, mb: 0.85, '&:last-child': { mb: 0 } },
      '& h1,h2,h3,h4,h5,h6': {
        fontFamily: '"Noto Serif SC", serif',
        fontWeight: 700,
        color: '#2C2C2C',
        mt: 1.75, mb: 0.85,
        lineHeight: 1.4,
      },
      '& h1': { fontSize: 24, letterSpacing: '0.08em' },
      '& h2': { fontSize: 20, borderBottom: '2px solid rgba(200,75,49,0.15)', pb: 0.5 },
      '& h3': { fontSize: 17 },
      /* Lists */
      '& ul,ol': { pl: 2.25, my: 0.65 },
      '& li': { mb: 0.3, '& > ul, & > ol': { mt: 0.3 } },
      '& li::marker': { color: '#C84B31' },
      /* Links */
      '& a': { color: '#C84B31', textDecoration: 'none', transition: 'all 150ms', '&:hover': { textDecoration: 'underline' } },
      /* Blockquotes - 水墨竖线风格 */
      '& blockquote': {
        borderLeft: '3px solid #C84B31',
        pl: 1.5, ml: 0, my: 0.85,
        py: 0.5, pr: 1,
        color: '#6A6660',
        backgroundColor: 'rgba(245,243,238,0.5)',
        borderRadius: '0 4px 4px 0',
        fontStyle: 'italic',
      },
      /* Tables */
      '& table': {
        borderCollapse: 'separate', borderSpacing: 0, my: 1.25, width: '100%',
        borderRadius: 4, overflow: 'hidden',
        border: '1px solid rgba(224,221,216,0.55)',
      },
      '& th, & td': {
        border: '0.5px solid rgba(224,221,216,0.4)',
        px: 1.25, py: 0.7, fontSize: 13.5,
      },
      '& th': {
        fontWeight: 600, fontSize: 12.5,
        backgroundColor: 'rgba(245,243,238,0.7)',
        color: '#4A4A4A',
        textTransform: 'uppercase',
        letterSpacing: '0.05em',
      },
      '& tr:nth-of-type(even) td': {
        backgroundColor: 'rgba(250,248,245,0.4)',
      },
      /* Math */
      '& .katex-display': { my: 1, overflowX: 'auto', py: 0.5 },
      '& .katex': { fontSize: '1.05em' },
      /* Strong/Bold */
      '& strong': { fontWeight: 700, color: '#2C2C2C' },
      /* HR */
      '& hr': {
        border: 'none',
        height: 1,
        backgroundImage: 'linear-gradient(90deg, transparent, rgba(224,221,216,0.6), transparent)',
        my: 1.5,
      },
      /* Images */
      '& img': { maxWidth: '100%', borderRadius: 4, border: '1px solid rgba(224,221,216,0.4)' },
    }}>
      <ReactMarkdown
        remarkPlugins={REMARK_PLUGINS}
        rehypePlugins={REHYPE_PLUGINS}
        components={components}
      >
        {displayContent}
      </ReactMarkdown>
      {isStreaming && (
        <Box
          component="span"
          sx={{
            display: 'inline-block',
            width: 2.5,
            height: '1.15em',
            ml: '1px',
            verticalAlign: 'text-bottom',
            backgroundColor: '#4A4A4A',
            borderRadius: '1px',
            animation: 'inkCursorPulse 1.2s ease-in-out infinite',
          }}
        />
      )}
    </Box>
  );
});
