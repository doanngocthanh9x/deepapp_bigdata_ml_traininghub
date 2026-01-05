import React from 'react';
import { Link } from 'react-router-dom';
import { Layers, Shield, ScanText, FileSearch, Tag, ChevronRight, Workflow, FileText, Folder, Target, FolderOpen } from 'lucide-react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { getAllModules, AAModuleLeaf } from '@/utils/aaModuleLoader';

const iconMap: Record<string, React.ElementType> = {
  Shield,
  ScanText,
  FileSearch,
  Tag,
  Layers,
  Workflow,
  FileText,
  Folder,
  Target,
  FolderOpen,
};

const statusColors: Record<string, string> = {
  active: 'bg-green-500/10 text-green-600 dark:text-green-400',
  development: 'bg-yellow-500/10 text-yellow-600 dark:text-yellow-400',
  planned: 'bg-muted text-muted-foreground',
};

const statusLabels: Record<string, string> = {
  active: 'Hoạt động',
  development: 'Đang phát triển',
  planned: 'Lên kế hoạch',
};

const ModulesLanding: React.FC = () => {
  const modules = getAllModules();

  // Group modules by category (A0, B0, etc.)
  const groupedModules = modules.reduce<Record<string, AAModuleLeaf[]>>((acc, module) => {
    const parts = module.path.split('/');
    const category = parts[3] || 'Other'; // e.g., A0, B0
    if (!acc[category]) acc[category] = [];
    acc[category].push(module);
    return acc;
  }, {});

  return (
    <DashboardLayout>
      <div className="max-w-6xl mx-auto space-y-8">
        {/* Header */}
        <div className="animate-fade-in">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 rounded-lg bg-primary/10">
              <Layers className="w-6 h-6 text-primary" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground">AA Modules</h1>
              <p className="text-muted-foreground">
                Danh sách các module AI và xử lý dữ liệu
              </p>
            </div>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <Card>
            <CardContent className="pt-6">
              <div className="text-2xl font-bold text-foreground">{modules.length}</div>
              <p className="text-sm text-muted-foreground">Tổng số modules</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="text-2xl font-bold text-green-600">
                {modules.filter(m => m.status === 'active').length}
              </div>
              <p className="text-sm text-muted-foreground">Đang hoạt động</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="text-2xl font-bold text-yellow-600">
                {modules.filter(m => m.status === 'development').length}
              </div>
              <p className="text-sm text-muted-foreground">Đang phát triển</p>
            </CardContent>
          </Card>
        </div>

        {/* Module Groups */}
        {Object.entries(groupedModules).map(([category, categoryModules]) => (
          <div key={category} className="space-y-4">
            <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
              <span className="text-primary">{category}</span>
              <span className="text-muted-foreground">Modules</span>
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {categoryModules.map((module) => {
                const Icon = iconMap[module.icon || 'Layers'] || Layers;

                return (
                  <Link key={module.path} to={module.path}>
                    <Card className="h-full hover:shadow-lg transition-all hover:border-primary/50 group cursor-pointer">
                      <CardHeader className="pb-3">
                        <div className="flex items-start justify-between">
                          <div className="p-2 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
                            <Icon className="w-5 h-5 text-primary" />
                          </div>
                          <Badge className={statusColors[module.status]}>
                            {statusLabels[module.status]}
                          </Badge>
                        </div>
                        <CardTitle className="text-lg flex items-center gap-2 mt-3">
                          <span className="font-mono text-sm text-muted-foreground">{module.code}</span>
                        </CardTitle>
                        <CardDescription className="font-medium text-foreground">
                          {module.name}
                        </CardDescription>
                      </CardHeader>
                      <CardContent>
                        <p className="text-sm text-muted-foreground mb-3">
                          {module.description}
                        </p>
                        <div className="flex items-center text-sm text-primary group-hover:translate-x-1 transition-transform">
                          <span>Mở module</span>
                          <ChevronRight className="w-4 h-4 ml-1" />
                        </div>
                      </CardContent>
                    </Card>
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </DashboardLayout>
  );
};

export default ModulesLanding;