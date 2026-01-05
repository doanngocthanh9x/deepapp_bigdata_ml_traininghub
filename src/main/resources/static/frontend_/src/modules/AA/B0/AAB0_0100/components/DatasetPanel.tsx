import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Upload, Image, FileText, ChevronLeft, ChevronRight, Check, Clock, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { getDatasets, uploadDataset, type Dataset } from '@/services/aab0Api';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';

interface DatasetPanelProps {
  onSelectDataset: (dataset: Dataset) => void;
  selectedDatasetId?: string;
}

const DatasetPanel: React.FC<DatasetPanelProps> = ({ onSelectDataset, selectedDatasetId }) => {
  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const pageSize = 6;
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { toast } = useToast();
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const fetchDatasets = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getDatasets(page, pageSize);
      if (mountedRef.current) {
        setDatasets(result.data);
        setTotal(result.total);
      }
    } catch (error) {
      console.error('Failed to fetch datasets:', error);
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, [page]);

  useEffect(() => {
    fetchDatasets();
  }, [fetchDatasets]);

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    try {
      const newDatasets = await uploadDataset(Array.from(files));
      if (mountedRef.current) {
        setDatasets(prev => [...newDatasets, ...prev].slice(0, pageSize));
        setTotal(prev => prev + newDatasets.length);
        toast({ title: 'Success', description: `Uploaded ${files.length} file(s)` });
      }
    } catch (error) {
      if (mountedRef.current) {
        toast({ title: 'Error', description: 'Failed to upload files', variant: 'destructive' });
      }
    } finally {
      if (mountedRef.current) {
        setUploading(false);
      }
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const totalPages = Math.ceil(total / pageSize);

  const getStatusIcon = (status: Dataset['status']) => {
    switch (status) {
      case 'annotated':
        return <Check className="w-3 h-3 text-green-500" />;
      case 'reviewed':
        return <Eye className="w-3 h-3 text-blue-500" />;
      default:
        return <Clock className="w-3 h-3 text-yellow-500" />;
    }
  };

  return (
    <div className="bg-card border border-border rounded-xl p-4 h-full flex flex-col">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-foreground">Datasets</h3>
        <div>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleUpload}
            accept="image/*,.pdf"
            multiple
            className="hidden"
          />
          <Button
            size="sm"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
          >
            <Upload className="w-4 h-4 mr-2" />
            {uploading ? 'Uploading...' : 'Upload'}
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex-1 flex items-center justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
        </div>
      ) : (
        <div className="flex-1 overflow-y-auto">
          <div className="grid grid-cols-2 gap-2">
            {datasets.map((dataset) => (
              <button
                key={dataset.id}
                onClick={() => onSelectDataset(dataset)}
                className={cn(
                  'relative rounded-lg overflow-hidden border-2 transition-all',
                  selectedDatasetId === dataset.id
                    ? 'border-primary ring-2 ring-primary/20'
                    : 'border-transparent hover:border-border'
                )}
              >
                <div className="aspect-video bg-muted relative">
                  {dataset.type === 'image' ? (
                    <img
                      src={dataset.thumbnailUrl || dataset.url}
                      alt={dataset.name}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      <FileText className="w-8 h-8 text-muted-foreground" />
                    </div>
                  )}
                  <div className="absolute top-1 right-1 p-1 rounded bg-background/80">
                    {getStatusIcon(dataset.status)}
                  </div>
                </div>
                <div className="p-2 text-left">
                  <p className="text-xs font-medium truncate">{dataset.name}</p>
                  <p className="text-[10px] text-muted-foreground">
                    {dataset.annotationCount} annotations
                  </p>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Pagination */}
      <div className="flex items-center justify-between mt-4 pt-4 border-t border-border">
        <span className="text-xs text-muted-foreground">
          {total} items
        </span>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="icon"
            className="h-8 w-8"
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={page === 1}
          >
            <ChevronLeft className="w-4 h-4" />
          </Button>
          <span className="text-sm">
            {page} / {totalPages || 1}
          </span>
          <Button
            variant="outline"
            size="icon"
            className="h-8 w-8"
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={page >= totalPages}
          >
            <ChevronRight className="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>
  );
};

export default DatasetPanel;
