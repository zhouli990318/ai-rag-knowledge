import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatSliderModule } from '@angular/material/slider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ThemeService, CyberTheme } from '../../services/theme.service';

@Component({
  selector: 'app-theme-switcher',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatMenuModule,
    MatIconModule,
    MatSliderModule,
    MatSlideToggleModule
  ],
  template: `
    <button 
      mat-icon-button 
      [matMenuTriggerFor]="themeMenu"
      class="cyber-button ghost theme-switcher-btn">
      <mat-icon>palette</mat-icon>
    </button>

    <mat-menu #themeMenu="matMenu" class="cyber-menu">
      <div class="theme-menu-content" (click)="$event.stopPropagation()">
        <h3 class="menu-title">主题设置</h3>
        
        <div class="theme-section">
          <h4>配色方案</h4>
          <div class="theme-options">
            <button 
              *ngFor="let theme of availableThemes"
              mat-button
              class="theme-option"
              [class.active]="currentTheme.name === theme.name"
              (click)="selectTheme(theme)">
              <div class="theme-preview">
                <div 
                  class="color-dot primary" 
                  [style.background-color]="theme.primaryColor">
                </div>
                <div 
                  class="color-dot accent" 
                  [style.background-color]="theme.accentColor">
                </div>
              </div>
              <span>{{ theme.name }}</span>
            </button>
          </div>
        </div>

        <div class="theme-section">
          <h4>发光强度</h4>
          <input 
            type="range"
            class="glow-slider cyber-input"
            [min]="0.5" 
            [max]="2" 
            [step]="0.1"
            [value]="glowIntensity"
            (input)="onGlowChange($event)">
        </div>

        <div class="theme-section">
          <label class="toggle-switch">
            <input 
              type="checkbox"
              [checked]="animationsEnabled"
              (change)="onAnimationToggle($event)">
            <span class="toggle-slider"></span>
            <span class="toggle-label">动画效果</span>
          </label>
        </div>
      </div>
    </mat-menu>
  `,
  styles: [`
    .theme-switcher-btn {
      position: relative;
      
      &::before {
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        width: 100%;
        height: 100%;
        background: var(--gradient-primary);
        border-radius: 50%;
        transform: translate(-50%, -50%);
        opacity: 0;
        transition: opacity var(--transition-normal) ease;
        z-index: -1;
      }
      
      &:hover::before {
        opacity: 0.2;
      }
    }

    .theme-menu-content {
      padding: var(--spacing-lg);
      min-width: 280px;
      background: var(--gradient-card);
      border: 1px solid var(--border-primary);
      border-radius: var(--radius-lg);
    }

    .menu-title {
      margin: 0 0 var(--spacing-lg) 0;
      color: var(--neon-blue);
      font-size: 18px;
      font-weight: 600;
      text-align: center;
    }

    .theme-section {
      margin-bottom: var(--spacing-lg);
      
      h4 {
        margin: 0 0 var(--spacing-md) 0;
        color: var(--text-secondary);
        font-size: 14px;
        font-weight: 500;
        text-transform: uppercase;
        letter-spacing: 1px;
      }
    }

    .theme-options {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-sm);
    }

    .theme-option {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
      padding: var(--spacing-sm) var(--spacing-md);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      background: transparent;
      color: var(--text-primary);
      transition: all var(--transition-normal) ease;
      cursor: pointer;
      
      &:hover {
        border-color: var(--border-primary);
        background: rgba(0, 212, 255, 0.1);
      }
      
      &.active {
        border-color: var(--neon-blue);
        background: rgba(0, 212, 255, 0.2);
        box-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
      }
    }

    .theme-preview {
      display: flex;
      gap: 4px;
    }

    .color-dot {
      width: 16px;
      height: 16px;
      border-radius: 50%;
      border: 2px solid rgba(255, 255, 255, 0.2);
      
      &.primary {
        box-shadow: 0 0 8px currentColor;
      }
      
      &.accent {
        box-shadow: 0 0 6px currentColor;
      }
    }

    .glow-slider {
      width: 100%;
      
      ::ng-deep {
        .mat-mdc-slider-track-active {
          background: var(--gradient-primary) !important;
        }
        
        .mat-mdc-slider-thumb {
          background: var(--neon-blue) !important;
          border-color: var(--neon-blue) !important;
          box-shadow: 0 0 10px rgba(0, 212, 255, 0.5) !important;
        }
      }
    }

    .glow-slider {
      width: 100%;
      height: 6px;
      border-radius: 3px;
      background: var(--border-subtle);
      outline: none;
      -webkit-appearance: none;
      
      &::-webkit-slider-thumb {
        -webkit-appearance: none;
        width: 20px;
        height: 20px;
        border-radius: 50%;
        background: var(--gradient-primary);
        cursor: pointer;
        box-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
        border: 2px solid var(--neon-blue);
      }
      
      &::-moz-range-thumb {
        width: 20px;
        height: 20px;
        border-radius: 50%;
        background: var(--gradient-primary);
        cursor: pointer;
        box-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
        border: 2px solid var(--neon-blue);
      }
    }

    .toggle-switch {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
      cursor: pointer;
      
      input[type="checkbox"] {
        display: none;
      }
      
      .toggle-slider {
        position: relative;
        width: 50px;
        height: 24px;
        background: var(--border-subtle);
        border-radius: 12px;
        transition: background var(--transition-normal) ease;
        
        &::before {
          content: '';
          position: absolute;
          top: 2px;
          left: 2px;
          width: 20px;
          height: 20px;
          background: var(--text-primary);
          border-radius: 50%;
          transition: transform var(--transition-normal) ease;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
        }
      }
      
      input:checked + .toggle-slider {
        background: var(--neon-blue);
        box-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
        
        &::before {
          transform: translateX(26px);
          box-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
        }
      }
      
      .toggle-label {
        color: var(--text-primary);
        font-weight: 500;
        user-select: none;
      }
    }
  `]
})
export class ThemeSwitcherComponent implements OnInit {
  availableThemes: CyberTheme[] = [];
  currentTheme: CyberTheme = {} as CyberTheme;
  glowIntensity = 1;
  animationsEnabled = true;

  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    this.availableThemes = this.themeService.getAvailableThemes();
    this.themeService.currentTheme$.subscribe(theme => {
      this.currentTheme = theme;
    });
  }

  selectTheme(theme: CyberTheme): void {
    const themeName = Object.keys(this.themeService['themes'])
      .find(key => this.themeService['themes'][key].name === theme.name);
    
    if (themeName) {
      this.themeService.setTheme(themeName);
    }
  }

  onGlowChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const value = parseFloat(target.value);
    this.glowIntensity = value;
    this.themeService.setGlowIntensity(value);
  }

  onAnimationToggle(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.animationsEnabled = target.checked;
    this.themeService.toggleAnimation(target.checked);
  }
}