import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface CyberTheme {
  name: string;
  primaryColor: string;
  accentColor: string;
  backgroundColor: string;
  surfaceColor: string;
  textColor: string;
}

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'cyber-theme';
  
  private readonly themes: Record<string, CyberTheme> = {
    cyberpunk: {
      name: 'Cyberpunk Blue',
      primaryColor: '#00D4FF',
      accentColor: '#8B5CF6',
      backgroundColor: '#0A0E27',
      surfaceColor: '#1A1F3A',
      textColor: '#FFFFFF'
    },
    neonPink: {
      name: 'Neon Pink',
      primaryColor: '#FF0080',
      accentColor: '#00FFF0',
      backgroundColor: '#0A0A0F',
      surfaceColor: '#1A1A2E',
      textColor: '#FFFFFF'
    },
    matrixGreen: {
      name: 'Matrix Green',
      primaryColor: '#00FF88',
      accentColor: '#00D4FF',
      backgroundColor: '#0A1A0A',
      surfaceColor: '#1A2E1A',
      textColor: '#FFFFFF'
    }
  };

  private currentThemeSubject = new BehaviorSubject<CyberTheme>(this.themes['cyberpunk']);
  public currentTheme$ = this.currentThemeSubject.asObservable();

  constructor() {
    this.loadTheme();
  }

  private loadTheme(): void {
    const savedTheme = localStorage.getItem(this.THEME_KEY);
    if (savedTheme && this.themes[savedTheme]) {
      this.setTheme(savedTheme);
    } else {
      this.setTheme('cyberpunk');
    }
  }

  public setTheme(themeName: string): void {
    const theme = this.themes[themeName];
    if (!theme) return;

    // 更新CSS变量
    const root = document.documentElement;
    root.style.setProperty('--neon-blue', theme.primaryColor);
    root.style.setProperty('--neon-purple', theme.accentColor);
    root.style.setProperty('--primary-bg', theme.backgroundColor);
    root.style.setProperty('--secondary-bg', theme.surfaceColor);
    root.style.setProperty('--text-primary', theme.textColor);

    // 更新渐变色
    root.style.setProperty('--gradient-primary', 
      `linear-gradient(135deg, ${theme.primaryColor}, ${theme.accentColor})`);
    
    // 更新发光效果
    root.style.setProperty('--shadow-glow', 
      `0 0 20px ${this.hexToRgba(theme.primaryColor, 0.3)}`);
    root.style.setProperty('--shadow-glow-purple', 
      `0 0 20px ${this.hexToRgba(theme.accentColor, 0.3)}`);

    // 保存主题选择
    localStorage.setItem(this.THEME_KEY, themeName);
    this.currentThemeSubject.next(theme);
  }

  public getAvailableThemes(): CyberTheme[] {
    return Object.values(this.themes);
  }

  public getCurrentTheme(): CyberTheme {
    return this.currentThemeSubject.value;
  }

  private hexToRgba(hex: string, alpha: number): string {
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }

  public toggleAnimation(enabled: boolean): void {
    const root = document.documentElement;
    if (enabled) {
      root.style.setProperty('--transition-fast', '0.2s');
      root.style.setProperty('--transition-normal', '0.3s');
      root.style.setProperty('--transition-slow', '0.5s');
    } else {
      root.style.setProperty('--transition-fast', '0s');
      root.style.setProperty('--transition-normal', '0s');
      root.style.setProperty('--transition-slow', '0s');
    }
  }

  public setGlowIntensity(intensity: number): void {
    const root = document.documentElement;
    const theme = this.getCurrentTheme();
    
    root.style.setProperty('--shadow-glow', 
      `0 0 ${20 * intensity}px ${this.hexToRgba(theme.primaryColor, 0.3 * intensity)}`);
    root.style.setProperty('--shadow-glow-purple', 
      `0 0 ${20 * intensity}px ${this.hexToRgba(theme.accentColor, 0.3 * intensity)}`);
  }
}