import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { settingsApi } from '@/entities/settings';
import type { SystemSettings } from '@/entities/settings';

export function useSystemSettings() {
  return useQuery({
    queryKey: ['systemSettings'],
    queryFn: settingsApi.get,
  });
}

export function useUpdateSettings() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: SystemSettings) => settingsApi.update(data),
    onSuccess: (saved) => {
      queryClient.setQueryData(['systemSettings'], saved);
    },
  });
}
