import { cn } from '@/lib/utils';
import { CheckCircle2, Clock, AlertCircle, Circle } from 'lucide-react';

export interface Feature {
  id: string;
  title: string;
  description: string;
  priority: 'must-have' | 'should-have' | 'could-have';
  status: 'completed' | 'in-progress' | 'planned';
  module: string;
  progress: number;
}

interface FeatureCardProps {
  feature: Feature;
  className?: string;
  style?: React.CSSProperties;
}

const statusConfig = {
  completed: {
    icon: CheckCircle2,
    label: 'Completed',
    className: 'text-success',
  },
  'in-progress': {
    icon: Clock,
    label: 'In Progress',
    className: 'text-warning',
  },
  planned: {
    icon: Circle,
    label: 'Planned',
    className: 'text-muted-foreground',
  },
};

const priorityConfig = {
  'must-have': {
    label: 'Must Have',
    className: 'bg-destructive/10 text-destructive border-destructive/20',
  },
  'should-have': {
    label: 'Should Have',
    className: 'bg-warning/10 text-warning border-warning/20',
  },
  'could-have': {
    label: 'Could Have',
    className: 'bg-primary/10 text-primary border-primary/20',
  },
};

export const FeatureCard = ({ feature, className, style }: FeatureCardProps) => {
  const status = statusConfig[feature.status];
  const priority = priorityConfig[feature.priority];
  const StatusIcon = status.icon;

  return (
    <div
      className={cn(
        'group bg-card border border-border rounded-xl p-5 transition-all duration-300',
        'hover:shadow-lg hover:border-primary/20 hover:-translate-y-0.5',
        'animate-fade-in',
        className
      )}
      style={style}
    >
      <div className="flex items-start justify-between gap-4 mb-3">
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-card-foreground truncate group-hover:text-primary transition-colors">
            {feature.title}
          </h3>
          <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
            {feature.description}
          </p>
        </div>
        <span
          className={cn(
            'shrink-0 px-2.5 py-1 text-xs font-medium rounded-full border',
            priority.className
          )}
        >
          {priority.label}
        </span>
      </div>

      <div className="flex items-center justify-between mt-4">
        <div className="flex items-center gap-2">
          <StatusIcon className={cn('w-4 h-4', status.className)} />
          <span className="text-sm text-muted-foreground">{status.label}</span>
        </div>
        <span className="text-xs font-mono text-muted-foreground bg-muted px-2 py-1 rounded">
          {feature.module}
        </span>
      </div>

      {feature.status !== 'planned' && (
        <div className="mt-4">
          <div className="flex items-center justify-between text-xs mb-1.5">
            <span className="text-muted-foreground">Progress</span>
            <span className="font-medium text-foreground">{feature.progress}%</span>
          </div>
          <div className="h-1.5 bg-muted rounded-full overflow-hidden">
            <div
              className={cn(
                'h-full rounded-full transition-all duration-500',
                feature.progress === 100 ? 'bg-success' : 'bg-primary'
              )}
              style={{ width: `${feature.progress}%` }}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default FeatureCard;
