import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { DashboardLayout } from '@/components/DashboardLayout';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { 
  Globe, 
  CheckCircle2, 
  Code, 
  ArrowLeft,
  Languages,
  Database,
  Layers,
  Zap
} from 'lucide-react';

const I18nDemo = () => {
  const { t, i18n } = useTranslation();

  const codeExample = `import { useTranslation } from 'react-i18next';

const MyComponent = () => {
  const { t } = useTranslation();
  
  return (
    <div>
      <h1>{t('common.welcome')}</h1>
      <p>{t('i18nDemo.greeting')}</p>
      
      {/* With interpolation */}
      <p>{t('i18nDemo.interpolationExample', { 
        name: 'John', 
        count: 5 
      })}</p>
    </div>
  );
};`;

  const features = [
    { icon: Globe, text: t('i18nDemo.features.autoDetect') },
    { icon: Database, text: t('i18nDemo.features.persist') },
    { icon: Layers, text: t('i18nDemo.features.nested') },
    { icon: Zap, text: t('i18nDemo.features.interpolation') },
  ];

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Header */}
        <div className="animate-fade-in">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary/10 to-accent/10 flex items-center justify-center">
              <Languages className="w-6 h-6 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">{t('i18nDemo.title')}</h1>
              <p className="text-muted-foreground">{t('i18nDemo.description')}</p>
            </div>
          </div>
        </div>

        {/* Language Switcher Card */}
        <Card className="animate-fade-in" style={{ animationDelay: '50ms' }}>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Globe className="w-5 h-5 text-primary" />
              {t('i18nDemo.selectLanguage')}
            </CardTitle>
            <CardDescription>
              {t('i18nDemo.currentLanguage')}: <Badge variant="secondary">{i18n.language.toUpperCase()}</Badge>
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-4">
              <LanguageSwitcher />
              <LanguageSwitcher variant="minimal" />
            </div>
          </CardContent>
        </Card>

        {/* Live Demo Card */}
        <Card className="animate-fade-in" style={{ animationDelay: '100ms' }}>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Zap className="w-5 h-5 text-warning" />
              {t('i18nDemo.liveDemo')}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="p-4 rounded-lg bg-muted/50 border border-border">
              <p className="text-lg font-medium text-foreground mb-2">
                {t('i18nDemo.greeting')}
              </p>
              <p className="text-muted-foreground">
                {t('i18nDemo.paragraph')}
              </p>
            </div>

            <div className="p-4 rounded-lg bg-primary/5 border border-primary/20">
              <p className="text-sm font-medium text-primary mb-1">Interpolation Example:</p>
              <p className="text-foreground">
                {t('i18nDemo.interpolationExample', { name: 'Admin', count: 5 })}
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Features Card */}
        <Card className="animate-fade-in" style={{ animationDelay: '150ms' }}>
          <CardHeader>
            <CardTitle>{t('i18nDemo.features.title')}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid sm:grid-cols-2 gap-3">
              {features.map((feature, index) => (
                <div
                  key={index}
                  className="flex items-center gap-3 p-3 rounded-lg bg-muted/30 border border-border"
                >
                  <div className="w-8 h-8 rounded-lg bg-success/10 flex items-center justify-center">
                    <CheckCircle2 className="w-4 h-4 text-success" />
                  </div>
                  <span className="text-sm text-foreground">{feature.text}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Code Example Card */}
        <Card className="animate-fade-in" style={{ animationDelay: '200ms' }}>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Code className="w-5 h-5 text-accent" />
              {t('i18nDemo.howToUse')}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <ol className="list-decimal list-inside space-y-2 text-muted-foreground">
                <li>{t('i18nDemo.step1')}</li>
                <li>{t('i18nDemo.step2')}</li>
                <li>{t('i18nDemo.step3')}</li>
              </ol>

              <div className="mt-4">
                <p className="text-sm font-medium text-foreground mb-2">{t('i18nDemo.example')}:</p>
                <pre className="p-4 rounded-lg bg-sidebar text-sidebar-foreground text-sm overflow-x-auto">
                  <code>{codeExample}</code>
                </pre>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* File Structure Card */}
        <Card className="animate-fade-in" style={{ animationDelay: '250ms' }}>
          <CardHeader>
            <CardTitle>📁 File Structure</CardTitle>
          </CardHeader>
          <CardContent>
            <pre className="p-4 rounded-lg bg-sidebar text-sidebar-foreground text-sm overflow-x-auto">
              <code>{`src/
├── i18n/
│   ├── index.ts          # i18n configuration
│   └── locales/
│       ├── vi.json       # Vietnamese translations
│       └── en.json       # English translations
└── components/
    └── LanguageSwitcher.tsx  # Language switch component`}</code>
            </pre>
          </CardContent>
        </Card>

        {/* Back Button */}
        <div className="flex justify-center animate-fade-in" style={{ animationDelay: '300ms' }}>
          <Link to="/dashboard">
            <Button variant="outline" size="lg">
              <ArrowLeft className="w-4 h-4 mr-2" />
              {t('i18nDemo.backToDashboard')}
            </Button>
          </Link>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default I18nDemo;
