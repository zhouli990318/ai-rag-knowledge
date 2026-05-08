import { ReactNode, ButtonHTMLAttributes } from 'react';
import { Box } from '@mui/material';
import { ink, sansFont } from '../../theme/ThemeProvider';

interface InkButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children?: ReactNode;
  variant?: 'filled' | 'outlined' | 'ghost' | 'cinnabar';
  size?: 'sm' | 'md' | 'lg';
}

export default function InkButton({ children, variant = 'filled', size = 'md', ...props }: InkButtonProps) {
  const sizes = {
    sm: { px: '12px', py: '6px', fontSize: 13, radius: 3 },
    md: { px: '18px', py: '8px', fontSize: 14.5, radius: 4 },
    lg: { px: '24px', py: '10px', fontSize: 16, radius: 4 },
  };

  const s = sizes[size];
  const baseStyles: Record<string, any> = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 0.5,
    fontFamily: sansFont,
    fontWeight: 500,
    cursor: props.disabled ? 'default' : 'pointer',
    opacity: props.disabled ? 0.45 : 1,
    border: 'none',
    outline: 'none',
    transition: 'all 200ms ease-in-out',
    letterSpacing: '0.02em',
  };

  const variants: Record<string, Record<string, any>> = {
    filled: {
      ...baseStyles,
      backgroundColor: ink.gray,
      color: '#FFFFFF',
      borderRadius: s.radius,
      padding: `${s.py} ${s.px}`,
      fontSize: s.fontSize,
      backgroundImage: `linear-gradient(135deg, ${ink.gray} 0%, #3A3A3A 100%)`,
      '&:hover:not([disabled])': { backgroundImage: `linear-gradient(135deg, #5A5A5A 0%, ${ink.gray} 100%)`, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' },
      '&:active:not([disabled])': { transform: 'scale(0.97)' },
    },
    outlined: {
      ...baseStyles,
      backgroundColor: 'rgba(255,255,255,0.7)',
      color: ink.gray,
      borderRadius: s.radius,
      padding: `${s.py} ${s.px}`,
      fontSize: s.fontSize,
      border: `1px solid rgba(224,221,216,0.7)`,
      backdropFilter: 'blur(8px)',
      '&:hover:not([disabled])': { borderColor: ink.cinnabar, color: ink.cinnabar, backgroundColor: 'rgba(255,255,255,0.9)' },
    },
    ghost: {
      ...baseStyles,
      background: 'transparent',
      color: ink.gray,
      borderRadius: s.radius,
      padding: `${s.py} ${s.px}`,
      fontSize: s.fontSize,
      '&:hover:not([disabled])': { backgroundColor: 'rgba(74,74,74,0.05)', color: ink.cinnabar },
    },
    cinnabar: {
      ...baseStyles,
      backgroundColor: ink.cinnabar,
      color: '#FFFFFF',
      borderRadius: s.radius,
      padding: `${s.py} ${s.px}`,
      fontSize: s.fontSize,
      backgroundImage: `linear-gradient(135deg, ${ink.cinnabar} 0%, #B53D26 100%)`,
      '&:hover:not([disabled])': { backgroundImage: 'linear-gradient(135deg, #D45A42 0%, #C84B31 100%)', boxShadow: '0 2px 12px rgba(200,75,49,0.25)' },
      '&:active:not([disabled])': { transform: 'scale(0.97)' },
    },
  };

  return (
    <Box
      component="button"
      sx={variants[variant]}
      {...props as any}
    >
      {children}
    </Box>
  );
}
