import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ThemeService } from './services/theme.service';
import { ThemeSwitcherComponent } from './components/ui/theme-switcher.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, 
    RouterLink, 
    RouterLinkActive, 
    CommonModule,
    ThemeSwitcherComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  title = 'Spring AI RAG 知识库问答系统';

  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    // 初始化主题系统
    this.themeService.setTheme('cyberpunk');
  }
}
