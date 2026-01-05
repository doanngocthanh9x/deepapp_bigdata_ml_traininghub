import { useState, useCallback } from 'react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { ScanText, Upload, FileText, Zap, Image, Loader2, CheckCircle, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StatsCard } from '@/components/StatsCard';
import { useApi } from '@/hooks/useApi';
import { ocrApi, fileToBase64, validateImageFile, type OcrRequest, type OcrResponse } from '@/services/aaApi';
import { cn } from '@/lib/utils';
import { useToast } from '@/hooks/use-toast';

const OcrPage = () => {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedEngine, setSelectedEngine] = useState<'vietocr' | 'paddleocr'>('vietocr');
  const [results, setResults] = useState<OcrResponse[]>([]);
  const { toast } = useToast();

  const { execute: processOcr, loading, error } = useApi<OcrResponse>(
    async (request: OcrRequest) => {
      if (selectedEngine === 'vietocr') {
        return ocrApi.processVietOcr(request);
      } else {
        return ocrApi.processPaddleOcr(request);
      }
    },
    {
      onSuccess: (data) => {
        setResults(prev => [data, ...prev.slice(0, 4)]); // Keep last 5 results
        toast({
          title: "OCR Complete",
          description: `Extracted ${data.text?.length || 0} characters`,
        });
      },
      onError: (error) => {
        toast({
          title: "OCR Failed",
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
      const request: OcrRequest = {
        engine: selectedEngine,
        language: 'vi',
        image: base64,
      };

      await processOcr(request);
    } catch (err) {
      toast({
        title: "File Processing Error",
        description: "Failed to process the selected file",
        variant: "destructive",
      });
    }
  }, [selectedEngine, processOcr, toast]);

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
            <div className="p-2 rounded-lg bg-primary/10">
              <ScanText className="w-6 h-6 text-primary" />
            </div>
            <div>
              <span className="text-xs font-mono text-muted-foreground">AAA0_0101</span>
              <h1 className="text-2xl font-bold text-foreground">OCR Services</h1>
            </div>
          </div>
          <p className="text-muted-foreground">
            Optical Character Recognition for image-to-text extraction
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
            value="98.5%"
            icon={Zap}
          />
          <StatsCard
            title="Avg. Processing"
            value={loading ? "..." : "1.2s"}
            subtitle="Per image"
            icon={Image}
          />
          <StatsCard
            title="Engine"
            value={selectedEngine.toUpperCase()}
            icon={ScanText}
          />
        </div>

        {/* Engine Selection */}
        <div className="bg-card border border-border rounded-xl p-6 animate-fade-in">
          <h3 className="font-semibold text-card-foreground mb-4">OCR Engine</h3>
          <div className="flex gap-4">
            <Button
              variant={selectedEngine === 'vietocr' ? 'default' : 'outline'}
              onClick={() => setSelectedEngine('vietocr')}
              className="flex-1"
            >
              VietOCR
            </Button>
            <Button
              variant={selectedEngine === 'paddleocr' ? 'default' : 'outline'}
              onClick={() => setSelectedEngine('paddleocr')}
              className="flex-1"
            >
              PaddleOCR
            </Button>
          </div>
        </div>

        {/* Upload Area */}
        <div
          className={cn(
            'bg-card border-2 border-dashed rounded-xl p-12 text-center transition-all animate-fade-in',
            dragActive
              ? 'border-primary bg-primary/5'
              : 'border-border hover:border-primary/50'
          )}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
        >
          <div className="flex flex-col items-center">
            {loading ? (
              <Loader2 className="w-8 h-8 text-primary animate-spin mb-4" />
            ) : (
              <div className="p-4 rounded-full bg-muted mb-4">
                <Upload className="w-8 h-8 text-muted-foreground" />
              </div>
            )}
            <h3 className="text-lg font-semibold text-card-foreground mb-2">
              {loading ? 'Processing...' : 'Upload Image for OCR'}
            </h3>
            <p className="text-sm text-muted-foreground mb-4">
              Drag and drop your image here, or click to browse
            </p>
            <input
              type="file"
              accept="image/*,.pdf"
              onChange={handleFileInput}
              className="hidden"
              id="file-input"
              disabled={loading}
            />
            <label htmlFor="file-input">
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
          <h3 className="font-semibold text-card-foreground mb-4">Recent Results</h3>
          {results.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <FileText className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>No recent OCR results</p>
              <p className="text-sm">Upload an image to get started</p>
            </div>
          ) : (
            <div className="space-y-4">
              {results.map((result, index) => (
                <div key={index} className="border border-border rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    {result.success ? (
                      <CheckCircle className="w-4 h-4 text-green-500" />
                    ) : (
                      <XCircle className="w-4 h-4 text-red-500" />
                    )}
                    <span className="text-sm font-medium">
                      {result.success ? 'Success' : 'Failed'}
                    </span>
                    {result.confidence && (
                      <span className="text-xs text-muted-foreground">
                        Confidence: {(result.confidence * 100).toFixed(1)}%
                      </span>
                    )}
                  </div>
                  {result.text && (
                    <div className="bg-muted p-3 rounded text-sm">
                      {result.text}
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

export default OcrPage;
