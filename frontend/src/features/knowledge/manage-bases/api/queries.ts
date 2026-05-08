import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { knowledgeApi } from '@/entities/knowledge';
import type { CreateKnowledgeBaseRequest } from '@/entities/knowledge';
import { useSnackbar } from 'notistack';

export function useKnowledgeBases() {
  return useQuery({
    queryKey: ['knowledgeBases'],
    queryFn: knowledgeApi.list,
  });
}

export function useCreateKnowledgeBase() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (data: CreateKnowledgeBaseRequest) => knowledgeApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] });
      enqueueSnackbar('创建成功', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '创建失败', { variant: 'error' }),
  });
}

export function useDeleteKnowledgeBase() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: knowledgeApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] });
      enqueueSnackbar('删除成功', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
}

export function useRebuildVectors() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (kbId: number) => knowledgeApi.rebuildVectors(kbId),
    onSuccess: (msg) => {
      queryClient.invalidateQueries({ queryKey: ['knowledgeBases'] });
      enqueueSnackbar(msg || '向量库重建完成', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || e?.message || '重建失败', { variant: 'error' }),
  });
}
