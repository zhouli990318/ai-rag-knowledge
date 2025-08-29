import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked, signal, computed, inject } from '@angular/core';
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
  
  private chatService = inject(ChatService);
  
  messages = signal<Message[]>([]);
  newMessage = signal<string>('');
  isLoading = signal<boolean>(false);
  selectedLlm = signal<string>('ollama');
  selectedModel = signal<string>('deepseek-r1:8b');
  selectedRagTag = signal<string>('');
  ragTags = signal<string[]>([]);
  useRag = signal<boolean>(false);
  lastUserMessage = signal<string>(''); // 存储最后一条用户消息，用于重试

  // 计算属性
  canSendMessage = computed(() => !this.isLoading() && this.newMessage().trim().length > 0);
  
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
        if (response?.data) {
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
    
    this.lastUserMessage.set(messageText); // 保存用户消息用于重试
    this.processMessage(messageText);
  }
  
  retryLastMessage(): void {
    const lastMessage = this.lastUserMessage();
    if (lastMessage) {
      // 移除最后一条AI错误消息
      this.messages.update(messages => {
        const updatedMessages = [...messages];
        if (updatedMessages.length > 0 && updatedMessages[updatedMessages.length - 1].type === MessageType.AI) {
          updatedMessages.pop();
        }
        return updatedMessages;
      });
      
      this.processMessage(lastMessage);
    }
  }
  
  private processMessage(messageText: string): void {
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
    
    const updateAiMessage = (updater: (message: Message) => Message) => {
      this.messages.update(messages => {
        const updatedMessages = [...messages];
        const aiMessageIndex = updatedMessages.findIndex(m => m.id === aiMessageId);
        if (aiMessageIndex !== -1) {
          updatedMessages[aiMessageIndex] = updater(updatedMessages[aiMessageIndex]);
        }
        return updatedMessages;
      });
    };
    
    const streamObservable = this.useRag() 
      ? this.chatService.generateStreamRag(
          this.selectedLlm(),
          this.selectedModel(),
          this.selectedRagTag(),
          userContent
        )
      : this.chatService.generateStream(
          this.selectedLlm(),
          this.selectedModel(),
          userContent
        );
    
    streamObservable.subscribe({
      next: (chunk: string) => {
        if (!chunk) return;
        
        if (chunk === '__THINKING_START__') {
          updateAiMessage(msg => ({ ...msg, isThinking: true }));
          return;
        }
        
        updateAiMessage(msg => ({
          ...msg,
          content: msg.content + chunk,
          isThinking: false
        }));
      },
      error: (error: any) => {
        console.error('生成回答出错', error);
        this.isLoading.set(false);
        
        // 检查当前AI消息是否已经有内容
        const currentMessages = this.messages();
        const lastAiMessage = currentMessages[currentMessages.length - 1];
        const hasContent = lastAiMessage && lastAiMessage.content && lastAiMessage.content.trim().length > 0;
        
        // 如果已经有内容，说明接口正常工作，不显示错误
        if (hasContent) {
          updateAiMessage(msg => ({
            ...msg,
            isStreaming: false,
            isThinking: false,
            error: false
          }));
          return;
        }
        
        // 只有在没有内容时才显示错误消息
        let errorMessage = '抱歉，生成回答时出现错误。请稍后重试。';
        
        if (error && error.message) {
          if (error.message.includes('连接')) {
            errorMessage = '无法连接到服务器，请检查网络连接。';
          } else if (error.message.includes('超时')) {
            errorMessage = '请求超时，请稍后重试。';
          } else if (error.message.includes('404')) {
            errorMessage = '服务接口不存在，请检查服务器配置。';
          } else if (error.message.includes('500')) {
            errorMessage = '服务器内部错误，请稍后重试。';
          }
        }
        
        updateAiMessage(msg => ({
          ...msg,
          content: errorMessage,
          isStreaming: false,
          isThinking: false,
          error: true
        }));
      },
      complete: () => {
        this.isLoading.set(false);
        updateAiMessage(msg => ({
          ...msg,
          isStreaming: false,
          isThinking: false
        }));
      }
    });
  }
  
  scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
    } catch (err) {
      console.error('滚动到底部失败', err);
    }
  }
  
  clearChat(): void {
    this.messages.set([]);
    this.lastUserMessage.set(''); // 清空重试消息
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
  
  handleEnterKey(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      if (this.canSendMessage()) {
        this.sendMessage();
      }
    }
  }
}