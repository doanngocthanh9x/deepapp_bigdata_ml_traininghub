import { useLocation, Link, useNavigate } from 'react-router-dom';
import { ChevronRight, Bell, Search, LogOut, User, Settings } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useSidebarStore } from '@/stores/sidebarStore';
import { getModuleByPath } from '@/utils/aaModuleLoader';
import { auth } from '@/utils/auth';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useToast } from '@/hooks/use-toast';

interface BreadcrumbItem {
  label: string;
  path?: string;
}

const getBreadcrumbs = (pathname: string): BreadcrumbItem[] => {
  if (pathname === '/' || pathname === '/dashboard') {
    return [{ label: 'Dashboard' }];
  }

  const crumbs: BreadcrumbItem[] = [{ label: 'Dashboard', path: '/dashboard' }];

  if (pathname.startsWith('/modules/')) {
    const module = getModuleByPath(pathname);
    if (module) {
      crumbs.push({ label: 'AA Modules', path: '/modules' });
      crumbs.push({ label: module.code });
    }
  } else {
    const segments = pathname.split('/').filter(Boolean);
    segments.forEach((segment, index) => {
      const path = '/' + segments.slice(0, index + 1).join('/');
      const label = segment.charAt(0).toUpperCase() + segment.slice(1);
      if (index < segments.length - 1) {
        crumbs.push({ label, path });
      } else {
        crumbs.push({ label });
      }
    });
  }

  return crumbs;
};

export const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { isCollapsed } = useSidebarStore();
  const breadcrumbs = getBreadcrumbs(location.pathname);
  const user = auth.getUser();

  const handleLogout = () => {
    auth.logout();
    toast({
      title: 'Đăng xuất thành công',
      description: 'Hẹn gặp lại bạn!',
    });
    navigate('/');
  };

  return (
    <header
      className={cn(
        'sticky top-0 z-30 h-14 bg-background/95 backdrop-blur border-b border-border',
        'flex items-center justify-between px-6 transition-all duration-300',
        isCollapsed ? 'lg:pl-24' : 'lg:pl-72'
      )}
    >
      {/* Breadcrumb */}
      <nav className="flex items-center gap-1.5 text-sm">
        {breadcrumbs.map((crumb, index) => (
          <div key={index} className="flex items-center gap-1.5">
            {index > 0 && <ChevronRight className="w-4 h-4 text-muted-foreground" />}
            {crumb.path ? (
              <Link
                to={crumb.path}
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                {crumb.label}
              </Link>
            ) : (
              <span className="text-foreground font-medium">{crumb.label}</span>
            )}
          </div>
        ))}
      </nav>

      {/* Actions */}
      <div className="flex items-center gap-3">
        <div className="relative hidden md:block">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Tìm kiếm..."
            className="w-64 h-9 pl-10 pr-4 rounded-lg bg-muted/50 border border-border text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
          />
        </div>

        {/* Notifications */}
        <Link
          to="/notifications"
          className="relative p-2 rounded-lg hover:bg-muted transition-colors"
        >
          <Bell className="w-5 h-5 text-muted-foreground" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-primary rounded-full" />
        </Link>

        {/* User Menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="w-8 h-8 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center hover:opacity-90 transition-opacity focus:outline-none focus:ring-2 focus:ring-primary/20">
              <span className="text-primary-foreground text-sm font-medium">
                {user?.name?.charAt(0) || 'A'}
              </span>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <div className="px-2 py-1.5">
              <p className="text-sm font-medium text-foreground">{user?.name || 'Admin User'}</p>
              <p className="text-xs text-muted-foreground">{user?.email || 'admin@swift.dev'}</p>
            </div>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link to="/profile" className="flex items-center cursor-pointer">
                <User className="w-4 h-4 mr-2" />
                Hồ sơ cá nhân
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <Link to="/settings" className="flex items-center cursor-pointer">
                <Settings className="w-4 h-4 mr-2" />
                Cài đặt
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout} className="text-destructive cursor-pointer">
              <LogOut className="w-4 h-4 mr-2" />
              Đăng xuất
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
};

export default Header;
