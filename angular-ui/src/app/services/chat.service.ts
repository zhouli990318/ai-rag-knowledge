import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, share } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatResponse, RagTagsResponse } from '../models/api.interface';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly BUFFER_FLUSH_INTERVAL = environment.bufferConfig.flushInterval;
  private readonly BUFFER_SIZE_THRESHOLD = environment.bufferConfig.sizeThreshold;

  // 获取知识库标签列表
  getRagTags(): Observable<RagTagsResponse> {
    const url = `${environment.apiBaseUrl}${environment.apiEndpoints.rag.base}${environment.apiEndpoints.rag.tags}`;
    return this.http.get<RagTagsResponse>(url).pipe(
      share() // 避免多个订阅者重复请求
    );
  }

  // 生成回答（非流式）
  generate(llm: string, model: string, message: string): Observable<any> {
    const url = `${environment.apiBaseUrl}${environment.apiEndpoints.chat.generate(llm)}`;
    return this.http.get(url, {
      params: {
        model,
        message
      }
    });
  }

  // 流式生成回答
  generateStream(llm: string, model: string, message: string): Observable<string> {
    const url = `${environment.apiBaseUrl}${environment.apiEndpoints.chat.generateStream(llm)}?model=${encodeURIComponent(model)}&message=${encodeURIComponent(message)}`;
    return this.createStreamObservable(url);
  }
  
  // 使用RAG流式生成回答
  generateStreamRag(llm: string, model: string, ragTag: string, message: string): Observable<string> {
    const url = `${environment.apiBaseUrl}${environment.apiEndpoints.chat.generateStreamRag(llm)}?model=${encodeURIComponent(model)}&ragTag=${encodeURIComponent(ragTag)}&message=${encodeURIComponent(message)}`;
    return this.createStreamObservable(url);
  }

  private createStreamObservable(url: string): Observable<string> {
    return new Observable<string>(observer => {
      const eventSource = new EventSource(url);
      let buffer = '';
      let hasError = false;

      const flushBuffer = () => {
        if (buffer.length > 0) {
          observer.next(buffer);
          buffer = '';
        }
      };

      // 设置定时刷新缓冲区，减少UI更新频率
      const bufferInterval = setInterval(flushBuffer, this.BUFFER_FLUSH_INTERVAL);

      const cleanup = () => {
        clearInterval(bufferInterval);
        eventSource.close();
      };

      eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data) as ChatResponse;
          
          // 检查是否有错误信息
          if (data?.code && data.code !== '200') {
            console.error('后端返回错误', data);
            hasError = true;
            return;
          }

          // 处理 Spring AI ChatResponse 格式
          if (data?.result) {
            const content = data.result.output.text;
            
            if (content) {
              if (content === '<think>') {
                observer.next('__THINKING_START__');
                return;
              } else if (content.trim() === '') {
                return; // 忽略空内容
              }

              buffer += content;

              // 如果缓冲区达到阈值，立即刷新
              if (buffer.length > this.BUFFER_SIZE_THRESHOLD) {
                flushBuffer();
              }
            }
            
            // 检查是否到达结束标识
            if (data.result.metadata.finishReason === 'stop') {
              flushBuffer();
              observer.complete();
              cleanup();
              return;
            }
          } else {
            console.warn('收到无效的SSE消息格式', data);
          }
        } catch (error) {
          console.error('解析SSE消息失败', error, event.data);
        }
      };
      
      // SSE错误处理
      eventSource.onerror = (error) => {
        console.error('SSE错误', error);
        cleanup();
        flushBuffer(); // 确保刷新剩余的缓冲区内容
        
        if (!hasError) { // 只有在没有已知错误的情况下才报告错误
          observer.error(error);
        }
        observer.complete();
      };
      
      // 监听complete事件
      eventSource.addEventListener('complete', () => {
        console.log('收到complete事件');
        cleanup();
        flushBuffer(); // 确保刷新剩余的缓冲区内容
        observer.complete();
      });
      
      return cleanup;
    }).pipe(
      share() // 避免多个订阅者创建多个 EventSource 连接
    );
  }
}
