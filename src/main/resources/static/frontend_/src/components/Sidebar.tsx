import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { useSidebarStore } from '@/stores/sidebarStore';
import { useThemeStore } from '@/stores/themeStore';
import {
  loadAAModulePaths,
  isLeaf,
  type AAModuleNode,
  type AAModuleLeaf,
} from '@/utils/aaModuleLoader';
import {
  LayoutDashboard,
  Kanban,
  Map,
  Settings,
  ChevronDown,
  ChevronRight,
  Sun,
  Moon,
  Menu,
  X,
  Shield,
  ScanText,
  FileSearch,
  Folder,
  FolderOpen,
  Layers,
  Box,
  Target,
  Bell,
  Users,
  Languages,
} from 'lucide-react';

interface NavItem {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  path: string;
}

const fixedNavItems: NavItem[] = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/dashboard' },
  { icon: Kanban, label: 'Kanban', path: '/kanban' },
  { icon: Map, label: 'Roadmap', path: '/roadmap' },
  { icon: Settings, label: 'Settings', path: '/settings' },
];

const userNavItems: NavItem[] = [
  { icon: Bell, label: 'Notifications', path: '/notifications' },
  { icon: Users, label: 'User Management', path: '/users' },
  { icon: Languages, label: 'i18n Demo', path: '/i18n-demo' },
];

const iconMap: Record<string, React.ComponentType<{ className?: string }>> = {
  Shield,
  ScanText,
  FileSearch,
  Folder,
  FolderOpen,
  Layers,
  Box,
  Target,
};

const getIcon = (iconName?: string) => iconMap[iconName || ''] || Folder;

