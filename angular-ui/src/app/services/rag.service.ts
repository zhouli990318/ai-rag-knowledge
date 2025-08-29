import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, share } from 'rxjs/operators';
import { Response } from '../models/response.model';
import { environment } from '../../environments/environment';
import { UploadResponse } from '../models/api.interface';

@Injectable({
  providedIn: 'root'
})
export class RagService {
  private readonly http = inject(HttpClient);
  private readonly API_BASE = 'http://localhost:8091/ai/v1/rag';

  // 获取知识库标签列表
  getRagTags(): Observable<Response<string[]>> {
    return this.http.get<Response<string[]>>(`${this.API_BASE}/query_rag_tag_list`).pipe(
      share(), // 避免多个订阅者重复请求
      catchError(this.handleError)
    );
  }
  
  // 上传文件到知识库
  uploadFiles(ragTag: string, files: File[]): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('ragTag', ragTag);
    
    files.forEach(file => {
      formData.append('file', file);
    });
    
    return this.http.post<UploadResponse>(`${this.API_BASE}/file/upload`, formData).pipe(
      catchError(this.handleError)
    );
  }
  
  // 分析Git仓库
  analyzeGitRepository(repoUrl: string, userName: string, token: string): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('repoUrl', repoUrl);
    formData.append('userName', userName);
    
    if (token) {
      formData.append('token', token);
    }
    
    return this.http.post<UploadResponse>(`${this.API_BASE}/analyze_git_repository`, formData).pipe(
      catchError(this.handleError)
    );
  }

  // 上传文件
  uploadFile(file: File): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    
    const url = `${environment.apiBaseUrl}${environment.apiEndpoints.rag.base}${environment.apiEndpoints.rag.upload}`;
    return this.http.post<UploadResponse>(url, formData).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = '发生未知错误';
    
    if (error.error instanceof ErrorEvent) {
      // 客户端错误
      errorMessage = `错误: ${error.error.message}`;
    } else {
      // 服务器错误
      errorMessage = `错误代码: ${error.status}\n消息: ${error.message}`;
    }
    
    console.error('API错误:', errorMessage);
    return throwError(() => new Error(errorMessage));
  }
}