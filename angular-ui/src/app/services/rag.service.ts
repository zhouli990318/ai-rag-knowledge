import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Response } from '../models/response.model';

@Injectable({
  providedIn: 'root'
})
export class RagService {
  constructor(private http: HttpClient) {}
  
  // 获取知识库标签列表
  getRagTags(): Observable<Response<string[]>> {
    return this.http.get<Response<string[]>>('/ai/v1/rag/query_rag_tag_list');
  }
  
  // 上传文件到知识库
  uploadFiles(ragTag: string, files: File[]): Observable<Response<string>> {
    const formData = new FormData();
    formData.append('ragTag', ragTag);
    
    files.forEach(file => {
      formData.append('file', file);
    });
    
    return this.http.post<Response<string>>('/ai/v1/rag/file/upload', formData);
  }
  
  // 分析Git仓库
  analyzeGitRepository(repoUrl: string, userName: string, token: string): Observable<Response<string>> {
    const formData = new FormData();
    formData.append('repoUrl', repoUrl);
    formData.append('userName', userName);
    
    if (token) {
      formData.append('token', token);
    }
    
    return this.http.post<Response<string>>('/ai/v1/rag/analyze_git_repository', formData);
  }
}