import { memo, useState } from 'react';
import {
  Box, Typography, ButtonBase,
  Accordion, AccordionSummary, AccordionDetails,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  AddOutlined as AddIcon,
  CloudOutlined as WeatherIcon,
  SearchOutlined as SearchIcon,
  CodeOutlined as CodeIcon,
  ExpandMoreOutlined as ExpandMoreIcon,
} from '@mui/icons-material';
import { providerApi } from '@/entities/provider';
import { knowledgeApi } from '@/entities/knowledge';
import { mcpGatewayApi } from '@/entities/mcp';
import type { Provider } from '@/entities/provider';
import type { McpApiSource } from '@/entities/mcp';
import { useChatConfigStore } from '@/features/chat';
import { InkBadge, InkSegmentedControl } from '@/shared/ui/ink';
import { ink, radius, serifFont, sansFont, useInk } from '@/shared/theme/ThemeProvider';
import { useThemeStore } from '@/shared/stores/themeStore';

export default memo(function ChatConfig() {
  const navigate = useNavigate();
  const di = useInk();
  const mode = useThemeStore((s) => s.mode);
  const {
    selectedProvider, setSelectedProvider,
    selectedKb, setSelectedKb,
    toolMode, setToolMode,
    selectedMcpServers, setSelectedMcpServers,
  } = useChatConfigStore();

  const [mcpExpanded, setMcpExpanded] = useState(false);
  const [kbExpanded, setKbExpanded] = useState(false);
  const [expanded, setExpanded] = useState<string | false>('mcp');

  const handleAccordion = (panel: string) => (_: unknown, isExpanded: boolean) => {
    setExpanded(isExpanded ? panel : false);
  };

  const { data: providers = [] } = useQuery({ queryKey: ['providers'], queryFn: providerApi.list });
  const { data: kbs = [] } = useQuery({ queryKey: ['knowledgeBases'], queryFn: knowledgeApi.list });
  const { data: mcpSources = [] } = useQuery({ queryKey: ['mcp-sources'], queryFn: mcpGatewayApi.listSources });
  const { data: mcpHealth = [] } = useQuery({
    queryKey: ['mcp-health'],
    queryFn: mcpGatewayApi.listSourcesHealth,
    refetchInterval: 30000,
    refetchIntervalInBackground: false,
  });

  const enabledProviders = providers.filter((p: Provider) => p.enabled);
  const activeMcpSources = mcpSources.filter((s: McpApiSource) => s.active);
  const healthMap = new Map(mcpHealth.map((h) => [h.id, h]));

  /* ── 手风琴公共样式 —— 独立悬浮卡片 ── */
  const accordionSx = {
    '&.MuiAccordion-root': {
      backgroundColor: mode === 'dark' ? '#2A2826' : '#f7f2e6',
      backgroundImage: 'none',
      boxShadow: mode === 'dark'
        ? '0 4px 20px rgba(0,0,0,0.3), 0 1px 4px rgba(0,0,0,0.2)'
        : '0 4px 20px rgba(0,0,0,0.08), 0 1px 4px rgba(0,0,0,0.04)',
      border: `1px solid ${mode === 'dark' ? 'rgba(255,255,255,0.06)' : 'rgba(224,220,213,0.5)'}`,
      borderRadius: '14px',
      overflow: 'hidden',
      '&::before': { display: 'none' },
      '&.Mui-expanded': {
        margin: 0,
        borderColor: mode === 'dark' ? 'rgba(255,255,255,0.1)' : 'rgba(200,196,190,0.6)',
        boxShadow: mode === 'dark'
          ? '0 6px 28px rgba(0,0,0,0.35), 0 2px 6px rgba(0,0,0,0.25)'
          : '0 6px 28px rgba(0,0,0,0.1), 0 2px 6px rgba(0,0,0,0.05)',
      },
    },
  };
  const summarySx = {
    minHeight: 44,
    px: 1.5,
    '&.Mui-expanded': { minHeight: 44 },
    '& .MuiAccordionSummary-content': { margin: '8px 0', '&.Mui-expanded': { margin: '8px 0' } },
  };

  /* ── 折叠态摘要 ── */
  const mcpSummary = `${activeMcpSources.length} 个服务 · ${toolMode === 'OFF' ? '关闭' : toolMode === 'AUTO' ? '自动' : '指定'}`;
  const selectedProviderObj = enabledProviders.find((p: Provider) => p.id === selectedProvider);
  const modelSummary = selectedProviderObj ? `${selectedProviderObj.name}` : `${enabledProviders.length} 个模型`;
  const selectedKbObj = kbs.find((kb: any) => kb.id === selectedKb);
  const kbSummary = selectedKbObj ? `${selectedKbObj.name}` : `${kbs.length} 个知识库`;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, height: '100%' }}>
      {/* ═══════ MCP 服务 ═══════ */}
      <Accordion expanded={expanded === 'mcp'} onChange={handleAccordion('mcp')} sx={accordionSx}>
        <AccordionSummary expandIcon={<ExpandMoreIcon sx={{ fontSize: 18, color: di.lightGray }} />} sx={summarySx}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flex: 1, minWidth: 0 }}>
            <Box sx={{
              width: 20, height: 20, borderRadius: radius.xs + 1, flexShrink: 0,
              background: `linear-gradient(135deg, ${di.gray}, ${di.black})`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#FFF', fontSize: 11,
            }}>✦</Box>
            <Typography sx={{ fontSize: 14, fontWeight: 600, fontFamily: serifFont }}>
              MCP 服务
            </Typography>
            {expanded !== 'mcp' && (
              <Typography sx={{ fontSize: 11, color: di.muted, ml: 'auto', mr: 1, flexShrink: 0 }}>
                {mcpSummary}
              </Typography>
            )}
          </Box>
        </AccordionSummary>
        <AccordionDetails sx={{ px: 1.5, pt: 0, pb: 1.5 }}>
          {/* 查看全部 + 添加 */}
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.75 }}>
            <Box
              component={ButtonBase}
              onClick={() => navigate('/mcp')}
              sx={{
                display: 'flex', alignItems: 'center', gap: 0.5,
                py: 0.5, px: 1,
                border: `1px dashed ${di.glassBorder}`,
                borderRadius: radius.sm,
                color: di.lightGray, fontSize: 12,
                transition: 'all 180ms ease-in-out',
                '&:hover': { borderColor: di.cinnabar, color: di.cinnabar },
              }}
            >
              <AddIcon sx={{ fontSize: 14 }} />
              添加服务
            </Box>
            <ButtonBase
              onClick={() => navigate('/mcp')}
              sx={{ fontSize: 11, color: di.lightGray, '&:hover': { color: di.cinnabar } }}
            >
              查看全部 ›
            </ButtonBase>
          </Box>

          {/* 工具模式切换 */}
          <Box sx={{ mb: 0.75 }}>
            <Typography sx={{ fontSize: 11, color: di.lightGray, mb: 0.4, letterSpacing: 0.5 }}>
              工具使用
            </Typography>
            <InkSegmentedControl
              value={toolMode}
              onChange={(v) => setToolMode(v as 'OFF' | 'AUTO' | 'SPECIFIC')}
              options={[
                { value: 'OFF', label: '不使用' },
                { value: 'AUTO', label: 'AI 自动' },
                { value: 'SPECIFIC', label: '指定' },
              ]}
            />
            {toolMode === 'SPECIFIC' && (
              <Box sx={{ mt: 0.75, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {activeMcpSources.length === 0 ? (
                  <Typography sx={{ fontSize: 11.5, color: di.muted }}>暂无可用服务</Typography>
                ) : activeMcpSources.map((s: McpApiSource) => {
                  const picked = selectedMcpServers.includes(s.id);
                  return (
                    <Box
                      key={s.id}
                      onClick={() => setSelectedMcpServers(
                        picked ? selectedMcpServers.filter((id) => id !== s.id) : [...selectedMcpServers, s.id]
                      )}
                      sx={{
                        px: 0.8, py: 0.25, fontSize: 11,
                        borderRadius: radius.xs + 1,
                        border: `1px solid ${picked ? di.cinnabar : di.glassBorder}`,
                        color: picked ? di.cinnabar : di.gray,
                        backgroundColor: picked ? 'rgba(200,75,49,0.06)' : 'transparent',
                        cursor: 'pointer', transition: 'all 150ms',
                      }}
                    >
                      {s.name}
                    </Box>
                  );
                })}
              </Box>
            )}
          </Box>

          {/* 服务列表 */}
          {activeMcpSources.map((source: McpApiSource) => {
            const health = healthMap.get(source.id);
            const status = health?.healthStatus ?? 'UNKNOWN';
            const label =
              status === 'HEALTHY' ? '已连接' :
              status === 'DEGRADED' ? '缓慢' :
              status === 'UNREACHABLE' ? '不可达' : '未知';
            const badgeStatus: 'success' | 'warning' | 'error' | 'default' =
              status === 'HEALTHY' ? 'success' :
              status === 'DEGRADED' ? 'warning' :
              status === 'UNREACHABLE' ? 'error' : 'default';
            return (
              <Box key={source.id} sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.6 }}>
                <Box sx={{
                  width: 24, height: 24, borderRadius: radius.xs + 2,
                  backgroundColor: source.name.includes('天气') ? 'rgba(91,180,220,0.12)' :
                                 source.name.includes('搜索') ? 'rgba(74,144,226,0.10)' :
                                 source.name.includes('代码') ? 'rgba(44,44,44,0.08)' : 'rgba(74,74,74,0.06)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                  color: source.name.includes('天气') ? di.statusInfo :
                         source.name.includes('搜索') ? di.statusInfo : di.gray,
                }}>
                  {source.name.includes('天气') ? <WeatherIcon sx={{ fontSize: 14 }} /> :
                   source.name.includes('搜索') ? <SearchIcon sx={{ fontSize: 14 }} /> :
                   <CodeIcon sx={{ fontSize: 14 }} />}
                </Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography sx={{ fontSize: 12.5, fontWeight: 500 }}>{source.name}</Typography>
                </Box>
                <InkBadge label={label} status={badgeStatus} dot />
              </Box>
            );
          })}
          {activeMcpSources.length === 0 && (
            <Typography sx={{ fontSize: 11.5, color: di.muted, textAlign: 'center', py: 0.75 }}>暂无可用服务</Typography>
          )}
        </AccordionDetails>
      </Accordion>

      {/* ═══════ 模型管理 ═══════ */}
      <Accordion expanded={expanded === 'model'} onChange={handleAccordion('model')} sx={accordionSx}>
        <AccordionSummary expandIcon={<ExpandMoreIcon sx={{ fontSize: 18, color: di.lightGray }} />} sx={summarySx}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flex: 1, minWidth: 0 }}>
            <Box sx={{
              width: 20, height: 20, borderRadius: radius.xs + 1, flexShrink: 0,
              background: `linear-gradient(135deg, ${di.gray}, ${di.black})`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#FFF', fontSize: 11,
            }}>◆</Box>
            <Typography sx={{ fontSize: 14, fontWeight: 600, fontFamily: serifFont }}>
              模型管理
            </Typography>
            {expanded !== 'model' && (
              <Typography sx={{ fontSize: 11, color: di.muted, ml: 'auto', mr: 1, flexShrink: 0 }}>
                {modelSummary}
              </Typography>
            )}
          </Box>
        </AccordionSummary>
        <AccordionDetails sx={{ px: 1.5, pt: 0, pb: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.75 }}>
            <Box
              component={ButtonBase}
              onClick={() => navigate('/settings')}
              sx={{
                display: 'flex', alignItems: 'center', gap: 0.5,
                py: 0.5, px: 1,
                border: `1px dashed ${di.glassBorder}`,
                borderRadius: radius.sm,
                color: di.lightGray, fontSize: 12,
                transition: 'all 180ms ease-in-out',
                '&:hover': { borderColor: di.cinnabar, color: di.cinnabar },
              }}
            >
              <AddIcon sx={{ fontSize: 14 }} />
              添加模型
            </Box>
            <ButtonBase
              onClick={() => navigate('/settings')}
              sx={{ fontSize: 11, color: di.lightGray, '&:hover': { color: di.cinnabar } }}
            >
              查看全部 ›
            </ButtonBase>
          </Box>

          {enabledProviders.map((provider: Provider) => {
            const isSelected = selectedProvider === provider.id;
            return (
              <motion.div key={provider.id} whileTap={{ scale: 0.97 }}>
              <Box
                onClick={() => setSelectedProvider(provider.id)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 1, py: 0.6,
                  cursor: 'pointer', borderRadius: radius.xs + 2,
                  bgcolor: isSelected ? 'rgba(200,75,49,0.05)' : 'transparent',
                  transition: 'all 150ms ease-in-out',
                  '&:hover': { bgcolor: isSelected ? 'rgba(200,75,49,0.08)' : 'rgba(74,74,74,0.03)' },
                }}
              >
                <Box sx={{
                  width: 24, height: 24, borderRadius: '50%',
                  backgroundColor: di.black,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: '#FFF', fontSize: 9, fontWeight: 600, fontFamily: serifFont,
                  flexShrink: 0,
                }}>
                  墨
                </Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography sx={{ fontSize: 12.5, fontWeight: 500 }}>
                    {provider.name}
                    <span style={{ fontSize: 11, color: di.lightGray, marginLeft: 4 }}>
                      {provider.defaultModel && `· ${provider.defaultModel}`}
                    </span>
                  </Typography>
                </Box>
                <InkBadge
                  label={isSelected ? '使用中' : '已启用'}
                  status={isSelected ? 'cinnabar' : 'success'}
                />
              </Box>
              </motion.div>
            );
          })}
          {enabledProviders.length === 0 && (
            <Typography sx={{ fontSize: 12, color: di.muted, textAlign: 'center', py: 0.75 }}>
              暂无可用模型
            </Typography>
          )}
        </AccordionDetails>
      </Accordion>

      {/* ═══════ 知识库 ═══════ */}
      <Accordion expanded={expanded === 'kb'} onChange={handleAccordion('kb')} sx={accordionSx}>
        <AccordionSummary expandIcon={<ExpandMoreIcon sx={{ fontSize: 18, color: di.lightGray }} />} sx={summarySx}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flex: 1, minWidth: 0 }}>
            <Box sx={{
              width: 20, height: 20, borderRadius: radius.xs + 1, flexShrink: 0,
              background: `linear-gradient(135deg, ${di.teal}, #4A5D53)`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#FFF', fontSize: 11,
            }}>📚</Box>
            <Typography sx={{ fontSize: 14, fontWeight: 600, fontFamily: serifFont }}>
              知识库
            </Typography>
            {expanded !== 'kb' && (
              <Typography sx={{ fontSize: 11, color: di.muted, ml: 'auto', mr: 1, flexShrink: 0 }}>
                {kbSummary}
              </Typography>
            )}
          </Box>
        </AccordionSummary>
        <AccordionDetails sx={{ px: 1.5, pt: 0, pb: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.75 }}>
            <Box
              component={ButtonBase}
              onClick={() => navigate('/knowledge')}
              sx={{
                display: 'flex', alignItems: 'center', gap: 0.5,
                py: 0.5, px: 1,
                border: `1px dashed ${di.glassBorder}`,
                borderRadius: radius.sm,
                color: di.lightGray, fontSize: 12,
                transition: 'all 180ms ease-in-out',
                '&:hover': { borderColor: di.teal, color: di.teal },
              }}
            >
              <AddIcon sx={{ fontSize: 14 }} />
              创建知识库
            </Box>
            <ButtonBase
              onClick={() => navigate('/knowledge')}
              sx={{ fontSize: 11, color: di.lightGray, '&:hover': { color: di.cinnabar } }}
            >
              查看全部 ›
            </ButtonBase>
          </Box>

          {kbs.map((kb: any) => {
            const isSelected = selectedKb === kb.id;
            return (
              <motion.div key={kb.id} whileTap={{ scale: 0.97 }}>
              <Box
                onClick={() => setSelectedKb(isSelected ? 0 : kb.id)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 1, py: 0.6,
                  cursor: 'pointer', borderRadius: radius.xs + 2,
                  bgcolor: isSelected ? 'rgba(91,112,101,0.06)' : 'transparent',
                  transition: 'all 150ms ease-in-out',
                  '&:hover': { bgcolor: isSelected ? 'rgba(91,112,101,0.09)' : 'rgba(74,74,74,0.03)' },
                }}
              >
                <Box sx={{
                  width: 24, height: 24, borderRadius: radius.xs + 2,
                  backgroundColor: di.kbIconBg,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: di.teal, fontSize: 12,
                  flexShrink: 0,
                }}>📖</Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography sx={{ fontSize: 12.5, fontWeight: 500 }}>{kb.name}</Typography>
                  <Typography sx={{ fontSize: 10.5, color: di.muted }}>
                    {kb.documentCount ?? 0} 文档 · {kb.fileSize ?? '-'}
                  </Typography>
                </Box>
              </Box>
              </motion.div>
            );
          })}
          {kbs.length === 0 && (
            <Typography sx={{ fontSize: 11.5, color: di.muted, textAlign: 'center', py: 0.75 }}>暂无知识库</Typography>
          )}
        </AccordionDetails>
      </Accordion>
    </Box>
  );
});
