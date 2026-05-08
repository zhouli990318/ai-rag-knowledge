import { InputBase, SxProps } from '@mui/material';
import { ink, radius, sansFont, useInk } from '../../theme/ThemeProvider';

interface InkInputProps {
  value: string;
  onChange: (val: string) => void;
  placeholder?: string;
  sx?: SxProps;
  multiline?: boolean;
  maxRows?: number;
  disabled?: boolean;
}

export default function InkInput({ value, onChange, placeholder = '', sx, multiline = false, maxRows = 5, disabled }: InkInputProps) {
  const di = useInk();
  return (
    <InputBase
      fullWidth
      multiline={multiline}
      maxRows={maxRows}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      disabled={disabled}
      sx={{
        fontSize: 14,
        fontFamily: sansFont,
        backgroundColor: 'rgba(255,255,255,0.6)',
        borderRadius: radius.sm,
        border: '1px solid rgba(224,221,216,0.6)',
        px: 1.5, py: 0.75,
        transition: 'all 200ms ease-in-out',
        '& input, & textarea': { py: 0.3 },
        '&::placeholder': { color: di.muted, opacity: 1 },
        '&:hover': { borderColor: '#D0CCC6', backgroundColor: 'rgba(255,255,255,0.85)' },
        '&.Mui-focused': {
          borderColor: di.gray,
          backgroundColor: '#FFFFFF',
          boxShadow: '0 0 0 2px rgba(74,74,74,0.08)',
        },
        ...sx,
      }}
    />
  );
}
