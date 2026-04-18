import { useState, useRef, useEffect } from 'react';
import { Box, InputBase, IconButton, useTheme } from '@mui/material';
import { Search as SearchIcon, Close as CloseIcon } from '@mui/icons-material';
import { motion, AnimatePresence } from 'framer-motion';

interface IOSSearchBarProps {
  value: string;
  onChange: (val: string) => void;
  onSearch?: (val: string) => void;
  placeholder?: string;
}

export default function IOSSearchBar({ value, onChange, onSearch, placeholder = '搜索' }: IOSSearchBarProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const [focused, setFocused] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleCancel = () => {
    onChange('');
    setFocused(false);
    inputRef.current?.blur();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && onSearch) {
      onSearch(value);
    }
    if (e.key === 'Escape') {
      handleCancel();
    }
  };

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <Box sx={{
        flex: 1,
        display: 'flex', alignItems: 'center', gap: 0.75,
        backgroundColor: isDark ? 'rgba(118,118,128,0.24)' : 'rgba(118,118,128,0.12)',
        borderRadius: 2.5,
        px: 1.25, py: 0.75,
        transition: 'all 200ms cubic-bezier(0.25,0.46,0.45,0.94)',
      }}>
        <SearchIcon sx={{ fontSize: 18, color: isDark ? 'rgba(235,235,245,0.4)' : 'rgba(60,60,67,0.4)' }} />
        <InputBase
          inputRef={inputRef}
          fullWidth
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setFocused(true)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          sx={{
            fontSize: 17,
            '& input::placeholder': {
              color: isDark ? 'rgba(235,235,245,0.4)' : 'rgba(60,60,67,0.4)',
              opacity: 1,
            },
          }}
        />
        {value && (
          <IconButton size="small" onClick={() => onChange('')} sx={{ p: 0.25 }}>
            <CloseIcon sx={{ fontSize: 16, color: isDark ? 'rgba(235,235,245,0.4)' : 'rgba(60,60,67,0.4)' }} />
          </IconButton>
        )}
      </Box>
      <AnimatePresence>
        {focused && (
          <motion.div
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: 'auto', opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            style={{ overflow: 'hidden', whiteSpace: 'nowrap' }}
          >
            <Box
              component="span"
              onClick={handleCancel}
              sx={{
                cursor: 'pointer', fontSize: 17, color: '#007AFF',
                '&:active': { opacity: 0.6 },
              }}
            >
              取消
            </Box>
          </motion.div>
        )}
      </AnimatePresence>
    </Box>
  );
}