export const Sidebar = () => {
  const location = useLocation();
  const { isCollapsed, toggleCollapsed } = useSidebarStore();
  const { theme, toggleTheme } = useThemeStore();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set(['AA', 'A0']));

  const aaModuleTree = loadAAModulePaths();

  const isActive = (path: string) => location.pathname === path || (path === '/dashboard' && location.pathname === '/');
  const isModuleActive = (path: string) => location.pathname === path;

  const toggleNode = (nodeName: string) => {
    setExpandedNodes((prev) => {
      const next = new Set(prev);
      if (next.has(nodeName)) {
        next.delete(nodeName);
      } else {
        next.add(nodeName);
      }
      return next;
    });
  };

  const NavLink = ({ item, collapsed }: { item: NavItem; collapsed: boolean }) => (
    <Link
      to={item.path}
      className={cn(
        'group flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 relative',
        'hover:bg-sidebar-accent',
        isActive(item.path)
          ? 'bg-sidebar-accent text-sidebar-primary-foreground'
          : 'text-sidebar-foreground/70 hover:text-sidebar-foreground'
      )}
    >
      <item.icon className={cn('w-5 h-5 shrink-0', isActive(item.path) && 'text-primary')} />
      {!collapsed && <span className="text-sm font-medium truncate">{item.label}</span>}
      {isActive(item.path) && (
        <div className="absolute left-0 w-1 h-6 bg-primary rounded-r-full" />
      )}
    </Link>
  );

  // Recursive component for nested modules
  const ModuleTreeNode = ({
    node,
    depth = 0,
    collapsed,
    parentPath = '',
  }: {
    node: AAModuleNode | AAModuleLeaf;
    depth?: number;
    collapsed: boolean;
    parentPath?: string;
  }) => {
    if (isLeaf(node)) {
      // Leaf node - clickable link
      const Icon = getIcon(node.icon);
      const active = isModuleActive(node.path);

      return (
        <Link
          to={node.path}
          className={cn(
            'group flex items-center gap-3 py-2 px-3 rounded-lg transition-all duration-200',
            'hover:bg-sidebar-accent',
            active
              ? 'bg-sidebar-accent text-sidebar-primary-foreground'
              : 'text-sidebar-foreground/60 hover:text-sidebar-foreground',
            !collapsed && depth > 0 && 'ml-3'
          )}
          style={{ paddingLeft: collapsed ? undefined : `${12 + depth * 12}px` }}
        >
          <Icon className={cn('w-4 h-4 shrink-0', active && 'text-primary')} />
          {!collapsed && (
            <div className="flex-1 min-w-0 flex items-center justify-between gap-2">
              <div className="min-w-0">
                <span className="text-sm font-medium truncate block">{node.name}</span>
                <span className="text-[10px] text-sidebar-muted truncate block">{node.code}</span>
              </div>
              <span
                className={cn(
                  'text-[10px] px-1.5 py-0.5 rounded-full font-medium shrink-0',
                  node.status === 'active' && 'bg-success/20 text-success',
                  node.status === 'development' && 'bg-warning/20 text-warning',
                  node.status === 'planned' && 'bg-muted text-muted-foreground'
                )}
              >
                {node.status === 'active' ? '●' : node.status === 'development' ? '◐' : '○'}
              </span>
            </div>
          )}
        </Link>
      );
    }

    // Branch node - expandable folder
    const nodePath = parentPath ? `${parentPath}/${node.name}` : node.name;
    const isExpanded = expandedNodes.has(node.name);
    const Icon = getIcon(isExpanded ? 'FolderOpen' : node.icon);

    return (
      <div className="animate-fade-in">
        <button
          onClick={() => toggleNode(node.name)}
          className={cn(
            'w-full flex items-center gap-2 py-2 px-3 rounded-lg transition-all duration-200',
            'hover:bg-sidebar-accent text-sidebar-foreground/70 hover:text-sidebar-foreground',
            !collapsed && depth > 0 && 'ml-3'
          )}
          style={{ paddingLeft: collapsed ? undefined : `${12 + depth * 12}px` }}
        >
          {isExpanded ? (
            <ChevronDown className="w-3.5 h-3.5 shrink-0 text-sidebar-muted" />
          ) : (
            <ChevronRight className="w-3.5 h-3.5 shrink-0 text-sidebar-muted" />
          )}
          <Icon className="w-4 h-4 shrink-0" />
          {!collapsed && (
            <span className="text-sm font-semibold uppercase tracking-wider">{node.name}</span>
          )}
        </button>

        {isExpanded && node.children && (
          <div className="mt-1 space-y-0.5">
            {node.children.map((child, index) => (
              <ModuleTreeNode
                key={isLeaf(child) ? child.path : `${nodePath}-${child.name}-${index}`}
                node={child}
                depth={depth + 1}
                collapsed={collapsed}
                parentPath={nodePath}
              />
            ))}
          </div>
        )}
      </div>
    );
  };

  const SidebarContent = ({ collapsed }: { collapsed: boolean }) => (
    <div className="flex flex-col h-full">
      {/* Logo */}
      <div
        className={cn(
          'flex items-center gap-3 px-4 py-5 border-b border-sidebar-border',
          collapsed && 'justify-center px-2'
        )}
      >
        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-accent flex items-center justify-center shrink-0">
          <span className="text-primary-foreground font-bold text-sm">S</span>
        </div>
        {!collapsed && (
          <div className="min-w-0">
            <h1 className="text-sidebar-foreground font-semibold text-base truncate">
              Swift Dashboard
            </h1>
            <p className="text-sidebar-muted text-xs truncate">OCR Processing</p>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 overflow-y-auto scrollbar-thin">
        <div className="space-y-1">
          {fixedNavItems.map((item) => (
            <NavLink key={item.path} item={item} collapsed={collapsed} />
          ))}
        </div>

        {/* User Section */}
        <div className="mt-6">
          <div
            className={cn(
              'px-3 mb-2 text-[10px] font-semibold uppercase tracking-widest text-sidebar-muted',
              collapsed && 'text-center'
            )}
          >
            {collapsed ? '•' : 'User'}
          </div>
          <div className="space-y-1">
            {userNavItems.map((item) => (
              <NavLink key={item.path} item={item} collapsed={collapsed} />
            ))}
          </div>
        </div>

        {/* AA Modules Tree */}
        <div className="mt-6">
          <div
            className={cn(
              'px-3 mb-2 text-[10px] font-semibold uppercase tracking-widest text-sidebar-muted',
              collapsed && 'text-center'
            )}
          >
            {collapsed ? '•••' : 'Modules'}
          </div>

          <div className="space-y-0.5">
            {aaModuleTree.map((node, index) => (
              <ModuleTreeNode
                key={`root-${node.name}-${index}`}
                node={node}
                collapsed={collapsed}
              />
            ))}
          </div>
        </div>
      </nav>

      {/* Footer */}
      <div className={cn('px-3 py-4 border-t border-sidebar-border', collapsed && 'px-2')}>
        <div className={cn('flex items-center', collapsed ? 'flex-col gap-2' : 'justify-between')}>
          <button
            onClick={toggleTheme}
            className="p-2 rounded-lg hover:bg-sidebar-accent text-sidebar-foreground/70 hover:text-sidebar-foreground transition-colors"
            title={theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}
          >
            {theme === 'light' ? <Moon className="w-5 h-5" /> : <Sun className="w-5 h-5" />}
          </button>
          <button
            onClick={toggleCollapsed}
            className="p-2 rounded-lg hover:bg-sidebar-accent text-sidebar-foreground/70 hover:text-sidebar-foreground transition-colors hidden lg:block"
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            <Menu className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );

  return (
    <>
      {/* Mobile Toggle */}
      <button
        onClick={() => setMobileOpen(!mobileOpen)}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 rounded-lg bg-card border border-border shadow-md"
      >
        {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
      </button>

      {/* Mobile Overlay */}
      {mobileOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-background/80 backdrop-blur-sm z-40"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed left-0 top-0 h-screen bg-sidebar border-r border-sidebar-border z-40 transition-all duration-300',
          'lg:translate-x-0',
          isCollapsed ? 'w-16' : 'w-64',
          mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        )}
        style={{ background: 'var(--gradient-sidebar)' }}
      >
        <SidebarContent collapsed={isCollapsed} />
      </aside>
    </>
  );
};

export default Sidebar;
