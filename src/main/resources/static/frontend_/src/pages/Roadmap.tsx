import { DashboardLayout } from '@/components/DashboardLayout';
import { Map, Calendar } from 'lucide-react';
import featuresData from '@/mock/features.json';
import { Feature } from '@/components/FeatureCard';
import { cn } from '@/lib/utils';

const Roadmap = () => {
  const features = featuresData.features as Feature[];
  
  const quarters = [
    { id: 'q1', label: 'Q1 2025', features: features.filter(f => f.status === 'completed') },
    { id: 'q2', label: 'Q2 2025', features: features.filter(f => f.status === 'in-progress') },
    { id: 'q3', label: 'Q3 2025', features: features.filter(f => f.status === 'planned') },
  ];

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-6">
        <div className="animate-fade-in">
          <h1 className="text-2xl font-bold text-foreground">Roadmap</h1>
          <p className="text-muted-foreground mt-1">
            Project timeline and milestones
          </p>
        </div>

        <div className="space-y-8">
          {quarters.map((quarter, index) => (
            <div
              key={quarter.id}
              className="animate-fade-in"
              style={{ animationDelay: `${index * 100}ms` }}
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2 rounded-lg bg-primary/10">
                  <Calendar className="w-5 h-5 text-primary" />
                </div>
                <h2 className="text-lg font-semibold text-foreground">{quarter.label}</h2>
                <span className="text-xs bg-muted px-2 py-1 rounded-full text-muted-foreground">
                  {quarter.features.length} items
                </span>
              </div>
              
              <div className="relative pl-8 border-l-2 border-border">
                <div className="space-y-4">
                  {quarter.features.map((feature, fIndex) => (
                    <div
                      key={feature.id}
                      className={cn(
                        'relative bg-card border border-border rounded-lg p-4',
                        'hover:shadow-md hover:border-primary/20 transition-all',
                        'before:absolute before:left-[-25px] before:top-5 before:w-3 before:h-3',
                        'before:rounded-full before:border-2 before:border-primary before:bg-background'
                      )}
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <h3 className="font-medium text-card-foreground">{feature.title}</h3>
                          <p className="text-sm text-muted-foreground mt-1">{feature.description}</p>
                        </div>
                        <span className="text-xs font-mono bg-muted px-2 py-1 rounded text-muted-foreground shrink-0">
                          {feature.module}
                        </span>
                      </div>
                      {feature.progress > 0 && (
                        <div className="mt-3">
                          <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                            <div
                              className={cn(
                                'h-full rounded-full transition-all',
                                feature.progress === 100 ? 'bg-success' : 'bg-primary'
                              )}
                              style={{ width: `${feature.progress}%` }}
                            />
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  );
};

export default Roadmap;
