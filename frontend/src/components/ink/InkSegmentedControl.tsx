import { ReactNode } from 'react';
import { Box, SxProps } from '@mui/material';
import { ink, radius } from '../../theme/ThemeProvider';

interface Segment {
  value: string | number;
  label: string;
}

interface InkSegmentedControlProps {
  value: string | number;
  onChange: (val: string | number) => void;
  options: Segment[];
  sx?: SxProps;
}

export default function InkSegmentedControl({ value, onChange, options, sx }: InkSegmentedControlProps) {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        backgroundColor: 'rgba(74,74,74,0.05)',
        borderRadius: radius.sm,
        p: 0.25,
        gap: 0,
        ...sx,
      }}
    >
      {options.map((opt) => {
        const isActive = value === opt.value;
        return (
          <Box
            key={opt.value}
            onClick={() => onChange(opt.value)}
            sx={{
              px: 1.5, py: 0.5,
              borderRadius: radius.sm - 2,
              fontSize: 12.5,
              fontWeight: 500,
              cursor: 'pointer',
              color: isActive ? '#FFFFFF' : ink.lightGray,
              backgroundColor: isActive ? ink.gray : 'transparent',
              transition: 'all 200ms ease-in-out',
              whiteSpace: 'nowrap',
              '&:hover': {
                color: isActive ? '#FFFFFF' : ink.gray,
                backgroundColor: isActive ? ink.gray : 'rgba(74,74,74,0.06)',
              },
            }}
          >
            {opt.label}
          </Box>
        );
      })}
    </Box>
  );
}
