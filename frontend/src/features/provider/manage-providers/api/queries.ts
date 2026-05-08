import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApi } from '@/entities/provider';
import { useSnackbar } from 'notistack';

export function useProviders() {
  return useQuery({
    queryKey: ['providers'],
    queryFn: providerApi.list,
  });
}

export function useProviderTypes() {
  return useQuery({
    queryKey: ['providerTypes'],
    queryFn: providerApi.types,
  });
}

export function useCreateProvider() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (data: Record<string, unknown>) => providerApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      enqueueSnackbar('创建成功', { variant: 'success' });
    },
    onError: () => enqueueSnackbar('创建失败', { variant: 'error' }),
  });
}

export function useUpdateProvider() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => providerApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      enqueueSnackbar('更新成功', { variant: 'success' });
    },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
}

export function useDeleteProvider() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: providerApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      enqueueSnackbar('删除成功', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
}

export function useToggleProvider() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: providerApi.toggle,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      enqueueSnackbar(data.enabled ? '已启用' : '已停用', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '操作失败', { variant: 'error' }),
  });
}

export function useTestProvider() {
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: providerApi.test,
    onSuccess: (data) => enqueueSnackbar(`连接成功: ${data}`, { variant: 'success' }),
    onError: () => enqueueSnackbar('连接失败', { variant: 'error' }),
  });
}
