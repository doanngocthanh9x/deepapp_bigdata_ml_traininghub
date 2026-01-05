import { useState, useCallback } from 'react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { Target, Upload, Zap, Image, Loader2, CheckCircle, XCircle, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StatsCard } from '@/components/StatsCard';
import { useApi } from '@/hooks/useApi';
import { yoloApi, fileToBase64, validateImageFile, type YoloDetectionRequest, type YoloDetectionResponse } from '@/services/aaApi';
import { cn } from '@/lib/utils';
import { useToast } from '@/hooks/use-toast';

const YoloDetectionPage = () => {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [results, setResults] = useState<YoloDetectionResponse[]>([]);
  const [confidence, setConfidence] = useState(0.5);
  const [iou, setIou] = useState(0.45);
  const [maxDetections, setMaxDetections] = useState(50);
  const [model, setModel] = useState('yolov8n');
  const { toast } = useToast();

  const { execute: detectObjects, loading, error } = useApi<YoloDetectionResponse>(
    async () => {
      if (!selectedFile) throw new Error('No file selected');

      const formData = new FormData();
      formData.append('image', selectedFile);
      formData.append('confidence', confidence.toString());
      formData.append('iou', iou.toString());
      formData.append('max_detections', maxDetections.toString());
      formData.append('model', model);

      return yoloApi.detectObjects(formData);
    },
    {
      onSuccess: (data) => {
        setResults(prev => [data, ...prev.slice(0, 4)]); // Keep last 5 results
        toast({
          title: "Detection Complete",
          description: `Found ${data.detections?.length || 0} objects`,
        });
      },
      onError: (error) => {
        toast({
          title: "Detection Failed",
          description: error,
          variant: "destructive",
        });
      },
    }
  );

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  }, []);

  const handleFileSelect = useCallback(async (file: File) => {
    const validation = validateImageFile(file);
    if (!validation.valid) {
      toast({
        title: "Invalid File",
        description: validation.error,
        variant: "destructive",
      });
      return;
    }

    setSelectedFile(file);

    // Create preview URL
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);

    toast({
      title: "File Selected",
      description: `${file.name} (${(file.size / 1024 / 1024).toFixed(2)} MB)`,
    });
  }, [toast]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const files = e.dataTransfer.files;
    if (files && files[0]) {
      handleFileSelect(files[0]);
    }
  }, [handleFileSelect]);

  const handleFileInput = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files[0]) {
      handleFileSelect(files[0]);
    }
  }, [handleFileSelect]);

  const handleDetect = useCallback(() => {
    if (!selectedFile) {
      toast({
        title: "No File Selected",
        description: "Please select an image file first",
        variant: "destructive",
      });
      return;
    }
    detectObjects();
  }, [selectedFile, detectObjects, toast]);

  const renderDetectionOverlay = (detections: YoloDetectionResponse['detections']) => {
    if (!detections || !previewUrl) return null;

    return (
      <div className="absolute inset-0 pointer-events-none">
        {detections.map((detection, index) => (
          <div
            key={index}
            className="absolute border-2 border-red-500 bg-red-500/20"
            style={{
              left: `${detection.bbox[0] * 100}%`,
              top: `${detection.bbox[1] * 100}%`,
              width: `${(detection.bbox[2] - detection.bbox[0]) * 100}%`,
              height: `${(detection.bbox[3] - detection.bbox[1]) * 100}%`,
            }}
          >
            <div className="absolute -top-6 left-0 bg-red-500 text-white text-xs px-1 py-0.5 rounded whitespace-nowrap">
              {detection.class} ({(detection.confidence * 100).toFixed(1)}%)
            </div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">YOLO Object Detection</h1>
            <p className="text-muted-foreground">
              Detect objects in images using YOLO models
            </p>
          </div>
          <Target className="h-8 w-8 text-muted-foreground" />
        </div>

        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <StatsCard
            title="Total Detections"
            value={results.length.toString()}
            icon={Target}
            description="Images processed"
          />
          <StatsCard
            title="Objects Found"
            value={results.reduce((acc, r) => acc + (r.detections?.length || 0), 0).toString()}
            icon={Eye}
            description="Total objects detected"
          />
          <StatsCard
            title="Avg Objects/Image"
            value={results.length > 0 ? (results.reduce((acc, r) => acc + (r.detections?.length || 0), 0) / results.length).toFixed(1) : '0'}
            icon={Zap}
            description="Average detections per image"
          />
          <StatsCard
            title="Success Rate"
            value={results.length > 0 ? `${((results.filter(r => r.success).length / results.length) * 100).toFixed(1)}%` : '0%'}
            icon={CheckCircle}
            description="Successful detections"
          />
        </div>

        {/* Main Content */}
        <div className="grid gap-6 lg:grid-cols-2">
          {/* Upload Section */}
          <div className="space-y-4">
            <div className="rounded-lg border-2 border-dashed border-muted-foreground/25 p-8 text-center">
              <div
                className={cn(
                  "cursor-pointer transition-colors",
                  dragActive && "border-primary bg-primary/5"
                )}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
                onClick={() => document.getElementById('file-input')?.click()}
              >
                <Upload className="mx-auto h-12 w-12 text-muted-foreground" />
                <div className="mt-4">
                  <p className="text-lg font-medium">
                    {selectedFile ? selectedFile.name : 'Drop your image here'}
                  </p>
                  <p className="text-sm text-muted-foreground mt-1">
                    or click to browse files
                  </p>
                  <p className="text-xs text-muted-foreground mt-2">
                    Supports PNG, JPG, JPEG up to 10MB
                  </p>
                </div>
              </div>
              <input
                id="file-input"
                type="file"
                accept="image/*"
                onChange={handleFileInput}
                className="hidden"
              />
            </div>

            {/* Image Preview */}
            {previewUrl && (
              <div className="rounded-lg border p-4">
                <h3 className="font-medium mb-2">Image Preview</h3>
                <div className="relative">
                  <img
                    src={previewUrl}
                    alt="Preview"
                    className="w-full max-h-64 object-contain rounded"
                  />
                  {results[0]?.detections && renderDetectionOverlay(results[0].detections)}
                </div>
              </div>
            )}

            {/* Parameters */}
            <div className="space-y-4 rounded-lg border p-4">
              <h3 className="font-medium">Detection Parameters</h3>

              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="text-sm font-medium">Confidence Threshold</label>
                  <input
                    type="range"
                    min="0.1"
                    max="1.0"
                    step="0.05"
                    value={confidence}
                    onChange={(e) => setConfidence(parseFloat(e.target.value))}
                    className="w-full mt-2"
                  />
                  <div className="text-xs text-muted-foreground mt-1">{confidence}</div>
                </div>

                <div>
                  <label className="text-sm font-medium">IoU Threshold</label>
                  <input
                    type="range"
                    min="0.1"
                    max="1.0"
                    step="0.05"
                    value={iou}
                    onChange={(e) => setIou(parseFloat(e.target.value))}
                    className="w-full mt-2"
                  />
                  <div className="text-xs text-muted-foreground mt-1">{iou}</div>
                </div>

                <div>
                  <label className="text-sm font-medium">Max Detections</label>
                  <input
                    type="number"
                    min="1"
                    max="100"
                    value={maxDetections}
                    onChange={(e) => setMaxDetections(parseInt(e.target.value))}
                    className="w-full mt-2 px-3 py-2 border rounded-md"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Model</label>
                  <select
                    value={model}
                    onChange={(e) => setModel(e.target.value)}
                    className="w-full mt-2 px-3 py-2 border rounded-md"
                  >
                    <option value="yolov8n">YOLOv8 Nano</option>
                    <option value="yolov8s">YOLOv8 Small</option>
                    <option value="yolov8m">YOLOv8 Medium</option>
                    <option value="yolov8l">YOLOv8 Large</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Detect Button */}
            <Button
              onClick={handleDetect}
              disabled={!selectedFile || loading}
              className="w-full"
              size="lg"
            >
              {loading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Detecting...
                </>
              ) : (
                <>
                  <Target className="mr-2 h-4 w-4" />
                  Detect Objects
                </>
              )}
            </Button>

            {error && (
              <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
                <div className="flex items-center">
                  <XCircle className="h-4 w-4 text-destructive mr-2" />
                  <p className="text-sm text-destructive">{error}</p>
                </div>
              </div>
            )}
          </div>

          {/* Results Section */}
          <div className="space-y-4">
            <h3 className="text-lg font-medium">Detection Results</h3>

            {results.length === 0 ? (
              <div className="rounded-lg border border-dashed p-8 text-center text-muted-foreground">
                <Target className="mx-auto h-12 w-12 mb-4" />
                <p>No results yet</p>
                <p className="text-sm">Process an image to see detection results</p>
              </div>
            ) : (
              <div className="space-y-4 max-h-96 overflow-y-auto">
                {results.map((result, index) => (
                  <div key={index} className="rounded-lg border p-4">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center">
                        {result.success ? (
                          <CheckCircle className="h-4 w-4 text-green-500 mr-2" />
                        ) : (
                          <XCircle className="h-4 w-4 text-red-500 mr-2" />
                        )}
                        <span className="font-medium">
                          Result {results.length - index}
                        </span>
                      </div>
                    </div>

                    {result.success && result.detections ? (
                      <div className="space-y-2">
                        <div className="text-sm text-muted-foreground">
                          Found {result.detections.length} object{result.detections.length !== 1 ? 's' : ''}
                        </div>

                        {result.detections.map((detection, detIndex) => (
                          <div key={detIndex} className="flex items-center justify-between p-2 bg-muted rounded">
                            <div>
                              <span className="font-medium">{detection.class}</span>
                              <span className="text-sm text-muted-foreground ml-2">
                                ({(detection.confidence * 100).toFixed(1)}%)
                              </span>
                            </div>
                            <div className="text-xs text-muted-foreground">
                              [{detection.bbox.map(coord => coord.toFixed(3)).join(', ')}]
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-sm text-red-600">
                        {result.error || 'Detection failed'}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default YoloDetectionPage;