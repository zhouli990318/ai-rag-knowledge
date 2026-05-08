import { useQuery } from '@tanstack/react-query';
import { traceApi } from '@/entities/orchestration';

export function useTraces(conversationId: number | undefined) {
  return useQuery({
    queryKey: ['traces', conversationId],
    queryFn: () => traceApi.getByConversation(conversationId!),
    enabled: !!conversationId,
  });
}
