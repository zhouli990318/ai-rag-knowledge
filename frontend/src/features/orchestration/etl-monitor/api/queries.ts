import { useQuery } from '@tanstack/react-query';
import { etlTaskApi } from '@/entities/orchestration';

export function useEtlTasks(kbId: number | '' | undefined) {
  return useQuery({
    queryKey: ['etlTasks', kbId],
    queryFn: () => etlTaskApi.getByKnowledgeBase(kbId as number),
    enabled: !!kbId,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (!data) return false;
      const hasActive = data.some((t: any) => !['COMPLETED', 'FAILED'].includes(t.currentStage));
      return hasActive ? 3000 : false;
    },
  });
}
