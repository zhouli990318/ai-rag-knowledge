import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mcpGatewayApi, toolIndexApi } from '@/entities/mcp';
import { useSnackbar } from 'notistack';

const triggerReindex = () => { toolIndexApi.reindex().catch(() => {}); };

export function useSources() {
  return useQuery({
    queryKey: ['mcp-sources'],
    queryFn: mcpGatewayApi.listSources,
  });
}

export function useSourceConnectionInfo(sourceId: number | undefined) {
  return useQuery({
    queryKey: ['mcp-source-connection-info', sourceId],
    queryFn: () => mcpGatewayApi.getSourceConnectionInfo(sourceId!),
    enabled: !!sourceId,
  });
}

export function useCreateSource() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (data: Record<string, unknown>) => mcpGatewayApi.createSource(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      enqueueSnackbar('创建成功', { variant: 'success' });
      triggerReindex();
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '创建失败', { variant: 'error' }),
  });
}

export function useUpdateSource() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => mcpGatewayApi.updateSource(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      enqueueSnackbar('更新成功', { variant: 'success' });
      triggerReindex();
    },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
}

export function useDeleteSource() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: mcpGatewayApi.deleteSource,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      enqueueSnackbar('删除成功', { variant: 'success' });
      triggerReindex();
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
}

export function useToggleSource() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: mcpGatewayApi.toggleSourceActive,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-sources'] });
      queryClient.invalidateQueries({ queryKey: ['mcp-health'] });
      enqueueSnackbar('源状态已切换', { variant: 'success' });
      triggerReindex();
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '操作失败', { variant: 'error' }),
  });
}
