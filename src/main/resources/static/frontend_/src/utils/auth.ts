export interface User {
  id: string;
  email: string;
  name: string;
  role: 'admin' | 'user' | 'viewer';
  avatar?: string;
}

const STORAGE_KEY = 'swift_dashboard_auth';

const mockUser: User = {
  id: '1',
  email: 'admin@swift-dashboard.dev',
  name: 'Admin User',
  role: 'admin',
  avatar: undefined,
};

export const auth = {
  login: async (email: string, password: string): Promise<User> => {
    // Mock login - always succeeds in development
    await new Promise((resolve) => setTimeout(resolve, 500));
    
    if (email && password) {
      const user = { ...mockUser, email };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
      return user;
    }
    throw new Error('Invalid credentials');
  },

  logout: (): void => {
    localStorage.removeItem(STORAGE_KEY);
  },

  getUser: (): User | null => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        return JSON.parse(stored) as User;
      } catch {
        return null;
      }
    }
    return null;
  },

  isAuthenticated: (): boolean => {
    return auth.getUser() !== null;
  },

  // Auto-login for development
  autoLogin: (): User => {
    const existingUser = auth.getUser();
    if (existingUser) return existingUser;
    
    localStorage.setItem(STORAGE_KEY, JSON.stringify(mockUser));
    return mockUser;
  },
};

export default auth;
