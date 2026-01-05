import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Tag, ChevronLeft, Download, FileJson, FileText, FileCode, Check } from 'lucide-react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { useToast } from '@/hooks/use-toast';
import { exportAnnotations } from '@/services/aab0Api';

type ExportFormat = 'yolo' | 'coco' | 'ocr_json' | 'hocr';

interface ExportOption {
  id: ExportFormat;
  name: string;
  description: string;
  icon: React.ElementType;
  type: 'YOLO' | 'OCR' | 'both';
}

const exportOptions: ExportOption[] = [
  {
    id: 'yolo',
    name: 'YOLO Format',
    description: 'File .txt với normalized coordinates, phù hợp training YOLO models',
    icon: FileText,
    type: 'YOLO',
  },
  {
    id: 'coco',
    name: 'COCO JSON',
    description: 'JSON format chuẩn COCO, tương thích nhiều framework',
    icon: FileJson,
    type: 'both',
  },
  {
    id: 'ocr_json',
    name: 'OCR JSON',
    description: 'JSON chứa text regions và nội dung OCR',
    icon: FileJson,
    type: 'OCR',
  },
  {
    id: 'hocr',
    name: 'hOCR (XML)',
    description: 'Định dạng hOCR chuẩn HTML với OCR metadata',
    icon: FileCode,
    type: 'OCR',
  },
];

const ExportPage: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [selectedFormat, setSelectedFormat] = useState<ExportFormat>('yolo');
  const [includeImages, setIncludeImages] = useState(true);
  const [splitDataset, setSplitDataset] = useState(false);
  const [exporting, setExporting] = useState(false);

  const handleExport = async () => {
    if (!projectId) return;

    setExporting(true);
    try {
      const blob = await exportAnnotations(projectId, selectedFormat);
      
      // Create download link
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `annotations_${selectedFormat}_${Date.now()}.${
        selectedFormat === 'yolo' ? 'zip' : 
        selectedFormat === 'hocr' ? 'xml' : 'json'
      }`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      toast({
        title: 'Xuất thành công',
        description: `Đã xuất annotations theo định dạng ${selectedFormat.toUpperCase()}`,
      });
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể xuất annotations',
        variant: 'destructive',
      });
    } finally {
      setExporting(false);
    }
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
              <span className="text-xs font-mono text-muted-foreground">AAB0_0100 / Bước 4</span>
              <h1 className="text-2xl font-bold text-foreground">Xuất Dataset</h1>
            </div>
          </div>
          <p className="text-muted-foreground">
            Xuất annotations theo các định dạng phổ biến
          </p>
        </div>

        {/* Progress Steps */}
        <div className="flex items-center gap-2 text-sm">
          <Badge variant="outline" className="text-green-600 border-green-600">✓ Chọn Model</Badge>
          <span className="text-muted-foreground">→</span>
          <Badge variant="outline" className="text-green-600 border-green-600">✓ Cấu hình Labels</Badge>
          <span className="text-muted-foreground">→</span>
          <Badge variant="outline" className="text-green-600 border-green-600">✓ Annotate</Badge>
          <span className="text-muted-foreground">→</span>
          <Badge>4. Export</Badge>
        </div>

        {/* Export Format Selection */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Chọn định dạng xuất</CardTitle>
            <CardDescription>
              Chọn định dạng phù hợp với mục đích sử dụng
            </CardDescription>
          </CardHeader>
          <CardContent>
            <RadioGroup value={selectedFormat} onValueChange={(v) => setSelectedFormat(v as ExportFormat)}>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {exportOptions.map((option) => {
                  const Icon = option.icon;
                  return (
                    <div
                      key={option.id}
                      className={`flex items-start gap-4 p-4 rounded-lg border cursor-pointer transition-all ${
                        selectedFormat === option.id
                          ? 'border-primary bg-primary/5'
                          : 'border-border hover:border-primary/50'
                      }`}
                      onClick={() => setSelectedFormat(option.id)}
                    >
                      <RadioGroupItem value={option.id} id={option.id} className="mt-1" />
                      <div className="p-2 rounded-lg bg-muted">
                        <Icon className="w-5 h-5 text-muted-foreground" />
                      </div>
                      <div className="flex-1">
                        <Label htmlFor={option.id} className="font-medium cursor-pointer flex items-center gap-2">
                          {option.name}
                          <Badge variant="outline" className="text-xs">
                            {option.type}
                          </Badge>
                        </Label>
                        <p className="text-sm text-muted-foreground mt-1">
                          {option.description}
                        </p>
                      </div>
                      {selectedFormat === option.id && (
                        <Check className="w-5 h-5 text-primary" />
                      )}
                    </div>
                  );
                })}
              </div>
            </RadioGroup>
          </CardContent>
        </Card>

        {/* Export Options */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Tùy chọn xuất</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center space-x-2">
              <Checkbox
                id="include-images"
                checked={includeImages}
                onCheckedChange={(checked) => setIncludeImages(checked as boolean)}
              />
              <Label htmlFor="include-images" className="cursor-pointer">
                Bao gồm ảnh gốc trong file xuất
              </Label>
            </div>
            <div className="flex items-center space-x-2">
              <Checkbox
                id="split-dataset"
                checked={splitDataset}
                onCheckedChange={(checked) => setSplitDataset(checked as boolean)}
              />
              <Label htmlFor="split-dataset" className="cursor-pointer">
                Chia thành train/val/test (80/10/10)
              </Label>
            </div>
          </CardContent>
        </Card>

        {/* Summary */}
        <Card className="bg-muted/50">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-medium text-foreground">Sẵn sàng xuất</h3>
                <p className="text-sm text-muted-foreground mt-1">
                  Định dạng: <strong>{selectedFormat.toUpperCase()}</strong>
                  {includeImages && ' • Bao gồm ảnh'}
                  {splitDataset && ' • Chia train/val/test'}
                </p>
              </div>
              <Button onClick={handleExport} disabled={exporting} size="lg">
                <Download className="w-4 h-4 mr-2" />
                {exporting ? 'Đang xuất...' : 'Xuất Dataset'}
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Navigation */}
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            onClick={() => navigate(`/modules/AA/B0/AAB0_0100/projects/${projectId}/annotate`)}
          >
            <ChevronLeft className="w-4 h-4 mr-2" />
            Quay lại Annotate
          </Button>
          <Button
            variant="outline"
            onClick={() => navigate('/modules/AA/B0/AAB0_0100/projects')}
          >
            Về danh sách dự án
          </Button>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default ExportPage;
