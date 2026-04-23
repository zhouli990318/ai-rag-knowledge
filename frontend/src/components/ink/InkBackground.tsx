import { Box, SxProps } from '@mui/material';
import { ink } from '../../theme/ThemeProvider';

interface InkBackgroundProps {
  sx?: SxProps;
}

export default function InkBackground({ sx }: InkBackgroundProps) {
  return (
    <Box
      sx={{
        position: 'absolute',
        inset: 0,
        pointerEvents: 'none',
        overflow: 'hidden',
        ...sx,
      }}
    >
      {/* 水墨山水 SVG 背景 - 多层远山近水效果 */}
      <Box
        component="svg"
        viewBox="0 0 800 500"
        preserveAspectRatio="xMidYMax slice"
        sx={{
          position: 'absolute',
          width: '100%',
          height: '100%',
          bottom: 0,
          left: 0,
        }}
      >
        <defs>
          <linearGradient id="bg-far" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#A09A8E" stopOpacity={0.15} />
            <stop offset="60%" stopColor="#C5C0B6" stopOpacity={0.08} />
            <stop offset="100%" stopColor="#E0DCD4" stopOpacity={0.02} />
          </linearGradient>
          <linearGradient id="bg-mid" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#8A8478" stopOpacity={0.22} />
            <stop offset="50%" stopColor="#B0AAA0" stopOpacity={0.12} />
            <stop offset="100%" stopColor="#D5D0C8" stopOpacity={0.03} />
          </linearGradient>
          <linearGradient id="bg-near" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#6B665C" stopOpacity={0.28} />
            <stop offset="40%" stopColor="#9A9488" stopOpacity={0.14} />
            <stop offset="100%" stopColor="#C8C2B8" stopOpacity={0.03} />
          </linearGradient>
          <linearGradient id="bg-water" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#B0AAA0" stopOpacity={0.06} />
            <stop offset="100%" stopColor="#F5F3EE" stopOpacity={0.01} />
          </linearGradient>
          <linearGradient id="bg-mist" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stopColor="#E8E4DC" stopOpacity={0} />
            <stop offset="30%" stopColor="#E8E4DC" stopOpacity={0.3} />
            <stop offset="70%" stopColor="#E8E4DC" stopOpacity={0.3} />
            <stop offset="100%" stopColor="#E8E4DC" stopOpacity={0} />
          </linearGradient>
          <filter id="bg-blur-sm"><feGaussianBlur in="SourceGraphic" stdDeviation={2} /></filter>
          <filter id="bg-blur-md"><feGaussianBlur in="SourceGraphic" stdDeviation={4} /></filter>
          <filter id="bg-blur-lg"><feGaussianBlur in="SourceGraphic" stdDeviation={6} /></filter>
        </defs>

        {/* 最远的山 */}
        <path
          d="M-50 360 Q80 180 180 300 Q280 130 400 270 Q520 110 620 250 Q720 150 850 280 L850 500 L-50 500 Z"
          fill="url(#bg-far)" filter="url(#bg-blur-lg)"
        />

        {/* 中层山 */}
        <path
          d="M-30 400 Q100 230 220 340 Q350 180 480 320 Q600 210 700 335 Q780 260 870 350 L870 500 L-30 500 Z"
          fill="url(#bg-mid)" filter="url(#bg-blur-md)"
        />

        {/* 山脊墨线 */}
        <path
          d="M-30 400 Q100 230 220 340 Q350 180 480 320 Q600 210 700 335"
          fill="none" stroke="#8A8478" strokeWidth={0.6} opacity={0.1} filter="url(#bg-blur-sm)"
        />

        {/* 云雾层 */}
        <rect x="0" y="340" width="800" height="35" fill="url(#bg-mist)" filter="url(#bg-blur-md)" />

        {/* 近处山 */}
        <path
          d="M0 430 Q120 290 260 380 Q400 260 550 375 Q680 300 800 400 L800 500 L0 500 Z"
          fill="url(#bg-near)" filter="url(#bg-blur-sm)"
        />

        {/* 飞鸟 */}
        <g opacity={0.18} fill="none" stroke={ink.gray} strokeLinecap="round">
          <path d="M280 140 Q285 133 292 138" strokeWidth={1} />
          <path d="M292 138 Q297 133 304 140" strokeWidth={1} />
          <path d="M330 115 Q334 109 340 114" strokeWidth={0.8} />
          <path d="M340 114 Q344 109 350 116" strokeWidth={0.8} />
          <path d="M250 162 Q254 157 259 161" strokeWidth={0.7} />
          <path d="M259 161 Q263 157 267 163" strokeWidth={0.7} />
        </g>

        {/* 水面 */}
        <rect x="0" y="460" width="800" height="40" fill="url(#bg-water)" />

        {/* 水面波纹 */}
        <g opacity={0.08} stroke={ink.gray} strokeWidth={0.5} fill="none">
          {[...Array(5)].map((_, i) => (
            <path key={i} d={`M${40 + i * 160} 468 Q${80 + i * 160} 464 ${120 + i * 160} 468`} />
          ))}
        </g>

        {/* 亭子剪影 */}
        <g opacity={0.1} fill={ink.gray}>
          <polygon points="62,385 40,400 84,400" />
          <line x1="40" y1="400" x2="37" y2="399" stroke={ink.gray} strokeWidth={0.6} />
          <line x1="84" y1="400" x2="87" y2="399" stroke={ink.gray} strokeWidth={0.6} />
          <rect x="45" y="400" width="34" height="18" rx={0.5} opacity={0.5} />
          <line x1="48" y1="400" x2="48" y2="418" stroke={ink.gray} strokeWidth={1} />
          <line x1="76" y1="400" x2="76" y2="418" stroke={ink.gray} strokeWidth={1} />
        </g>
      </Box>
    </Box>
  );
}
