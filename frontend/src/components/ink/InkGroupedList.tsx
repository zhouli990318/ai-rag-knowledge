import { ReactNode } from 'react';
import { Box, Typography, SxProps } from '@mui/material';
import { ink, radius } from '../../theme/ThemeProvider';

interface GroupItem {
  label: string;
  value?: string;
  icon?: ReactNode;
  onClick?: () => void;
  right?: ReactNode;
}

interface InkGroupedListProps {
  title?: string;
  items: GroupItem[];
  sx?: SxProps;
}

export default function InkGroupedList({ title, items, sx }: InkGroupedListProps) {
  return (
    <Box
      sx={{
        backgroundColor: 'rgba(255,255,255,0.65)',
        backdropFilter: 'blur(8px)',
        WebkitBackdropFilter: 'blur(8px)',
        borderRadius: radius.md,
        border: '1px solid rgba(224,221,216,0.5)',
        overflow: 'hidden',
        ...sx,
      }}
    >
      {title && (
        <Typography
          sx={{
            px: 2, pt: 1.75, pb: 0.5,
            fontSize: 12,
            fontWeight: 600,
            color: ink.lightGray,
            textTransform: 'uppercase',
            letterSpacing: 1,
          }}
        >
          {title}
        </Typography>
      )}
      {items.map((item, i) => (
        <Box
          key={i}
          onClick={item.onClick}
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1.25,
            px: 2, py: 1.25,
            cursor: item.onClick ? 'pointer' : 'default',
            borderBottom: i < items.length - 1 ? '0.5px solid rgba(224,221,216,0.4)' : 'none',
            transition: 'background-color 150ms ease-in-out',
            '&:hover': item.onClick ? { backgroundColor: 'rgba(74,74,74,0.03)' } : {},
            '&:active': item.onClick ? { backgroundColor: 'rgba(74,74,74,0.06)' } : {},
          }}
        >
          {item.icon && (
            <Box sx={{ fontSize: 20, color: ink.lightGray, flexShrink: 0 }}>
              {item.icon}
            </Box>
          )}
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography sx={{ fontSize: 15, fontWeight: 500 }}>{item.label}</Typography>
            {item.value && (
              <Typography sx={{ fontSize: 13, color: ink.lightGray, mt: 0.25 }}>{item.value}</Typography>
            )}
          </Box>
          {item.right && <Box sx={{ flexShrink: 0 }}>{item.right}</Box>}
        </Box>
      ))}
      {!items.length && (
        <Box sx={{ py: 4, textAlign: 'center' }}>
          <Typography sx={{ fontSize: 14, color: ink.lightGray }}>暂无数据</Typography>
        </Box>
      )}
    </Box>
  );
}
