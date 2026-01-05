import { DashboardLayout } from '@/components/DashboardLayout';
import { Shield, Users, Key, Activity, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StatsCard } from '@/components/StatsCard';

const AuthWorkerPage = () => {
  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        <div className="animate-fade-in">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 rounded-lg bg-primary/10">
              <Shield className="w-6 h-6 text-primary" />
            </div>
            <div>
              <span className="text-xs font-mono text-muted-foreground">AAA0_0100</span>
              <h1 className="text-2xl font-bold text-foreground">Auth & Worker</h1>
            </div>
          </div>
          <p className="text-muted-foreground">
            Authentication management and background worker processing
          </p>
        </div>

        {/* Stats */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatsCard
            title="Active Users"
            value="1,234"
            subtitle="Currently online"
            icon={Users}
            trend={{ value: 8, isPositive: true }}
          />
          <StatsCard
            title="API Keys"
            value="56"
            subtitle="Active keys"
            icon={Key}
          />
          <StatsCard
            title="Worker Jobs"
            value="89"
            subtitle="In queue"
            icon={Activity}
            trend={{ value: 15, isPositive: false }}
          />
          <StatsCard
            title="Processing"
            value="12"
            subtitle="Active workers"
            icon={RefreshCw}
          />
        </div>

        {/* Actions */}
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="bg-card border border-border rounded-xl p-6 animate-fade-in">
            <h3 className="font-semibold text-card-foreground mb-4">Authentication</h3>
            <div className="space-y-3">
              <Button variant="outline" className="w-full justify-start gap-3">
                <Users className="w-4 h-4" />
                Manage Users
              </Button>
              <Button variant="outline" className="w-full justify-start gap-3">
                <Key className="w-4 h-4" />
                Generate API Key
              </Button>
              <Button variant="outline" className="w-full justify-start gap-3">
                <Shield className="w-4 h-4" />
                Security Settings
              </Button>
            </div>
          </div>

          <div className="bg-card border border-border rounded-xl p-6 animate-fade-in">
            <h3 className="font-semibold text-card-foreground mb-4">Worker Queue</h3>
            <div className="space-y-3">
              <Button variant="outline" className="w-full justify-start gap-3">
                <Activity className="w-4 h-4" />
                View Queue
              </Button>
              <Button variant="outline" className="w-full justify-start gap-3">
                <RefreshCw className="w-4 h-4" />
                Restart Workers
              </Button>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default AuthWorkerPage;
