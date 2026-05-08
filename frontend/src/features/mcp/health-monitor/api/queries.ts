import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mcpGatewayApi } from '@/entities/mcp';
import { useSnackbar } from 'notistack';

export function useSourcesHealth() {
  return useQuery({
    queryKey: ['mcp-health'],
    queryFn: mcpGatewayApi.listSourcesHealth,
    refetchInterval: 30000,
  });
}

export function useTriggerHealthCheck() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: mcpGatewayApi.triggerHealthCheck,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mcp-health'] });
      enqueueSnackbar('健康检查已完成', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '检查失败', { variant: 'error' }),
  });
}
