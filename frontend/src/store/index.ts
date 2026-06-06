import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import api from '@/lib/api';

export interface User {
  id: string;
  email: string;
  name: string;
  credits: number;
  role: 'USER' | 'ADMIN';
  avatarUrl?: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isLoading: false,

      login: async (email, password) => {
        set({ isLoading: true });
        try {
          const { data } = await api.post('/api/auth/login', { email, password });
          localStorage.setItem('pat_token', data.token);
          set({ user: data.user, token: data.token, isLoading: false });
        } catch (err) {
          set({ isLoading: false });
          throw err;
        }
      },

      register: async (name, email, password) => {
        set({ isLoading: true });
        try {
          const { data } = await api.post('/api/auth/register', { name, email, password });
          localStorage.setItem('pat_token', data.token);
          set({ user: data.user, token: data.token, isLoading: false });
        } catch (err) {
          set({ isLoading: false });
          throw err;
        }
      },

      logout: () => {
        localStorage.removeItem('pat_token');
        set({ user: null, token: null });
      },

      refreshUser: async () => {
        try {
          const { data } = await api.get('/api/users/me');
          set({ user: data });
        } catch {
          // silently ignore — no auth required
        }
      },
    }),
    { name: 'presenter-ai-auth', partialize: (s) => ({ user: s.user, token: s.token }) }
  )
);
