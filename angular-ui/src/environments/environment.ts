export const environment = {
  production: false,
  apiBaseUrl: '/ai/v1',
  apiEndpoints: {
    rag: {
      base: '/rag',
      tags: '/query_rag_tag_list',
      upload: '/file/upload',
      analyzeGit: '/analyze_git_repository'
    },
    chat: {
      generate: (llm: string) => `/${llm}/generate`,
      generateStream: (llm: string) => `/${llm}/generate_stream`,
      generateStreamRag: (llm: string) => `/${llm}/generate_stream_rag`
    }
  },
  bufferConfig: {
    flushInterval: 100, // ms
    sizeThreshold: 20
  }
};