import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Plus, Upload, FolderOpen } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { getModels, createProject, uploadModel, type Model, type Project } from '@/services/aab0Api';
import { useToast } from '@/hooks/use-toast';

interface ProjectHeaderProps {
  onProjectCreate: (project: Project) => void;
  currentProject?: Project | null;
}

const ProjectHeader: React.FC<ProjectHeaderProps> = ({ onProjectCreate, currentProject }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [models, setModels] = useState<Model[]>([]);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    type: 'YOLO' as 'YOLO' | 'OCR',
    modelId: '',
  });
  const [uploadingModel, setUploadingModel] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
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
      try {
        const data = await getModels();
        if (mountedRef.current) {
          setModels(data);
        }
      } catch (error) {
        console.error('Failed to fetch models:', error);
      }
    };
    fetchModels();
  }, []);

  const filteredModels = models.filter(m => m.type === formData.type);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      toast({ title: 'Error', description: 'Project name is required', variant: 'destructive' });
      return;
    }

    setLoading(true);
    try {
      const project = await createProject({
        name: formData.name,
        type: formData.type,
        modelId: formData.modelId || undefined,
      });
      if (mountedRef.current) {
        onProjectCreate(project);
        setIsOpen(false);
        setFormData({ name: '', type: 'YOLO', modelId: '' });
        toast({ title: 'Success', description: 'Project created successfully' });
      }
    } catch (error) {
      if (mountedRef.current) {
        toast({ title: 'Error', description: 'Failed to create project', variant: 'destructive' });
      }
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  };

  const handleModelUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingModel(true);
    try {
      const newModel = await uploadModel(file);
      if (mountedRef.current) {
        setModels(prev => [...prev, newModel]);
        setFormData(prev => ({ ...prev, modelId: newModel.id }));
        toast({ title: 'Success', description: 'Model uploaded successfully' });
      }
    } catch (error) {
      if (mountedRef.current) {
        toast({ title: 'Error', description: 'Failed to upload model', variant: 'destructive' });
      }
    } finally {
      if (mountedRef.current) {
        setUploadingModel(false);
      }
    }
  };

  return (
    <div className="flex items-center justify-between p-4 bg-card border border-border rounded-xl">
      <div className="flex items-center gap-4">
        <div className="p-2 rounded-lg bg-primary/10">
          <FolderOpen className="w-6 h-6 text-primary" />
        </div>
        <div>
          {currentProject ? (
            <>
              <h2 className="font-semibold text-foreground">{currentProject.name}</h2>
              <p className="text-sm text-muted-foreground">
                {currentProject.type} • {currentProject.datasetCount} datasets • {currentProject.annotationCount} annotations
              </p>
            </>
          ) : (
            <>
              <h2 className="font-semibold text-foreground">No Project Selected</h2>
              <p className="text-sm text-muted-foreground">Create a new project to get started</p>
            </>
          )}
        </div>
      </div>

      <Dialog open={isOpen} onOpenChange={setIsOpen}>
        <DialogTrigger asChild>
          <Button>
            <Plus className="w-4 h-4 mr-2" />
            New Project
          </Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create New Project</DialogTitle>
            <DialogDescription>
              Set up a new annotation project with YOLO or OCR configuration.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Project Name</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Enter project name"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="type">Project Type</Label>
              <Select
                value={formData.type}
                onValueChange={(value: 'YOLO' | 'OCR') => setFormData(prev => ({ ...prev, type: value, modelId: '' }))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="YOLO">YOLO (Object Detection)</SelectItem>
                  <SelectItem value="OCR">OCR (Text Recognition)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="model">Model (Optional)</Label>
              <Select
                value={formData.modelId}
                onValueChange={(value) => setFormData(prev => ({ ...prev, modelId: value }))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select existing model" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">No model</SelectItem>
                  {filteredModels.map(model => (
                    <SelectItem key={model.id} value={model.id}>
                      {model.name} (v{model.version}) {model.accuracy && `- ${(model.accuracy * 100).toFixed(0)}%`}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleModelUpload}
                accept=".pt,.onnx,.pth"
                className="hidden"
              />
              <Button
                type="button"
                variant="outline"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingModel}
              >
                <Upload className="w-4 h-4 mr-2" />
                {uploadingModel ? 'Uploading...' : 'Upload New Model'}
              </Button>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setIsOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={loading}>
                {loading ? 'Creating...' : 'Create Project'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default ProjectHeader;
