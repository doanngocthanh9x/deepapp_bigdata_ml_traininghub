import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface SidebarState {
  isCollapsed: boolean;
  isAAModulesOpen: boolean;
  setCollapsed: (collapsed: boolean) => void;
  toggleCollapsed: () => void;
  setAAModulesOpen: (open: boolean) => void;
  toggleAAModules: () => void;
}

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set, get) => ({
      isCollapsed: false,
      isAAModulesOpen: true,
      setCollapsed: (collapsed) => set({ isCollapsed: collapsed }),
      toggleCollapsed: () => set({ isCollapsed: !get().isCollapsed }),
      setAAModulesOpen: (open) => set({ isAAModulesOpen: open }),
      toggleAAModules: () => set({ isAAModulesOpen: !get().isAAModulesOpen }),
    }),
    {
      name: 'swift-dashboard-sidebar',
    }
  )
);
