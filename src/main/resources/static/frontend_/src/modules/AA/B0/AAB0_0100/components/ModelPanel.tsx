import React, { useState, useEffect, useRef } from 'react';
import { Play, Cpu, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select';
import { getModels, runInference, type Model, type Dataset, type Annotation } from '@/services/aab0Api';
import { useToast } from '@/hooks/use-toast';
import { Progress } from '@/components/ui/progress';

interface ModelPanelProps {
  dataset?: Dataset | null;
  projectType?: 'YOLO' | 'OCR';
  onInferenceComplete: (annotations: Annotation[]) => void;
}

const ModelPanel: React.FC<ModelPanelProps> = ({ 
  dataset, 
  projectType = 'YOLO',
  onInferenceComplete 
}) => {
  const [models, setModels] = useState<Model[]>([]);
  const [selectedModelId, setSelectedModelId] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [inferencing, setInferencing] = useState(false);
  const [progress, setProgress] = useState(0);
  const { toast } = useToast();
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    const fetchModels = async () => {
      setLoading(true);
      try {
        const data = await getModels();
        if (mountedRef.current) {
          const filtered = data.filter(m => m.type === projectType);
          setModels(filtered);
          if (filtered.length > 0) {
            setSelectedModelId(filtered[0].id);
          }
        }
      } catch (error) {
        console.error('Failed to fetch models:', error);
      } finally {
        if (mountedRef.current) {
          setLoading(false);
        }
      }
    };
    fetchModels();
  }, [projectType]);

  const handleRunInference = async () => {
    if (!dataset || !selectedModelId) {
      toast({ title: 'Error', description: 'Please select a dataset and model', variant: 'destructive' });
      return;
    }

    setInferencing(true);
    setProgress(0);

    // Simulate progress
    const progressInterval = setInterval(() => {
      setProgress(p => Math.min(p + 10, 90));
    }, 200);

    try {
      const result = await runInference(dataset.id, selectedModelId);
      if (mountedRef.current) {
        setProgress(100);
        onInferenceComplete(result.annotations);
        toast({ 
          title: 'Inference Complete', 
          description: `Found ${result.annotations.length} annotations in ${result.processingTime}s` 
        });
      }
    } catch (error) {
      if (mountedRef.current) {
        toast({ title: 'Error', description: 'Inference failed', variant: 'destructive' });
      }
    } finally {
      clearInterval(progressInterval);
      if (mountedRef.current) {
        setInferencing(false);
        setTimeout(() => setProgress(0), 1000);
      }
    }
  };

  const selectedModel = models.find(m => m.id === selectedModelId);

  return (
    <div className="bg-card border border-border rounded-xl p-4">
      <div className="flex items-center gap-2 mb-4">
        <Cpu className="w-5 h-5 text-primary" />
        <h3 className="font-semibold text-foreground">Model</h3>
      </div>

      <div className="space-y-4">
        <div>
          <label className="text-sm text-muted-foreground mb-2 block">Select Model</label>
          <Select value={selectedModelId} onValueChange={setSelectedModelId} disabled={loading}>
            <SelectTrigger>
              <SelectValue placeholder="Choose a model" />
            </SelectTrigger>
            <SelectContent>
              {models.map(model => (
                <SelectItem key={model.id} value={model.id}>
                  {model.name} (v{model.version})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {selectedModel && (
          <div className="p-3 bg-muted/50 rounded-lg space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Type</span>
              <span className="font-medium">{selectedModel.type}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Version</span>
              <span className="font-medium">{selectedModel.version}</span>
            </div>
            {selectedModel.accuracy && (
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Accuracy</span>
                <span className="font-medium text-green-500">
                  {(selectedModel.accuracy * 100).toFixed(1)}%
                </span>
              </div>
            )}
          </div>
        )}

        {inferencing && (
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <RefreshCw className="w-4 h-4 animate-spin" />
              Running inference...
            </div>
            <Progress value={progress} className="h-2" />
          </div>
        )}

        <Button 
          className="w-full" 
          onClick={handleRunInference}
          disabled={!dataset || !selectedModelId || inferencing}
        >
          <Play className="w-4 h-4 mr-2" />
          {inferencing ? 'Processing...' : 'Run Inference'}
        </Button>
      </div>
    </div>
  );
};

export default ModelPanel;
