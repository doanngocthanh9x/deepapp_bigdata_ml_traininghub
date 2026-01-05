import { DashboardLayout } from '@/components/DashboardLayout';
import { Settings as SettingsIcon, User, Bell, Palette, Shield, Database } from 'lucide-react';
import { useThemeStore } from '@/stores/themeStore';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const Settings = () => {
  const { theme, setTheme } = useThemeStore();

  const sections = [
    {
      icon: User,
      title: 'Profile',
      description: 'Manage your account settings',
    },
    {
      icon: Bell,
      title: 'Notifications',
      description: 'Configure notification preferences',
    },
    {
      icon: Shield,
      title: 'Security',
      description: 'Password and authentication settings',
    },
    {
      icon: Database,
      title: 'API Keys',
      description: 'Manage API keys and tokens',
    },
  ];

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-8">
        <div className="animate-fade-in">
          <h1 className="text-2xl font-bold text-foreground">Settings</h1>
          <p className="text-muted-foreground mt-1">
            Configure your dashboard preferences
          </p>
        </div>

        {/* Theme Selector */}
        <div
          className="bg-card border border-border rounded-xl p-6 animate-fade-in"
          style={{ animationDelay: '50ms' }}
        >
          <div className="flex items-start gap-4">
            <div className="p-2.5 rounded-lg bg-primary/10">
              <Palette className="w-5 h-5 text-primary" />
            </div>
            <div className="flex-1">
              <h3 className="font-semibold text-card-foreground">Appearance</h3>
              <p className="text-sm text-muted-foreground mt-1">
                Choose your preferred theme
              </p>
              <div className="flex gap-3 mt-4">
                <button
                  onClick={() => setTheme('light')}
                  className={cn(
                    'flex-1 p-4 rounded-lg border-2 transition-all',
                    theme === 'light'
                      ? 'border-primary bg-primary/5'
                      : 'border-border hover:border-primary/50'
                  )}
                >
                  <div className="w-full h-16 rounded bg-gradient-to-br from-slate-100 to-slate-200 mb-2" />
                  <span className="text-sm font-medium text-card-foreground">Light</span>
                </button>
                <button
                  onClick={() => setTheme('dark')}
                  className={cn(
                    'flex-1 p-4 rounded-lg border-2 transition-all',
                    theme === 'dark'
                      ? 'border-primary bg-primary/5'
                      : 'border-border hover:border-primary/50'
                  )}
                >
                  <div className="w-full h-16 rounded bg-gradient-to-br from-slate-800 to-slate-900 mb-2" />
                  <span className="text-sm font-medium text-card-foreground">Dark</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Other Settings */}
        <div className="space-y-4">
          {sections.map((section, index) => (
            <div
              key={section.title}
              className="bg-card border border-border rounded-xl p-6 hover:shadow-md hover:border-primary/20 transition-all cursor-pointer animate-fade-in"
              style={{ animationDelay: `${(index + 2) * 50}ms` }}
            >
              <div className="flex items-center gap-4">
                <div className="p-2.5 rounded-lg bg-muted">
                  <section.icon className="w-5 h-5 text-muted-foreground" />
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold text-card-foreground">{section.title}</h3>
                  <p className="text-sm text-muted-foreground">{section.description}</p>
                </div>
                <span className="text-muted-foreground">→</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  );
};

export default Settings;
