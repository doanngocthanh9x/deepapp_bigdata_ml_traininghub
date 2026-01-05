import { Feature, FeatureCard } from './FeatureCard';
import { cn } from '@/lib/utils';

interface FeatureListProps {
  features: Feature[];
  title: string;
  description?: string;
  className?: string;
}

export const FeatureList = ({ features, title, description, className }: FeatureListProps) => {
  if (features.length === 0) return null;

  return (
    <section className={cn('space-y-4', className)}>
      <div>
        <h2 className="text-xl font-semibold text-foreground">{title}</h2>
        {description && (
          <p className="text-sm text-muted-foreground mt-1">{description}</p>
        )}
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {features.map((feature, index) => (
          <FeatureCard
            key={feature.id}
            feature={feature}
            style={{ animationDelay: `${index * 50}ms` }}
          />
        ))}
      </div>
    </section>
  );
};

export default FeatureList;
