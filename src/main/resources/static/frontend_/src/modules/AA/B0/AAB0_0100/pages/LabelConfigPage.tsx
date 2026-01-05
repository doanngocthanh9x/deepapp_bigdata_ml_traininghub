import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Tag, Plus, ChevronRight, ChevronLeft, X, Palette } from 'lucide-react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';

interface LabelClass {
  id: string;
  name: string;
  color: string;
}

const defaultColors = [
  '#ef4444', '#f97316', '#eab308', '#22c55e', '#06b6d4', 
  '#3b82f6', '#8b5cf6', '#ec4899', '#6366f1', '#14b8a6'
];

const LabelConfigPage: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [classes, setClasses] = useState<LabelClass[]>([
    { id: '1', name: 'person', color: '#ef4444' },
    { id: '2', name: 'car', color: '#3b82f6' },
    { id: '3', name: 'text', color: '#22c55e' },
  ]);
  const [newClassName, setNewClassName] = useState('');

  const handleAddClass = () => {
    if (!newClassName.trim()) {
      toast({ title: 'Lỗi', description: 'Vui lòng nhập tên class', variant: 'destructive' });
      return;
    }

    if (classes.some(c => c.name.toLowerCase() === newClassName.toLowerCase())) {
      toast({ title: 'Lỗi', description: 'Class này đã tồn tại', variant: 'destructive' });
      return;
    }

    const newClass: LabelClass = {
      id: Date.now().toString(),
      name: newClassName.trim(),
      color: defaultColors[classes.length % defaultColors.length],
    };

    setClasses(prev => [...prev, newClass]);
    setNewClassName('');
    toast({ title: 'Thành công', description: `Đã thêm class "${newClass.name}"` });
  };

  const handleRemoveClass = (id: string) => {
    setClasses(prev => prev.filter(c => c.id !== id));
  };

  const handleColorChange = (id: string, color: string) => {
    setClasses(prev => prev.map(c => c.id === id ? { ...c, color } : c));
  };

  const handleContinue = () => {
    if (classes.length === 0) {
      toast({ title: 'Lỗi', description: 'Vui lòng thêm ít nhất một class', variant: 'destructive' });
      return;
    }
    navigate(`/modules/AA/B0/AAB0_0100/projects/${projectId}/annotate`);
  };

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="animate-fade-in">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 rounded-lg bg-primary/10">
              <Tag className="w-6 h-6 text-primary" />
            </div>
            <div>
              <span className="text-xs font-mono text-muted-foreground">AAB0_0100 / Bước 2</span>
              <h1 className="text-2xl font-bold text-foreground">Cấu hình Labels</h1>
            </div>
          </div>
          <p className="text-muted-foreground">
            Định nghĩa các class/nhãn cho việc annotation
          </p>
        </div>

        {/* Progress Steps */}
        <div className="flex items-center gap-2 text-sm">
          <Badge variant="outline" className="text-green-600 border-green-600">✓ Chọn Model</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge>2. Cấu hình Labels</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge variant="outline">3. Annotate</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge variant="outline">4. Export</Badge>
        </div>

        {/* Add New Class */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Thêm Class mới</CardTitle>
            <CardDescription>
              Nhập tên class và nhấn Enter hoặc nút Thêm
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex gap-3">
              <Input
                placeholder="Ví dụ: person, car, text..."
                value={newClassName}
                onChange={(e) => setNewClassName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleAddClass()}
                className="flex-1"
              />
              <Button onClick={handleAddClass}>
                <Plus className="w-4 h-4 mr-2" />
                Thêm
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Class List */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">
              Danh sách Classes ({classes.length})
            </CardTitle>
            <CardDescription>
              Click vào màu để thay đổi, hoặc xóa class không cần thiết
            </CardDescription>
          </CardHeader>
          <CardContent>
            {classes.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                Chưa có class nào. Thêm class mới để bắt đầu.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {classes.map((cls, index) => (
                  <div
                    key={cls.id}
                    className="flex items-center gap-3 p-3 rounded-lg border border-border hover:border-primary/50 transition-colors"
                  >
                    <span className="text-sm text-muted-foreground font-mono w-6">
                      {index}
                    </span>
                    <Label className="relative cursor-pointer">
                      <input
                        type="color"
                        value={cls.color}
                        onChange={(e) => handleColorChange(cls.id, e.target.value)}
                        className="sr-only"
                      />
                      <div
                        className="w-8 h-8 rounded-lg flex items-center justify-center hover:opacity-80 transition-opacity"
                        style={{ backgroundColor: cls.color }}
                      >
                        <Palette className="w-4 h-4 text-white" />
                      </div>
                    </Label>
                    <span className="flex-1 font-medium text-foreground">{cls.name}</span>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleRemoveClass(cls.id)}
                      className="h-8 w-8 text-muted-foreground hover:text-destructive"
                    >
                      <X className="w-4 h-4" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Quick Add Presets */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Preset Classes</CardTitle>
            <CardDescription>
              Click để thêm nhanh các class phổ biến
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-2">
              {['person', 'car', 'dog', 'cat', 'text', 'logo', 'table', 'chart', 'signature', 'stamp'].map((preset) => (
                <Button
                  key={preset}
                  variant="outline"
                  size="sm"
                  disabled={classes.some(c => c.name === preset)}
                  onClick={() => {
                    setNewClassName(preset);
                    handleAddClass();
                  }}
                >
                  {preset}
                </Button>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Navigation */}
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            onClick={() => navigate(`/modules/AA/B0/AAB0_0100/projects/${projectId}/models`)}
          >
            <ChevronLeft className="w-4 h-4 mr-2" />
            Quay lại
          </Button>
          <Button onClick={handleContinue}>
            Tiếp tục Annotate
            <ChevronRight className="w-4 h-4 ml-2" />
          </Button>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default LabelConfigPage;
