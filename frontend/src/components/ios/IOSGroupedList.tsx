import { ReactNode } from 'react';
import { Box, Typography, useTheme } from '@mui/material';

interface GroupItem {
  label: string;
  value?: ReactNode;
  onClick?: () => void;
  trailing?: ReactNode;
}

interface IOSGroupedListProps {
  header?: string;
  footer?: string;
  items?: GroupItem[];
  children?: ReactNode;
}

export default function IOSGroupedList({ header, footer, items, children }: IOSGroupedListProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  return (
    <Box sx={{ mb: 2.5 }}>
      {header && (
        <Typography sx={{
          fontSize: 13, fontWeight: 400, color: 'text.secondary',
          textTransform: 'uppercase', letterSpacing: 0.5,
          px: 2, mb: 0.75,
        }}>
          {header}
        </Typography>
      )}
      <Box sx={{
        backgroundColor: isDark ? '#1C1C1E' : '#FFFFFF',
        borderRadius: 2,
        overflow: 'hidden',
      }}>
        {items ? items.map((item, i) => (
          <Box
            key={i}
            onClick={item.onClick}
            sx={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              px: 2, py: 1.5, minHeight: 44,
              cursor: item.onClick ? 'pointer' : 'default',
              '&:active': item.onClick ? { backgroundColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.04)' } : {},
              ...(i < (items.length - 1) && {
                borderBottom: `0.5px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'}`,
                ml: 2, px: 0, pr: 2,
              }),
            }}
          >
            <Typography sx={{ fontSize: 17, fontWeight: 400 }}>{item.label}</Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {item.value && (
                <Typography sx={{ fontSize: 17, color: 'text.secondary' }}>{item.value}</Typography>
              )}
              {item.trailing}
            </Box>
          </Box>
        )) : children}
      </Box>
      {footer && (
        <Typography sx={{
          fontSize: 13, color: 'text.secondary',
          px: 2, mt: 0.75,
        }}>
          {footer}
        </Typography>
      )}
    </Box>
  );
}
