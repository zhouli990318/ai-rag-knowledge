import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { intentTreeApi } from '@/entities/orchestration';
import type { IntentNode } from '@/entities/orchestration';

export function useIntentTree() {
  return useQuery({
    queryKey: ['intentTree'],
    queryFn: intentTreeApi.getTree,
  });
}

export function useCreateIntent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (node: Partial<IntentNode>) => intentTreeApi.create(node),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['intentTree'] });
    },
  });
}

export function useDeleteIntent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => intentTreeApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['intentTree'] });
    },
  });
}
