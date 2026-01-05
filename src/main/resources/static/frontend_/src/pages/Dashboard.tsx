import { useMemo } from 'react';
import { Package, CheckCircle2, Clock, AlertCircle } from 'lucide-react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { FeatureList } from '@/components/FeatureList';
import { StatsCard } from '@/components/StatsCard';
import { Feature } from '@/components/FeatureCard';
import featuresData from '@/mock/features.json';

const Dashboard = () => {
  const features = featuresData.features as Feature[];
  const stats = featuresData.stats;

  const groupedFeatures = useMemo(() => {
    return {
      mustHave: features.filter((f) => f.priority === 'must-have'),
      shouldHave: features.filter((f) => f.priority === 'should-have'),
      couldHave: features.filter((f) => f.priority === 'could-have'),
    };
  }, [features]);

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        {/* Page Title */}
        <div className="animate-fade-in">
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-muted-foreground mt-1">
            Overview of features and project progress
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatsCard
            title="Total Features"
            value={stats.total}
            subtitle="Across all modules"
            icon={Package}
            style={{ animationDelay: '0ms' }}
          />
          <StatsCard
            title="Completed"
            value={stats.completed}
            subtitle={`${Math.round((stats.completed / stats.total) * 100)}% done`}
            icon={CheckCircle2}
            trend={{ value: 12, isPositive: true }}
            style={{ animationDelay: '50ms' }}
          />
          <StatsCard
            title="In Progress"
            value={stats.inProgress}
            subtitle="Active development"
            icon={Clock}
            style={{ animationDelay: '100ms' }}
          />
          <StatsCard
            title="Planned"
            value={stats.planned}
            subtitle="Upcoming work"
            icon={AlertCircle}
            style={{ animationDelay: '150ms' }}
          />
        </div>

        {/* Feature Lists by Priority */}
        <FeatureList
          title="Must Have"
          description="Core features essential for MVP"
          features={groupedFeatures.mustHave}
        />

        <FeatureList
          title="Should Have"
          description="Important features for better user experience"
          features={groupedFeatures.shouldHave}
        />

        <FeatureList
          title="Could Have"
          description="Nice-to-have features for future iterations"
          features={groupedFeatures.couldHave}
        />
      </div>
    </DashboardLayout>
  );
};

export default Dashboard;
