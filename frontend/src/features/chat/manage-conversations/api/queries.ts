import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { chatApi } from '@/entities/chat';
import { useSnackbar } from 'notistack';

export function useConversations() {
  return useQuery({
    queryKey: ['conversations'],
    queryFn: chatApi.getConversations,
  });
}

export function useConversation(id: number | null) {
  return useQuery({
    queryKey: ['conversation', id],
    queryFn: () => chatApi.getConversation(id!),
    enabled: !!id,
  });
}

export function useDeleteConversation() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: chatApi.deleteConversation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      enqueueSnackbar('对话已删除', { variant: 'success' });
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
}
