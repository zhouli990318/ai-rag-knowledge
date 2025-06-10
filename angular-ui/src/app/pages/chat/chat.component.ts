import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MarkdownModule } from 'ngx-markdown';
import { ChatService } from '../../services/chat.service';
import { Message, MessageType } from '../../models/message.model';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnInit, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  
  messages = signal<Message[]>([]);
  newMessage = signal<string>('');
  isLoading = signal<boolean>(false);
  selectedLlm = signal<string>('ollama');
  selectedModel = signal<string>('deepseek-r1:8b');
  selectedRagTag = signal<string>('');
  ragTags = signal<string[]>([]);
  useRag = signal<boolean>(false);
  
  constructor(private chatService: ChatService) {}
  
  ngOnInit(): void {
    this.loadRagTags();
    
    // 添加欢迎消息
    this.messages.update(messages => [
      ...messages,
      {
        id: Date.now().toString(),
        content: '你好！我是 Spring AI 助手，有什么可以帮助你的吗？',
        type: MessageType.AI,
        timestamp: new Date()
      }
    ]);
  }
  
  ngAfterViewChecked() {
    this.scrollToBottom();
  }
  
  loadRagTags(): void {
    this.chatService.getRagTags().subscribe({
      next: (response) => {
        if (response && response.data) {
          this.ragTags.set(response.data);
          if (response.data.length > 0) {
            this.selectedRagTag.set(response.data[0]);
          }
        }
      },
      error: (error) => console.error('获取知识库标签失败', error)
    });
  }
  
  sendMessage(): void {
    const messageText = this.newMessage();
    if (!messageText.trim()) return;
    
    // 添加用户消息
    const userMessage: Message = {
      id: Date.now().toString(),
      content: messageText,
      type: MessageType.USER,
      timestamp: new Date()
    };
    
    this.messages.update(messages => [...messages, userMessage]);
    
    // 添加AI消息占位符
    const aiMessageId = (Date.now() + 1).toString();
    const aiMessage: Message = {
      id: aiMessageId,
      content: '',
      type: MessageType.AI,
      timestamp: new Date(),
      isStreaming: true
    };
    
    this.messages.update(messages => [...messages, aiMessage]);
    this.isLoading.set(true);
    
    const userContent = messageText;
    this.newMessage.set(''); // 清空输入框
    
    if (this.useRag()) {
      // 使用RAG进行回答
      this.chatService.generateStreamRag(
        this.selectedLlm(),
        this.selectedModel(),
        this.selectedRagTag(),
        userContent
      ).subscribe({
        next: (chunk) => {
          if (chunk) { // 确保chunk不是undefined或空
            // 检查是否是思考过程的标记
            if (chunk === '__THINKING_START__') {
              // 更新消息状态为思考中
              this.messages.update(messages => {
                const updatedMessages = [...messages];
                const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
                if (aiMessageIndex !== -1) {
                  updatedMessages[aiMessageIndex] = {
                    ...updatedMessages[aiMessageIndex],
                    isThinking: true
                  };
                }
                return updatedMessages;
              });
              return;
            }
            
            // 使用函数式更新方式，减少不必要的重新渲染
            this.messages.update(messages => {
              const updatedMessages = [...messages];
              const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
              if (aiMessageIndex !== -1) {
                // 创建新的消息对象，但保持引用稳定性
                updatedMessages[aiMessageIndex] = {
                  ...updatedMessages[aiMessageIndex],
                  content: updatedMessages[aiMessageIndex].content + chunk,
                  isThinking: false // 收到实际内容，不再是思考状态
                };
              }
              return updatedMessages;
            });
          }
        },
        error: (error) => {
          console.error('生成回答出错', error);
          this.isLoading.set(false);
          
          // 检查是否是真正的错误，而不是被取消的请求
          if (error && error.type !== 'error') {
            return; // 不是真正的错误，可能是EventSource被正常关闭
          }
          
          this.messages.update(messages => {
            const updatedMessages = [...messages];
            const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
            if (aiMessageIndex !== -1) {
              updatedMessages[aiMessageIndex] = {
                ...updatedMessages[aiMessageIndex],
                content: '抱歉，生成回答时出现错误。',
                isStreaming: false,
                isThinking: false, // 确保在错误时重置思考状态
                error: true
              };
            }
            return updatedMessages;
          });
        },
        complete: () => {
          this.isLoading.set(false);
          this.messages.update(messages => {
            const updatedMessages = [...messages];
            const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
            if (aiMessageIndex !== -1) {
              updatedMessages[aiMessageIndex] = {
                ...updatedMessages[aiMessageIndex],
                isStreaming: false,
                isThinking: false // 确保在完成时重置思考状态
              };
            }
            return updatedMessages;
          });
        }
      });
    } else {
      // 使用普通流式生成
      this.chatService.generateStream(
        this.selectedLlm(),
        this.selectedModel(),
        userContent
      ).subscribe({
        next: (chunk) => {
          if (chunk) { // 确保chunk不是undefined或空
            // 检查是否是思考过程的标记
            if (chunk === '__THINKING_START__') {
              // 更新消息状态为思考中
              this.messages.update(messages => {
                const updatedMessages = [...messages];
                const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
                if (aiMessageIndex !== -1) {
                  updatedMessages[aiMessageIndex] = {
                    ...updatedMessages[aiMessageIndex],
                    isThinking: true
                  };
                }
                return updatedMessages;
              });
              return;
            }
            
            // 使用函数式更新方式，减少不必要的重新渲染
            this.messages.update(messages => {
              const updatedMessages = [...messages];
              const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
              if (aiMessageIndex !== -1) {
                // 创建新的消息对象，但保持引用稳定性
                updatedMessages[aiMessageIndex] = {
                  ...updatedMessages[aiMessageIndex],
                  content: updatedMessages[aiMessageIndex].content + chunk,
                  isThinking: false // 收到实际内容，不再是思考状态
                };
              }
              return updatedMessages;
            });
          }
        },
        error: (error) => {
          console.error('生成回答出错', error);
          this.isLoading.set(false);
          
          // 检查是否是真正的错误，而不是被取消的请求
          if (error && error.type !== 'error') {
            return; // 不是真正的错误，可能是EventSource被正常关闭
          }
          
          this.messages.update(messages => {
            const updatedMessages = [...messages];
            const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
            if (aiMessageIndex !== -1) {
              updatedMessages[aiMessageIndex] = {
                ...updatedMessages[aiMessageIndex],
                content: '抱歉，生成回答时出现错误。',
                isStreaming: false,
                isThinking: false, // 确保在错误时重置思考状态
                error: true
              };
            }
            return updatedMessages;
          });
        },
        complete: () => {
          this.isLoading.set(false);
          this.messages.update(messages => {
            const updatedMessages = [...messages];
            const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
            if (aiMessageIndex !== -1) {
              updatedMessages[aiMessageIndex] = {
                ...updatedMessages[aiMessageIndex],
                isStreaming: false,
                isThinking: false // 确保在完成时重置思考状态
              };
            }
            return updatedMessages;
          });
        }
      });
    }
  }
  
  scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
    } catch (err) {
      console.error('滚动到底部出错', err);
    }
  }
  
  // 用于ngFor的trackBy函数，通过消息ID跟踪消息
  trackById(index: number, message: Message): string {
    return message.id;
  }
  
  clearChat(): void {
    this.messages.set([{
      id: Date.now().toString(),
      content: '聊天已清空。有什么可以帮助你的吗？',
      type: MessageType.AI,
      timestamp: new Date()
    }]);
  }

  updateLlm(value: string): void {
    this.selectedLlm.set(value);
  }

  updateModel(value: string): void {
    this.selectedModel.set(value);
  }

  updateRagTag(value: string): void {
    this.selectedRagTag.set(value);
  }

  toggleRag(value: boolean): void {
    this.useRag.set(value);
  }

  updateNewMessage(value: string): void {
    this.newMessage.set(value);
  }
  
  handleEnterKey(event: Event): void {
    // 检查是否是键盘事件
    if (event instanceof KeyboardEvent) {
      // 检查是否是Enter键
      if (event.key === 'Enter' && !event.shiftKey) {
        this.sendMessage();
        event.preventDefault();
      }
    }
  }
}