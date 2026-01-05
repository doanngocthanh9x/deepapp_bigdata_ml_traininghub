import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Tag, Upload, ChevronRight, ChevronLeft, Check, Cpu } from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { useToast } from '@/hooks/use-toast';
import { getModels, uploadModel, type Model } from '@/services/aab0Api';

const ModelSelectPage: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [models, setModels] = useState<Model[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedModelId, setSelectedModelId] = useState<string>('');
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const loadModels = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getModels();
      if (mountedRef.current) {
        setModels(data);
      }
    } catch (error) {
      console.error('Failed to load models:', error);
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    loadModels();
  }, [loadModels]);

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    try {
      const newModel = await uploadModel(file);
      if (mountedRef.current) {
        setModels(prev => [newModel, ...prev]);
        setSelectedModelId(newModel.id);
        toast({ title: 'Thành công', description: 'Đã upload model mới' });
      }
    } catch (error) {
      toast({ title: 'Lỗi', description: 'Không thể upload model', variant: 'destructive' });
    } finally {
      if (mountedRef.current) {
        setUploading(false);
      }
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleContinue = () => {
    // Save selected model to project (mock)
    navigate(`/modules/AA/B0/AAB0_0100/projects/${projectId}/labels`);
  };

  const handleSkip = () => {
    navigate(`/modules/AA/B0/AAB0_0100/projects/${projectId}/labels`);
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
              <span className="text-xs font-mono text-muted-foreground">AAB0_0100 / Bước 1</span>
              <h1 className="text-2xl font-bold text-foreground">Chọn Model</h1>
            </div>
          </div>
          <p className="text-muted-foreground">
            Chọn model có sẵn hoặc upload model mới để sử dụng cho inference
          </p>
        </div>

        {/* Progress Steps */}
        <div className="flex items-center gap-2 text-sm">
          <Badge>1. Chọn Model</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge variant="outline">2. Cấu hình Labels</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge variant="outline">3. Annotate</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge variant="outline">4. Export</Badge>
        </div>

        {/* Upload Section */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Upload Model mới</CardTitle>
            <CardDescription>
              Hỗ trợ định dạng .pt, .onnx, .pth
            </CardDescription>
          </CardHeader>
          <CardContent>
            <input
              ref={fileInputRef}
              type="file"
              accept=".pt,.onnx,.pth"
              onChange={handleFileUpload}
              className="hidden"
            />
            <Button
              variant="outline"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
              className="w-full h-20 border-dashed"
            >
              <Upload className="w-5 h-5 mr-2" />
              {uploading ? 'Đang upload...' : 'Chọn file model'}
            </Button>
          </CardContent>
        </Card>

        {/* Model List */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Models có sẵn</CardTitle>
            <CardDescription>
              Chọn một model để sử dụng hoặc bỏ qua bước này
            </CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-16 bg-muted rounded animate-pulse" />
                ))}
              </div>
            ) : models.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                Chưa có model nào. Upload model mới để bắt đầu.
              </div>
            ) : (
              <RadioGroup value={selectedModelId} onValueChange={setSelectedModelId}>
                <div className="space-y-3">
                  {models.map((model) => (
                    <div
                      key={model.id}
                      className={`flex items-center gap-4 p-4 rounded-lg border cursor-pointer transition-all ${
                        selectedModelId === model.id
                          ? 'border-primary bg-primary/5'
                          : 'border-border hover:border-primary/50'
                      }`}
                      onClick={() => setSelectedModelId(model.id)}
                    >
                      <RadioGroupItem value={model.id} id={model.id} />
                      <div className="p-2 rounded-lg bg-muted">
                        <Cpu className="w-5 h-5 text-muted-foreground" />
                      </div>
                      <div className="flex-1">
                        <Label htmlFor={model.id} className="font-medium cursor-pointer">
                          {model.name}
                        </Label>
                        <div className="flex items-center gap-3 text-sm text-muted-foreground mt-1">
                          <Badge variant="outline" className="text-xs">
                            {model.type}
                          </Badge>
                          <span>v{model.version}</span>
                          {model.accuracy && (
                            <span className="text-green-600">
                              {(model.accuracy * 100).toFixed(0)}% accuracy
                            </span>
                          )}
                        </div>
                      </div>
                      {selectedModelId === model.id && (
                        <Check className="w-5 h-5 text-primary" />
                      )}
                    </div>
                  ))}
                </div>
              </RadioGroup>
            )}
          </CardContent>
        </Card>

        {/* Navigation */}
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            onClick={() => navigate('/modules/AA/B0/AAB0_0100/projects')}
          >
            <ChevronLeft className="w-4 h-4 mr-2" />
            Quay lại
          </Button>
          <div className="flex gap-3">
            <Button variant="ghost" onClick={handleSkip}>
              Bỏ qua
            </Button>
            <Button onClick={handleContinue}>
              Tiếp tục
              <ChevronRight className="w-4 h-4 ml-2" />
            </Button>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default ModelSelectPage;
