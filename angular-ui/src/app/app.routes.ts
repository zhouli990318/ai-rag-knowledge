import { Routes } from '@angular/router';

export const routes: Routes = [
  { 
    path: '', 
    loadComponent: () => import('./pages/chat/chat.component').then(m => m.ChatComponent) 
  },
  { 
    path: 'rag', 
    loadComponent: () => import('./pages/rag/rag.component').then(m => m.RagComponent) 
  },
  { 
    path: '**', 
    redirectTo: '' 
  }
];
