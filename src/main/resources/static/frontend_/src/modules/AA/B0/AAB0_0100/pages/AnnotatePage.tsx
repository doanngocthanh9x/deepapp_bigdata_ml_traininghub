import React, { useState, useCallback, useRef, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Tag, ChevronRight, ChevronLeft } from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import DatasetPanel from '../components/DatasetPanel';
import AnnotatorCanvas from '../components/AnnotatorCanvas';
import ModelPanel from '../components/ModelPanel';
import ActivityLog from '../components/ActivityLog';
import type { Dataset, Annotation } from '@/services/aab0Api';

const AnnotatePage: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [selectedDataset, setSelectedDataset] = useState<Dataset | null>(null);
  const [annotations, setAnnotations] = useState<Annotation[]>([]);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const handleSelectDataset = useCallback((dataset: Dataset) => {
    if (mountedRef.current) {
      setSelectedDataset(dataset);
      setAnnotations([]);
    }
  }, []);

  const handleAnnotationsChange = useCallback((newAnnotations: Annotation[]) => {
    if (mountedRef.current) {
      setAnnotations(newAnnotations);
    }
  }, []);

  const handleInferenceComplete = useCallback((inferredAnnotations: Annotation[]) => {
    if (mountedRef.current) {
      setAnnotations(prev => [...prev, ...inferredAnnotations]);
    }
  }, []);

  return (
    <DashboardLayout>
      <div className="max-w-full mx-auto space-y-4">
        {/* Header */}
        <div className="animate-fade-in">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-primary/10">
                <Tag className="w-6 h-6 text-primary" />
              </div>
              <div>
                <span className="text-xs font-mono text-muted-foreground">AAB0_0100 / Bước 3</span>
                <h1 className="text-2xl font-bold text-foreground">Annotate Dataset</h1>
              </div>
            </div>
            <Button
              onClick={() => navigate(`/modules/AA/B0/AAB0_0100/projects/${projectId}/export`)}
            >
              Tiếp tục Export
              <ChevronRight className="w-4 h-4 ml-2" />
            </Button>
          </div>
        </div>

        {/* Progress Steps */}
        <div className="flex items-center gap-2 text-sm">
          <Badge variant="outline" className="text-green-600 border-green-600">✓ Chọn Model</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge variant="outline" className="text-green-600 border-green-600">✓ Cấu hình Labels</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge>3. Annotate</Badge>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
          <Badge variant="outline">4. Export</Badge>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-12 gap-4" style={{ height: 'calc(100vh - 280px)', minHeight: '500px' }}>
          {/* Left Panel - Datasets */}
          <div className="col-span-12 lg:col-span-2">
            <DatasetPanel 
              onSelectDataset={handleSelectDataset}
              selectedDatasetId={selectedDataset?.id}
            />
          </div>

          {/* Center - Canvas */}
          <div className="col-span-12 lg:col-span-7">
            <AnnotatorCanvas
              dataset={selectedDataset}
              annotations={annotations}
              onAnnotationsChange={handleAnnotationsChange}
              projectType="YOLO"
            />
          </div>

          {/* Right Panel */}
          <div className="col-span-12 lg:col-span-3 space-y-4">
            {/* Model Panel */}
            <ModelPanel
              dataset={selectedDataset}
              projectType="YOLO"
              onInferenceComplete={handleInferenceComplete}
            />

            {/* Activity Log */}
            <div className="flex-1" style={{ minHeight: '200px' }}>
              <ActivityLog projectId={projectId} />
            </div>
          </div>
        </div>

        {/* Navigation */}
        <div className="flex items-center justify-between pt-4 border-t border-border">
          <Button
            variant="outline"
            onClick={() => navigate(`/modules/AA/B0/AAB0_0100/projects/${projectId}/labels`)}
          >
            <ChevronLeft className="w-4 h-4 mr-2" />
            Quay lại Labels
          </Button>
          <div className="text-sm text-muted-foreground">
            {annotations.length} annotations
          </div>
          <Button
            onClick={() => navigate(`/modules/AA/B0/AAB0_0100/projects/${projectId}/export`)}
          >
            Tiếp tục Export
            <ChevronRight className="w-4 h-4 ml-2" />
          </Button>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default AnnotatePage;
