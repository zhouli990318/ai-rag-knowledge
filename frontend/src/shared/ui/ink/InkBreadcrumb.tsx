import { Box, Breadcrumbs, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { serifFont, useInk } from '../../theme/ThemeProvider';

interface BreadcrumbItem {
  label: string;
  path?: string;
}

interface InkBreadcrumbProps {
  items: BreadcrumbItem[];
}

export default function InkBreadcrumb({ items }: InkBreadcrumbProps) {
  const navigate = useNavigate();
  const di = useInk();

  if (items.length <= 1) return null;

  return (
    <Breadcrumbs
      separator={<Typography sx={{ color: di.muted, fontSize: 12, mx: -0.3 }}>›</Typography>}
      sx={{ mb: 1.5 }}
    >
      {items.map((item, idx) => {
        const isLast = idx === items.length - 1;
        return isLast ? (
          <Typography
            key={idx}
            sx={{
              fontSize: 12.5,
              fontFamily: serifFont,
              color: di.gray,
              fontWeight: 500,
            }}
          >
            {item.label}
          </Typography>
        ) : (
          <Box
            key={idx}
            component="span"
            onClick={() => item.path && navigate(item.path)}
            sx={{
              fontSize: 12.5,
              fontFamily: serifFont,
              color: di.lightGray,
              cursor: item.path ? 'pointer' : 'default',
              transition: 'color 150ms ease',
              '&:hover': item.path ? { color: di.cinnabar } : {},
            }}
          >
            {item.label}
          </Box>
        );
      })}
    </Breadcrumbs>
  );
}
