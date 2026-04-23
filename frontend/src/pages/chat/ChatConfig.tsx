import { memo, useState } from 'react';
import {
  Box, Typography, ButtonBase,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Add as AddIcon,
  Cloud as WeatherIcon,
  Search as SearchIcon,
  Code as CodeIcon,
} from '@mui/icons-material';
import { providerApi } from '../../api/providerApi';
import { knowledgeApi } from '../../api/knowledgeApi';
import { mcpGatewayApi } from '../../api/mcpApi';
import { Provider, McpApiSource } from '../../api/types';
import { useChatConfigStore } from '../../stores/chatConfigStore';
import { InkCard, InkBadge, InkSegmentedControl } from '../../components/ink';
import { ink, radius, serifFont, sansFont } from '../../theme/ThemeProvider';

export default memo(function ChatConfig() {
  const navigate = useNavigate();
  const {
    selectedProvider, setSelectedProvider,
    selectedKb, setSelectedKb,
    toolMode, setToolMode,
    selectedMcpServers, setSelectedMcpServers,
  } = useChatConfigStore();

  const [mcpExpanded, setMcpExpanded] = useState(false);
  const [kbExpanded, setKbExpanded] = useState(false);

  const { data: providers = [] } = useQuery({ queryKey: ['providers'], queryFn: providerApi.list });
  const { data: kbs = [] } = useQuery({ queryKey: ['knowledgeBases'], queryFn: knowledgeApi.list });
  const { data: mcpSources = [] } = useQuery({ queryKey: ['mcp-sources'], queryFn: mcpGatewayApi.listSources });
  const { data: mcpHealth = [] } = useQuery({
    queryKey: ['mcp-health'],
    queryFn: mcpGatewayApi.listSourcesHealth,
    refetchInterval: 30000,
  });

  const enabledProviders = providers.filter((p: Provider) => p.enabled);
  const activeMcpSources = mcpSources.filter((s: McpApiSource) => s.active);
  const healthMap = new Map(mcpHealth.map((h) => [h.id, h]));

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, height: '100%' }}>
      {/* ═══════ MCP 服务卡片 ═══════ */}
      <InkCard sx={{ flex: 1, minHeight: 0, borderRadius: `${radius.md + 10}px`, overflow: 'auto', display: 'flex', flexDirection: 'column', boxShadow: '0 12px 12px rgba(0,0,0,0.04)' }}>
        {/* 卡片标题 */}
        <Box sx={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          px: 2, pt: 1.75, pb: 1.25,
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box sx={{
              width: 20, height: 20, borderRadius: radius.xs + 1,
              background: `linear-gradient(135deg, ${ink.gray}, ${ink.black})`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#FFF', fontSize: 11,
            }}>✦</Box>
            <Typography sx={{ fontSize: 15, fontWeight: 600, fontFamily: serifFont }}>
              MCP 服务
            </Typography>
          </Box>
          <ButtonBase
            onClick={() => navigate('/mcp')}
            sx={{
              fontSize: 12, color: ink.lightGray,
              '&:hover': { color: ink.cinnabar },
              display: 'flex', alignItems: 'center', gap: 0.3,
            }}
          >
            查看全部
          </ButtonBase>
        </Box>

        {/* 添加服务按钮 */}
        <Box
          component={ButtonBase}
          onClick={() => navigate('/mcp')}
          sx={{
            display: 'flex', alignItems: 'center', gap: 0.6,
            mx: 2, mb: 1.25, py: 0.7, px: 1.2,
            border: `1px dashed ${ink.glassBorder}`,
            borderRadius: radius.sm,
            color: ink.lightGray,
            fontSize: 13,
            transition: 'all 180ms ease-in-out',
            '&:hover': { borderColor: ink.cinnabar, color: ink.cinnabar, bgcolor: 'rgba(200,75,49,0.03)' },
          }}
        >
          <AddIcon sx={{ fontSize: 16 }} />
          添加服务
        </Box>

        {/* 工具模式切换 */}
        <Box sx={{ px: 2, mb: 1.25 }}>
          <Typography sx={{ fontSize: 11, color: ink.lightGray, mb: 0.6, letterSpacing: 0.5 }}>
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
            <Box sx={{ mt: 1, display: 'flex', flexWrap: 'wrap', gap: 0.6 }}>
              {activeMcpSources.length === 0 ? (
                <Typography sx={{ fontSize: 11.5, color: ink.muted }}>
                  暂无可用服务
                </Typography>
              ) : activeMcpSources.map((s: McpApiSource) => {
                const picked = selectedMcpServers.includes(s.id);
                return (
                  <Box
                    key={s.id}
                    onClick={() => setSelectedMcpServers(
                      picked
                        ? selectedMcpServers.filter((id) => id !== s.id)
                        : [...selectedMcpServers, s.id]
                    )}
                    sx={{
                      px: 1, py: 0.3,
                      fontSize: 11.5,
                      borderRadius: radius.xs + 1,
                      border: `1px solid ${picked ? ink.cinnabar : ink.glassBorder}`,
                      color: picked ? ink.cinnabar : ink.gray,
                      backgroundColor: picked ? 'rgba(200,75,49,0.06)' : 'transparent',
                      cursor: 'pointer',
                      transition: 'all 150ms',
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
        <Box sx={{ px: 2, pb: 1.75, flex: 1, overflow: 'auto' }}>
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
            <Box key={source.id} sx={{ display: 'flex', alignItems: 'center', gap: 1.2, py: 0.85 }}>
              <Box sx={{
                width: 28, height: 28, borderRadius: radius.xs + 2,
                backgroundColor: source.name.includes('天气') ? 'rgba(91,180,220,0.12)' :
                               source.name.includes('搜索') ? 'rgba(74,144,226,0.10)' :
                               source.name.includes('代码') ? 'rgba(44,44,44,0.08)' : 'rgba(74,74,74,0.06)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                flexShrink: 0,
                color: source.name.includes('天气') ? '#5BB4DC' :
                       source.name.includes('搜索') ? '#4A90E2' : ink.gray,
              }}>
                {source.name.includes('天气') ? <WeatherIcon sx={{ fontSize: 16 }} /> :
                 source.name.includes('搜索') ? <SearchIcon sx={{ fontSize: 16 }} /> :
                 <CodeIcon sx={{ fontSize: 16 }} />}
              </Box>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography sx={{ fontSize: 13.5, fontWeight: 500 }}>{source.name}</Typography>
              </Box>
              <InkBadge label={label} status={badgeStatus} dot />
            </Box>
            );
          })}
          {activeMcpSources.length === 0 && (
            <Typography sx={{ fontSize: 12, color: ink.muted, textAlign: 'center', py: 1 }}>暂无可用服务</Typography>
          )}
        </Box>
      </InkCard>

      {/* ═══════ 模型管理卡片 ═══════ */}
      <InkCard sx={{ flex: 1, minHeight: 0, borderRadius: `${radius.md + 10}px`, overflow: 'auto', display: 'flex', flexDirection: 'column', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
        <Box sx={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          px: 2, pt: 1.75, pb: 1.25,
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box sx={{
              width: 20, height: 20, borderRadius: radius.xs + 1,
              background: `linear-gradient(135deg, ${ink.gray}, ${ink.black})`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#FFF', fontSize: 11,
            }}>◆</Box>
            <Typography sx={{ fontSize: 15, fontWeight: 600, fontFamily: serifFont }}>
              模型管理
            </Typography>
          </Box>
          <ButtonBase
            onClick={() => navigate('/settings')}
            sx={{ fontSize: 12, color: ink.lightGray, '&:hover': { color: ink.cinnabar }, display: 'flex', alignItems: 'center', gap: 0.3 }}
          >
            查看全部
          </ButtonBase>
        </Box>

        <Box
          component={ButtonBase}
          onClick={() => navigate('/settings')}
          sx={{
            display: 'flex', alignItems: 'center', gap: 0.6,
            mx: 2, mb: 1.25, py: 0.7, px: 1.2,
            border: `1px dashed ${ink.glassBorder}`,
            borderRadius: radius.sm,
            color: ink.lightGray, fontSize: 13,
            transition: 'all 180ms ease-in-out',
            '&:hover': { borderColor: ink.cinnabar, color: ink.cinnabar, bgcolor: 'rgba(200,75,49,0.03)' },
          }}
        >
          <AddIcon sx={{ fontSize: 16 }} />
          添加模型
        </Box>

        <Box sx={{ px: 2, pb: 1.75, flex: 1, overflow: 'auto' }}>
          {enabledProviders.map((provider: Provider) => {
            const isSelected = selectedProvider === provider.id;
            return (
              <Box
                key={provider.id}
                onClick={() => setSelectedProvider(provider.id)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 1.2, py: 0.85,
                  cursor: 'pointer', borderRadius: radius.xs + 2,
                  bgcolor: isSelected ? 'rgba(200,75,49,0.05)' : 'transparent',
                  transition: 'all 150ms ease-in-out',
                  '&:hover': { bgcolor: isSelected ? 'rgba(200,75,49,0.08)' : 'rgba(74,74,74,0.03)' },
                }}
              >
                <Box sx={{
                  width: 28, height: 28, borderRadius: '50%',
                  backgroundColor: ink.black,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: '#FFF', fontSize: 10, fontWeight: 600, fontFamily: serifFont,
                  flexShrink: 0,
                }}>
                  墨
                </Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography sx={{ fontSize: 13.5, fontWeight: 500 }}>
                    {provider.name}
                    <span style={{ fontSize: 12, color: ink.lightGray, marginLeft: 4 }}>
                      {provider.defaultModel && `· ${provider.defaultModel}`}
                    </span>
                  </Typography>
                </Box>
                <InkBadge
                  label={isSelected ? '当前使用' : '已启用'}
                  status={isSelected ? 'cinnabar' : 'success'}
                />
              </Box>
            );
          })}
          {enabledProviders.length === 0 && (
            <Typography sx={{ fontSize: 13, color: ink.muted, textAlign: 'center', py: 1.5 }}>
              暂无可用模型
            </Typography>
          )}
        </Box>
      </InkCard>

      {/* ═══════ 知识库卡片 ═══════ */}
      <InkCard sx={{ flex: 1, minHeight: 0, borderRadius: `${radius.md + 10}px`, overflow: 'auto', display: 'flex', flexDirection: 'column', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
        <Box sx={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          px: 2, pt: 1.75, pb: 1.25,
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box sx={{
              width: 20, height: 20, borderRadius: radius.xs + 1,
              background: `linear-gradient(135deg, ${ink.teal}, #4A5D53)`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#FFF', fontSize: 11,
            }}>📚</Box>
            <Typography sx={{ fontSize: 15, fontWeight: 600, fontFamily: serifFont }}>
              知识库
            </Typography>
          </Box>
          <ButtonBase
            onClick={() => navigate('/knowledge')}
            sx={{ fontSize: 12, color: ink.lightGray, '&:hover': { color: ink.cinnabar }, display: 'flex', alignItems: 'center', gap: 0.3 }}
          >
            查看全部
          </ButtonBase>
        </Box>

        <Box
          component={ButtonBase}
          onClick={() => navigate('/knowledge')}
          sx={{
            display: 'flex', alignItems: 'center', gap: 0.6,
            mx: 2, mb: 1.25, py: 0.7, px: 1.2,
            border: `1px dashed ${ink.glassBorder}`,
            borderRadius: radius.sm,
            color: ink.lightGray, fontSize: 13,
            transition: 'all 180ms ease-in-out',
            '&:hover': { borderColor: ink.teal, color: ink.teal, bgcolor: 'rgba(91,112,101,0.03)' },
          }}
        >
          <AddIcon sx={{ fontSize: 16 }} />
          创建知识库
        </Box>

        <Box sx={{ px: 2, pb: 1.75, flex: 1, overflow: 'auto' }}>
          {kbs.map((kb: any) => {
            const isSelected = selectedKb === kb.id;
            return (
              <Box
                key={kb.id}
                onClick={() => setSelectedKb(isSelected ? 0 : kb.id)}
                sx={{
                  display: 'flex', alignItems: 'center', gap: 1.2, py: 0.85,
                  cursor: 'pointer', borderRadius: radius.xs + 2,
                  bgcolor: isSelected ? 'rgba(91,112,101,0.06)' : 'transparent',
                  transition: 'all 150ms ease-in-out',
                  '&:hover': { bgcolor: isSelected ? 'rgba(91,112,101,0.09)' : 'rgba(74,74,74,0.03)' },
                }}
              >
                <Box sx={{
                  width: 28, height: 28, borderRadius: radius.xs + 2,
                  backgroundColor: ink.kbIconBg,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: ink.teal, fontSize: 14,
                  flexShrink: 0,
                }}>📖</Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography sx={{ fontSize: 13.5, fontWeight: 500 }}>{kb.name}</Typography>
                  <Typography sx={{ fontSize: 11, color: ink.muted }}>
                    {kb.documentCount ?? 0} 文档 · {kb.fileSize ?? '-'}
                  </Typography>
                </Box>
                <Typography sx={{ fontSize: 11, color: ink.muted, flexShrink: 0 }}>
                  更新于 {new Date().toLocaleDateString('zh-CN')}
                </Typography>
              </Box>
            );
          })}
          {kbs.length === 0 && (
            <Typography sx={{ fontSize: 12, color: ink.muted, textAlign: 'center', py: 1 }}>暂无知识库</Typography>
          )}
        </Box>
      </InkCard>
    </Box>
  );
});

