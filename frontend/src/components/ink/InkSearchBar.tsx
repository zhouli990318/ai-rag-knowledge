import { useState } from 'react';
import { Box, IconButton, InputBase, SxProps } from '@mui/material';
import { Search as SearchIcon } from '@mui/icons-material';
import { ink, radius, sansFont, useInk } from '../../theme/ThemeProvider';

interface InkSearchBarProps {
  value: string;
  onChange: (val: string) => void;
  onSearch?: () => void;
  placeholder?: string;
  sx?: SxProps;
}

export default function InkSearchBar({ value, onChange, onSearch, placeholder = '搜索...', sx }: InkSearchBarProps) {
  const di = useInk();
  const [focused, setFocused] = useState(false);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') onSearch?.();
  };

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 0.75,
        px: 1.5,
        py: 0.5,
        backgroundColor: 'rgba(255,255,255,0.65)',
        borderRadius: radius.sm,
        border: `1px solid ${focused ? `${di.gray}40` : 'rgba(224,221,216,0.6)'}`,
        transition: 'all 200ms ease-in-out',
        ...(focused && {
          boxShadow: '0 0 0 2px rgba(74,74,74,0.06)',
          backgroundColor: '#FFFFFF',
        }),
        ...sx,
      }}
    >
      <SearchIcon sx={{ fontSize: 19, color: focused ? di.gray : di.muted, flexShrink: 0 }} />
      <InputBase
        fullWidth
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        sx={{
          fontSize: 14,
          fontFamily: sansFont,
          '& input': { py: 0.5 },
          '&::placeholder': { color: di.muted, opacity: 1 },
        }}
      />
      {value && (
        <IconButton size="small" onClick={() => onChange('')} sx={{ p: 0.3 }}>
          <Box component="span" sx={{ fontSize: 16, color: di.muted, lineHeight: 1 }}>×</Box>
        </IconButton>
      )}
    </Box>
  );
}
