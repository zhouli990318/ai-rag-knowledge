import { Box, useTheme } from '@mui/material';

interface IOSSegmentedControlProps {
  value: string | number;
  onChange: (val: string | number) => void;
  options: { value: string | number; label: string }[];
}

export default function IOSSegmentedControl({ value, onChange, options }: IOSSegmentedControlProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  return (
    <Box sx={{
      display: 'inline-flex',
      backgroundColor: isDark ? 'rgba(118,118,128,0.24)' : 'rgba(118,118,128,0.12)',
      borderRadius: '9px',
      padding: '2px',
      gap: '2px',
    }}>
      {options.map((opt) => {
        const isActive = opt.value === value;
        return (
          <Box
            key={opt.value}
            onClick={() => onChange(opt.value)}
            sx={{
              px: 2, py: 0.5,
              borderRadius: '7px',
              fontSize: 13, fontWeight: 600,
              cursor: 'pointer',
              transition: 'all 200ms cubic-bezier(0.25,0.46,0.45,0.94)',
              userSelect: 'none',
              color: isActive ? (isDark ? '#FFFFFF' : '#000000') : (isDark ? 'rgba(235,235,245,0.6)' : 'rgba(60,60,67,0.6)'),
              backgroundColor: isActive
                ? (isDark ? 'rgba(255,255,255,0.18)' : '#FFFFFF')
                : 'transparent',
              boxShadow: isActive && !isDark ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
              '&:active': { transform: 'scale(0.96)' },
            }}
          >
            {opt.label}
          </Box>
        );
      })}
    </Box>
  );
}
