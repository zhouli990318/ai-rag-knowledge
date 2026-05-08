import { styled } from '@mui/material/styles';
import Switch, { SwitchProps } from '@mui/material/Switch';

/**
 * InkSwitch — 水墨风格胶囊开关。
 * 关闭时轨道为淡灰，开启时朱砂红渐变；拇指白底带微投影。
 */
const StyledSwitch = styled((props: SwitchProps) => (
  <Switch disableRipple focusVisibleClassName=".Mui-focusVisible" {...props} />
))(({ theme }) => ({
  width: 36,
  height: 20,
  padding: 0,
  overflow: 'visible',
  '& .MuiSwitch-switchBase': {
    padding: 2,
    transitionDuration: '200ms',
    '&.Mui-checked': {
      transform: 'translateX(16px)',
      color: '#fff',
      '& + .MuiSwitch-track': {
        opacity: 1,
        backgroundColor: 'transparent',
        backgroundImage: 'linear-gradient(135deg, #C84B31 0%, #E07860 100%)',
        border: 'none',
      },
    },
    '&.Mui-focusVisible .MuiSwitch-thumb': {
      boxShadow: '0 0 0 3px rgba(200,75,49,0.25)',
    },
    '&.Mui-disabled': {
      opacity: 0.45,
    },
  },
  '& .MuiSwitch-thumb': {
    boxSizing: 'border-box',
    width: 16,
    height: 16,
    backgroundColor: '#FFFFFF',
    boxShadow: '0 1px 3px rgba(0,0,0,0.2), 0 0 1px rgba(0,0,0,0.08)',
  },
  '& .MuiSwitch-track': {
    borderRadius: 20 / 2,
    backgroundColor: 'rgba(120,120,120,0.22)',
    opacity: 1,
    border: '1px solid rgba(0,0,0,0.04)',
    transition: theme.transitions.create(['background-color', 'background-image', 'border'], {
      duration: 200,
    }),
  },
}));

export default function InkSwitch(props: SwitchProps) {
  return <StyledSwitch {...props} />;
}
