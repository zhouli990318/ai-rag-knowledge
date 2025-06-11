import { MatTabsModule } from '@angular/material/tabs';
import { Component, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RagService } from '../../services/rag.service';
import { MatTabGroup } from '@angular/material/tabs';

// 定义 Message 接口
interface Message {
  content: string;
  isUser: boolean;
}

// 定义 Session 接口
interface Session {
  id: string;
  name: string;
  messages: Message[];
}

@Component({
  selector: 'app-rag',
  standalone: true,
  imports: [CommonModule, FormsModule, MatTabsModule],
  templateUrl: './rag.component.html',
  styleUrl: './rag.component.scss'
})
export class RagComponent implements OnInit {
  ragTags = signal<string[]>([]);
  selectedRagTag = signal<string>('');
  newRagTag = signal<string>('');
  files = signal<File[]>([]);
  isUploading = signal<boolean>(false);
  uploadProgress = signal<number>(0);
  uploadSuccess = signal<boolean>(false);
  uploadError = signal<string>('');
  
  // Git仓库分析
  gitRepoUrl = signal<string>('');
  gitUserName = signal<string>('');
  gitToken = signal<string>('');
  isAnalyzing = signal<boolean>(false);
  analyzeSuccess = signal<boolean>(false);
  analyzeError = signal<string>('');
  
  // 聊天相关属性
  newMessage = '';
  
  // 多会话管理
  sessions = signal<Session[]>([
    { 
      id: Date.now().toString(), 
      name: '新会话', 
      messages: [] 
    }
  ]);
  selectedSessionId = signal<string>(this.sessions()[0].id);

  @ViewChild('tabGroup') tabGroup!: MatTabGroup;

  constructor(private ragService: RagService) {}

  ngOnInit(): void {
    this.loadRagTags();
  }

  // 添加新会话
  addSession(): void {
    const newSession: Session = {
      id: Date.now().toString(),
      name: `会话 ${this.sessions().length + 1}`,
      messages: []
    };
    this.sessions.update(sessions => [...sessions, newSession]);
    this.selectedSessionId.set(newSession.id);
  }

  // 删除会话
  removeSession(sessionId: string): void {
    this.sessions.update(sessions => 
      sessions.filter(session => session.id !== sessionId)
    );
    if (this.selectedSessionId() === sessionId && this.sessions().length > 0) {
      this.selectedSessionId.set(this.sessions()[0].id);
    }
  }

  // 切换会话
  selectSession(id: string) {
    this.selectedSessionId.set(id);
  }

  // 切换标签页时更新选中的会话ID
  onTabChanged(event: any): void {
    const selectedSession = this.sessions()[event.index];
    if (selectedSession) {
      this.selectedSessionId.set(selectedSession.id);
    }
  }

  // 发送消息
  sendMessage() {
    if (this.newMessage.trim()) {
      const currentSession = this.sessions().find(
        (session) => session.id === this.selectedSessionId()
      );
      
      if (currentSession) {
        // 添加用户消息
        currentSession.messages.push({
          content: this.newMessage,
          isUser: true
        });
        
        // 清空输入框
        this.newMessage = '';
        
        // 模拟 AI 回复
        setTimeout(() => {
          currentSession.messages.push({
            content: '这是 AI 的回复内容。',
            isUser: false
          });
        }, 500);
      }
    }
  }

  loadRagTags(): void {
    this.ragService.getRagTags().subscribe({
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

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.files.set(Array.from(input.files));
    }
  }

  uploadFiles(): void {
    if (!this.selectedRagTag() || this.files().length === 0) return;
    
    this.isUploading.set(true);
    this.uploadProgress.set(0);
    this.uploadSuccess.set(false);
    this.uploadError.set('');
    
    // 模拟上传进度
    const progressInterval = setInterval(() => {
      if (this.uploadProgress() < 90) {
        this.uploadProgress.update(value => value + 10);
      }
    }, 300);
    
    this.ragService.uploadFiles(this.selectedRagTag(), this.files()).subscribe({
      next: (response) => {
        clearInterval(progressInterval);
        this.uploadProgress.set(100);
        this.uploadSuccess.set(true);
        this.files.set([]);
        setTimeout(() => {
          this.isUploading.set(false);
          this.uploadProgress.set(0);
        }, 1000);
        
        // 重新加载标签列表
        this.loadRagTags();
      },
      error: (error) => {
        clearInterval(progressInterval);
        this.isUploading.set(false);
        this.uploadError.set('上传文件失败，请重试。');
        console.error('上传文件失败', error);
      }
    });
  }

  addRagTag(): void {
    const newTag = this.newRagTag();
    if (!newTag.trim()) return;
    
    const currentTags = this.ragTags();
    if (!currentTags.includes(newTag)) {
      this.ragTags.update(tags => [...tags, newTag]);
      this.selectedRagTag.set(newTag);
      this.newRagTag.set('');
    }
  }

  analyzeGitRepo(): void {
    if (!this.gitRepoUrl() || !this.gitUserName()) return;
    
    this.isAnalyzing.set(true);
    this.analyzeSuccess.set(false);
    this.analyzeError.set('');
    
    this.ragService.analyzeGitRepository(this.gitRepoUrl(), this.gitUserName(), this.gitToken()).subscribe({
      next: (response) => {
        this.isAnalyzing.set(false);
        this.analyzeSuccess.set(true);
        
        // 清空输入
        this.gitRepoUrl.set('');
        this.gitUserName.set('');
        this.gitToken.set('');
        
        // 重新加载标签列表
        this.loadRagTags();
      },
      error: (error) => {
        this.isAnalyzing.set(false);
        this.analyzeError.set('分析Git仓库失败，请检查URL和凭据。');
        console.error('分析Git仓库失败', error);
      }
    });
  }

  updateSelectedRagTag(value: string): void {
    this.selectedRagTag.set(value);
  }

  updateNewRagTag(value: string): void {
    this.newRagTag.set(value);
  }

  updateGitRepoUrl(value: string): void {
    this.gitRepoUrl.set(value);
  }

  updateGitUserName(value: string): void {
    this.gitUserName.set(value);
  }

  updateGitToken(value: string): void {
    this.gitToken.set(value);
  }
}