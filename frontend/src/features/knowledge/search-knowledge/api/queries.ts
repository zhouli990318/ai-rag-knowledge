import { knowledgeApi } from '@/entities/knowledge';
import type { SearchResult } from '@/entities/knowledge';

export async function searchKnowledge(kbId: number, query: string, topK?: number): Promise<SearchResult[]> {
  return knowledgeApi.search(kbId, query, topK);
}
