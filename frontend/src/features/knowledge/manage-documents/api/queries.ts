import { useQuery, useMutation } from '@tanstack/react-query';
import { knowledgeApi } from '@/entities/knowledge';
import { useSnackbar } from 'notistack';

export function useDocuments(kbId: number | undefined) {
  return useQuery({
    queryKey: ['kb-docs', kbId],
    queryFn: () => knowledgeApi.listDocuments(kbId!),
    enabled: !!kbId,
  });
}

export function useUploadDocument(kbId: number) {
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (file: File) => knowledgeApi.uploadDocument(kbId, file),
    onSuccess: () => enqueueSnackbar('上传成功', { variant: 'success' }),
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || e?.message || '上传失败', { variant: 'error' }),
  });
}

export function useDeleteDocument(kbId: number) {
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (docId: number) => knowledgeApi.deleteDocument(kbId, docId),
    onSuccess: () => enqueueSnackbar('文档已删除', { variant: 'success' }),
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '删除失败', { variant: 'error' }),
  });
}

export function useImportGit(kbId: number) {
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (url: string) => knowledgeApi.importGit(kbId, { repoUrl: url }),
    onSuccess: () => enqueueSnackbar('导入任务已提交', { variant: 'success' }),
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '导入失败', { variant: 'error' }),
  });
}
