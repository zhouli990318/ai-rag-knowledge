import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Response } from '../models/response.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  constructor(private http: HttpClient) {}

  // 获取知识库标签列表
  getRagTags(): Observable<Response<string[]>> {
    return this.http.get<Response<string[]>>('/ai/v1/rag/query_rag_tag_list');
  }

  // 生成回答（非流式）
  generate(llm: string, model: string, message: string): Observable<any> {
    return this.http.get(`/ai/v1/${llm}/generate`, {
      params: {
        model,
        message
      }
    });
  }

  // 流式生成回答
  generateStream(llm: string, model: string, message: string): Observable<string> {
    return new Observable<string>(observer => {
      const eventSource = new EventSource(
        `/ai/v1/${llm}/generate_stream?model=${encodeURIComponent(model)}&message=${encodeURIComponent(message)}`
      );

      let buffer = '';
      let hasError = false;
      const flushBuffer = () => {
        if (buffer.length > 0) {
          observer.next(buffer);
          buffer = '';
        }
      };

      // 设置定时刷新缓冲区，减少UI更新频率
      const bufferInterval = setInterval(flushBuffer, 100); // 每100ms刷新一次缓冲区

      eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log('收到SSE消息', data);

          // 检查是否有错误信息
          if (data && data.code && data.code !== '200') {
            console.error('后端返回错误', data);
            hasError = true;
            return;
          }

          // 处理 Spring AI ChatResponse 格式
          if (data && data.result) {
            const content = data.result.output.text;
            
            if (content) {
              if (content === '<think>') {
                // 发送特殊标记表示这是模型思考过程的开始
                observer.next('__THINKING_START__');
                return;
              } else if (content.trim() === '') {
                return; // 忽略空内容
              }

              buffer += content;

              // 如果缓冲区达到一定大小，立即刷新
              if (buffer.length > 20) {
                flushBuffer();
              }
            }
            
            // 检查是否到达结束标识
            if (data.result.metadata.finishReason === 'stop') {
              flushBuffer();
              observer.complete();
              eventSource.close();
              clearInterval(bufferInterval);
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
        clearInterval(bufferInterval);
        flushBuffer(); // 确保刷新剩余的缓冲区内容
        eventSource.close();
        if (!hasError) { // 只有在没有已知错误的情况下才报告错误
          observer.error(error);
        }
        observer.complete();
      };
      
      // 监听complete事件
      eventSource.addEventListener('complete', () => {
        console.log('收到complete事件');
        clearInterval(bufferInterval);
        flushBuffer(); // 确保刷新剩余的缓冲区内容
        eventSource.close();
        observer.complete();
      });
      
      return () => {
        clearInterval(bufferInterval);
        eventSource.close();
      };
    });
  }
  
  // 使用RAG流式生成回答
  generateStreamRag(llm: string, model: string, ragTag: string, message: string): Observable<string> {
    return new Observable<string>(observer => {
      const eventSource = new EventSource(
        `/ai/v1/${llm}/generate_stream_rag?model=${encodeURIComponent(model)}&ragTag=${encodeURIComponent(ragTag)}&message=${encodeURIComponent(message)}`
      );
      
      let buffer = '';
      let hasError = false;
      const flushBuffer = () => {
        if (buffer.length > 0) {
          observer.next(buffer);
          buffer = '';
        }
      };
      
      // 设置定时刷新缓冲区，减少UI更新频率
      const bufferInterval = setInterval(flushBuffer, 100); // 每100ms刷新一次缓冲区
      
      eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log('收到RAG SSE消息', data);
          
          // 检查是否有错误信息
          if (data && data.code && data.code !== '200') {
            console.error('后端返回错误', data);
            hasError = true;
            return;
          }
          
          // 处理 Spring AI ChatResponse 格式
          if (data && data.result) {
            const content = data.result.output.text;
            
            if (content) {
              if (content === '<think>') {
                // 发送特殊标记表示这是模型思考过程的开始
                observer.next('__THINKING_START__');
                return;
              } else if (content.trim() === '') {
                return; // 忽略空内容
              }

              buffer += content;

              // 如果缓冲区达到一定大小，立即刷新
              if (buffer.length > 20) {
                flushBuffer();
              }
            }
            
            // 检查是否到达结束标识
            if (data.result.metadata.finishReason === 'stop') {
              flushBuffer();
              observer.complete();
              eventSource.close();
              clearInterval(bufferInterval);
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
        clearInterval(bufferInterval);
        flushBuffer(); // 确保刷新剩余的缓冲区内容
        eventSource.close();
        if (!hasError) { // 只有在没有已知错误的情况下才报告错误
          observer.error(error);
        }
        observer.complete();
      };
      
      // 监听complete事件
      eventSource.addEventListener('complete', () => {
        console.log('收到complete事件');
        clearInterval(bufferInterval);
        flushBuffer(); // 确保刷新剩余的缓冲区内容
        eventSource.close();
        observer.complete();
      });
      
      return () => {
        clearInterval(bufferInterval);
        eventSource.close();
      };
    });
  }
}
