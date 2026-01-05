import React, { useState, useRef, useEffect } from 'react';
import { Download, FileJson, FileText, Code } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { exportAnnotations } from '@/services/aab0Api';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';

interface ExportPanelProps {
  projectId?: string;
  annotationCount: number;
}

type ExportFormat = 'yolo' | 'coco' | 'ocr_json' | 'hocr';

const EXPORT_FORMATS: { id: ExportFormat; name: string; icon: React.ReactNode; ext: string }[] = [
  { id: 'yolo', name: 'YOLO TXT', icon: <FileText className="w-4 h-4" />, ext: '.txt' },
  { id: 'coco', name: 'COCO JSON', icon: <FileJson className="w-4 h-4" />, ext: '.json' },
  { id: 'ocr_json', name: 'OCR JSON', icon: <FileJson className="w-4 h-4" />, ext: '.json' },
  { id: 'hocr', name: 'hOCR', icon: <Code className="w-4 h-4" />, ext: '.xml' },
];

const ExportPanel: React.FC<ExportPanelProps> = ({ projectId, annotationCount }) => {
  const [selectedFormat, setSelectedFormat] = useState<ExportFormat>('yolo');
  const [exporting, setExporting] = useState(false);
  const { toast } = useToast();
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const handleExport = async () => {
    if (!projectId) {
      toast({ title: 'Error', description: 'No project selected', variant: 'destructive' });
      return;
    }

    if (annotationCount === 0) {
      toast({ title: 'Warning', description: 'No annotations to export', variant: 'destructive' });
      return;
    }

    setExporting(true);
    try {
      const blob = await exportAnnotations(projectId, selectedFormat);
      
      // Download the file
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      const format = EXPORT_FORMATS.find(f => f.id === selectedFormat);
      a.href = url;
      a.download = `annotations_${Date.now()}${format?.ext || '.txt'}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      if (mountedRef.current) {
        toast({ title: 'Success', description: `Exported ${annotationCount} annotations` });
      }
    } catch (error) {
      if (mountedRef.current) {
        toast({ title: 'Error', description: 'Export failed', variant: 'destructive' });
      }
    } finally {
      if (mountedRef.current) {
        setExporting(false);
      }
    }
  };

  return (
    <div className="bg-card border border-border rounded-xl p-4">
      <div className="flex items-center gap-2 mb-4">
        <Download className="w-5 h-5 text-primary" />
        <h3 className="font-semibold text-foreground">Export</h3>
      </div>

      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-2">
          {EXPORT_FORMATS.map(format => (
            <button
              key={format.id}
              onClick={() => setSelectedFormat(format.id)}
              className={cn(
                'flex items-center gap-2 p-2 rounded-lg border-2 transition-all text-sm',
                selectedFormat === format.id
                  ? 'border-primary bg-primary/10 text-primary'
                  : 'border-transparent bg-muted/50 text-muted-foreground hover:bg-muted'
              )}
            >
              {format.icon}
              <span>{format.name}</span>
            </button>
          ))}
        </div>

        <div className="p-3 bg-muted/50 rounded-lg text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Annotations</span>
            <span className="font-medium">{annotationCount}</span>
          </div>
        </div>

        <Button 
          className="w-full" 
          onClick={handleExport}
          disabled={exporting || annotationCount === 0}
        >
          <Download className="w-4 h-4 mr-2" />
          {exporting ? 'Exporting...' : 'Export Annotations'}
        </Button>
      </div>
    </div>
  );
};

export default ExportPanel;
