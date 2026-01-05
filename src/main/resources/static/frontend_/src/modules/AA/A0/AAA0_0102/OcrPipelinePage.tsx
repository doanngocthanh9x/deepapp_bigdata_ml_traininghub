import { useState, useCallback } from 'react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { Workflow, Upload, FileText, Zap, Image, Loader2, CheckCircle, XCircle, Layers } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StatsCard } from '@/components/StatsCard';
import { useApi } from '@/hooks/useApi';
import { ocrPipelineApi, fileToBase64, validateImageFile, type OcrPipelineRequest, type OcrPipelineResponse } from '@/services/aaApi';
import { cn } from '@/lib/utils';
import { useToast } from '@/hooks/use-toast';

const OcrPipelinePage = () => {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [results, setResults] = useState<OcrPipelineResponse[]>([]);
  const { toast } = useToast();

  const { execute: processPipeline, loading, error } = useApi<OcrPipelineResponse>(
    async (request: OcrPipelineRequest) => {
      return ocrPipelineApi.process(request);
    },
    {
      onSuccess: (data) => {
        setResults(prev => [data, ...prev.slice(0, 4)]); // Keep last 5 results
        toast({
          title: "OCR Pipeline Complete",
          description: `Processed ${data.boundingBoxes?.length || 0} text regions`,
        });
      },
      onError: (error) => {
        toast({
          title: "OCR Pipeline Failed",
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

    try {
      const base64 = await fileToBase64(file);
      const request: OcrPipelineRequest = {
        image: base64,
        language: 'vi',
        detectText: true,
        recognizeText: true,
      };

      await processPipeline(request);
    } catch (err) {
      toast({
        title: "File Processing Error",
        description: "Failed to process the selected file",
        variant: "destructive",
      });
    }
  }, [processPipeline, toast]);

  const handleDrop = useCallback(async (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);

    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      await handleFileSelect(files[0]);
    }
  }, [handleFileSelect]);

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        <div className="animate-fade-in">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 rounded-lg bg-secondary/10">
              <Workflow className="w-6 h-6 text-secondary" />
            </div>
            <div>
              <span className="text-xs font-mono text-muted-foreground">AAA0_0102</span>
              <h1 className="text-2xl font-bold text-foreground">OCR Pipeline</h1>
            </div>
          </div>
          <p className="text-muted-foreground">
            Advanced OCR pipeline combining text detection and recognition
          </p>
        </div>

        {/* Stats */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatsCard
            title="Processed Today"
            value={results.length.toString()}
            icon={FileText}
            trend={{ value: results.length > 0 ? 100 : 0, isPositive: true }}
          />
          <StatsCard
            title="Accuracy Rate"
            value="97.8%"
            icon={Zap}
          />
          <StatsCard
            title="Avg. Processing"
            value={loading ? "..." : "2.1s"}
            subtitle="Per image"
            icon={Image}
          />
          <StatsCard
            title="Pipeline Steps"
            value="2"
            subtitle="Detection + Recognition"
            icon={Layers}
          />
        </div>

        {/* Pipeline Description */}
        <div className="bg-card border border-border rounded-xl p-6 animate-fade-in">
          <h3 className="font-semibold text-card-foreground mb-4">Pipeline Overview</h3>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                  <span className="text-sm font-medium text-primary">1</span>
                </div>
                <div>
                  <h4 className="font-medium">Text Detection</h4>
                  <p className="text-sm text-muted-foreground">PaddleOCR detects text regions</p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                  <span className="text-sm font-medium text-primary">2</span>
                </div>
                <div>
                  <h4 className="font-medium">Text Recognition</h4>
                  <p className="text-sm text-muted-foreground">VietOCR recognizes text content</p>
                </div>
              </div>
            </div>
            <div className="bg-muted/50 rounded-lg p-4">
              <h4 className="font-medium mb-2">Benefits</h4>
              <ul className="text-sm text-muted-foreground space-y-1">
                <li>• Higher accuracy than single-stage OCR</li>
                <li>• Better handling of complex layouts</li>
                <li>• Structured text extraction</li>
                <li>• Confidence scoring per region</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Upload Area */}
        <div
          className={cn(
            'bg-card border-2 border-dashed rounded-xl p-12 text-center transition-all animate-fade-in',
            dragActive
              ? 'border-secondary bg-secondary/5'
              : 'border-border hover:border-secondary/50'
          )}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
        >
          <div className="flex flex-col items-center">
            {loading ? (
              <Loader2 className="w-8 h-8 text-secondary animate-spin mb-4" />
            ) : (
              <div className="p-4 rounded-full bg-muted mb-4">
                <Workflow className="w-8 h-8 text-muted-foreground" />
              </div>
            )}
            <h3 className="text-lg font-semibold text-card-foreground mb-2">
              {loading ? 'Processing Pipeline...' : 'Upload Image for OCR Pipeline'}
            </h3>
            <p className="text-sm text-muted-foreground mb-4">
              Advanced OCR with text detection and recognition pipeline
            </p>
            <input
              type="file"
              accept="image/*,.pdf"
              onChange={handleFileInput}
              className="hidden"
              id="pipeline-file-input"
              disabled={loading}
            />
            <label htmlFor="pipeline-file-input">
              <Button disabled={loading} asChild>
                <span className="cursor-pointer">
                  <Upload className="w-4 h-4 mr-2" />
                  Select File
                </span>
              </Button>
            </label>
            <p className="text-xs text-muted-foreground mt-4">
              Supports PNG, JPG, JPEG, PDF up to 10MB
            </p>
          </div>
        </div>

        {/* Results */}
        <div className="bg-card border border-border rounded-xl p-6 animate-fade-in">
          <h3 className="font-semibold text-card-foreground mb-4">Pipeline Results</h3>
          {results.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <Workflow className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>No recent pipeline results</p>
              <p className="text-sm">Upload an image to start the OCR pipeline</p>
            </div>
          ) : (
            <div className="space-y-4">
              {results.map((result, index) => (
                <div key={index} className="border border-border rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-3">
                    {result.success ? (
                      <CheckCircle className="w-4 h-4 text-green-500" />
                    ) : (
                      <XCircle className="w-4 h-4 text-red-500" />
                    )}
                    <span className="text-sm font-medium">
                      {result.success ? 'Pipeline Success' : 'Pipeline Failed'}
                    </span>
                    {result.confidence && (
                      <span className="text-xs text-muted-foreground">
                        Overall Confidence: {(result.confidence * 100).toFixed(1)}%
                      </span>
                    )}
                    {result.processingTime && (
                      <span className="text-xs text-muted-foreground">
                        Time: {result.processingTime.toFixed(2)}s
                      </span>
                    )}
                  </div>

                  {result.text && (
                    <div className="mb-3">
                      <h5 className="text-sm font-medium mb-2">Extracted Text:</h5>
                      <div className="bg-muted p-3 rounded text-sm">
                        {result.text}
                      </div>
                    </div>
                  )}

                  {result.boundingBoxes && result.boundingBoxes.length > 0 && (
                    <div>
                      <h5 className="text-sm font-medium mb-2">
                        Text Regions ({result.boundingBoxes.length}):
                      </h5>
                      <div className="space-y-2 max-h-40 overflow-y-auto">
                        {result.boundingBoxes.map((box, boxIndex) => (
                          <div key={boxIndex} className="bg-muted/50 p-2 rounded text-xs">
                            <div className="flex justify-between items-center">
                              <span>Region {boxIndex + 1}</span>
                              <span>{box.x}, {box.y} ({box.width}×{box.height})</span>
                            </div>
                            {box.text && (
                              <div className="mt-1 text-muted-foreground">
                                "{box.text}"
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {result.error && (
                    <div className="bg-destructive/10 text-destructive p-3 rounded text-sm">
                      {result.error}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
};

export default OcrPipelinePage;