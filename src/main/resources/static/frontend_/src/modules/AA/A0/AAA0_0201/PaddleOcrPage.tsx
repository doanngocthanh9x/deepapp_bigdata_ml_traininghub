import { DashboardLayout } from '@/components/DashboardLayout';
import { FileSearch, Cpu, Languages, Table, Layers } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StatsCard } from '@/components/StatsCard';
import { cn } from '@/lib/utils';

const PaddleOcrPage = () => {
  const features = [
    {
      icon: Languages,
      title: 'Multi-Language',
      description: 'Support for 80+ languages including CJK',
      status: 'active',
    },
    {
      icon: Table,
      title: 'Table Detection',
      description: 'Automatic table structure recognition',
      status: 'development',
    },
    {
      icon: Layers,
      title: 'Layout Analysis',
      description: 'Document layout understanding',
      status: 'development',
    },
  ];

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        <div className="animate-fade-in">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 rounded-lg bg-warning/10">
              <FileSearch className="w-6 h-6 text-warning" />
            </div>
            <div>
              <span className="text-xs font-mono text-muted-foreground">AAA0_0201</span>
              <h1 className="text-2xl font-bold text-foreground">PaddleOCR</h1>
              <span className="inline-block mt-1 text-xs px-2 py-0.5 rounded-full bg-warning/10 text-warning font-medium">
                In Development
              </span>
            </div>
          </div>
          <p className="text-muted-foreground mt-2">
            Advanced OCR powered by PaddlePaddle deep learning framework
          </p>
        </div>

        {/* Stats */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <StatsCard
            title="Accuracy"
            value="99.2%"
            subtitle="On standard benchmarks"
            icon={FileSearch}
          />
          <StatsCard
            title="Languages"
            value="80+"
            subtitle="Supported languages"
            icon={Languages}
          />
          <StatsCard
            title="GPU Acceleration"
            value="Active"
            subtitle="CUDA enabled"
            icon={Cpu}
          />
        </div>

        {/* Features Grid */}
        <div>
          <h3 className="font-semibold text-foreground mb-4">Features</h3>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((feature, index) => (
              <div
                key={feature.title}
                className="bg-card border border-border rounded-xl p-5 animate-fade-in"
                style={{ animationDelay: `${index * 50}ms` }}
              >
                <div className="flex items-start gap-4">
                  <div className="p-2.5 rounded-lg bg-muted">
                    <feature.icon className="w-5 h-5 text-muted-foreground" />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h4 className="font-medium text-card-foreground">{feature.title}</h4>
                      <span
                        className={cn(
                          'text-[10px] px-1.5 py-0.5 rounded-full font-medium',
                          feature.status === 'active'
                            ? 'bg-success/20 text-success'
                            : 'bg-warning/20 text-warning'
                        )}
                      >
                        {feature.status === 'active' ? 'Active' : 'Dev'}
                      </span>
                    </div>
                    <p className="text-sm text-muted-foreground mt-1">{feature.description}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Coming Soon */}
        <div className="bg-gradient-to-r from-primary/5 to-accent/5 border border-primary/20 rounded-xl p-8 text-center animate-fade-in">
          <Cpu className="w-12 h-12 mx-auto mb-4 text-primary" />
          <h3 className="text-xl font-semibold text-foreground mb-2">
            Full Integration Coming Soon
          </h3>
          <p className="text-muted-foreground max-w-md mx-auto">
            PaddleOCR integration is currently in development. Advanced features like table
            extraction and document understanding will be available soon.
          </p>
          <Button className="mt-6" variant="outline">
            Subscribe to Updates
          </Button>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default PaddleOcrPage;
