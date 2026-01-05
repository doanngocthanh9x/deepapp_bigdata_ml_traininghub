import { useState, useCallback, useRef, useEffect } from 'react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { ScanText,FileText, Upload, Zap, Image, Loader2, CheckCircle, XCircle, Activity, Eye, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { StatsCard } from '@/components/StatsCard';
import { useApi } from '@/hooks/useApi';
import { dischargePaperOcrApi, validateImageFile } from '@/services/aaApi';
import { cn } from '@/lib/utils';
import { useToast } from '@/hooks/use-toast';

interface OcrResult {
  success: boolean;
  message?: string;
  worker?: string;
  processingTime?: number;
  regions?: Array<{
    bbox: {
      x1: number;
      y1: number;
      x2: number;
      y2: number;
    };
    confidence: number;
    label: string;
    text: string;
    ocr_success: boolean;
  }>;
  extracted_text?: string;
  detections?: number;
  error?: string;
  imageDimensions?: { width: number; height: number };
  coordinateSystem?: string; // 'original_image' or 'detection_space'
}

const DischargePaperOcrPageFixed = () => {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [results, setResults] = useState<OcrResult[]>([]);
  const [confidence, setConfidence] = useState(0.5);
  const [iou, setIou] = useState(0.45);
  const [maxDetections, setMaxDetections] = useState(50);
  const [worker, setWorker] = useState<'python' | 'cpp'>('python');
  const [imageLoading, setImageLoading] = useState(false);
  const [originalDimensions, setOriginalDimensions] = useState<{width: number, height: number} | null>(null);
  const [hoveredRegion, setHoveredRegion] = useState<number | null>(null);
  const [showBboxes, setShowBboxes] = useState(true);
  const imageContainerRef = useRef<HTMLDivElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);
  const { toast } = useToast();

  const { execute: processDischargePaper, loading, error } = useApi<OcrResult>(
    async () => {
      if (!selectedFile) throw new Error('No file selected');

      const formData = new FormData();
      formData.append('image', selectedFile);
      formData.append('confidence', confidence.toString());
      formData.append('iou', iou.toString());
      formData.append('max_detections', maxDetections.toString());
      formData.append('worker', worker);

      return dischargePaperOcrApi.process(formData);
    },
    {
      onSuccess: (data) => {
        console.log('🎯 Backend Response:', {
          coordinateSystem: data.coordinateSystem,
          imageDimensions: data.imageDimensions,
          regions: data.regions?.length,
          firstBbox: data.regions?.[0]?.bbox
        });
        setResults(prev => [data, ...prev.slice(0, 4)]);
        toast({
          title: "✅ OCR Completed",
          description: `Extracted ${data.regions?.length || 0} regions in ${data.processingTime?.toFixed(2)}s`,
        });
      },
      onError: (error) => {
        toast({
          title: "❌ OCR Failed",
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
    setImageLoading(true);
    
    try {
      const reader = new FileReader();
      const base64Promise = new Promise<string>((resolve, reject) => {
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
      
      const base64 = await base64Promise;
      
      // Get original dimensions
      const img = new window.Image();
      const dimensionsPromise = new Promise<{width: number, height: number}>((resolve, reject) => {
        img.onload = () => {
          const dims = {
            width: img.naturalWidth,
            height: img.naturalHeight
          };
          console.log('📐 Loaded Image Dimensions:', dims);
          resolve(dims);
          setImageLoading(false);
        };
        img.onerror = reject;
        img.src = base64;
      });
      
      setImagePreview(base64);
      const dims = await dimensionsPromise;
      setOriginalDimensions(dims);
      
      toast({
        title: "📸 File Loaded",
        description: `${file.name} (${dims.width}×${dims.height})`,
      });
    } catch (error) {
      console.error('Failed to load image:', error);
      setImageLoading(false);
      toast({
        title: "Error",
        description: "Failed to load image",
        variant: "destructive",
      });
    }
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

  const handleProcess = useCallback(() => {
    if (!selectedFile) {
      toast({
        title: "No File Selected",
        description: "Please select an image file first",
        variant: "destructive",
      });
      return;
    }
    processDischargePaper();
  }, [selectedFile, processDischargePaper, toast]);

  
  // Render bounding boxes - FIXED FOR ORIGINAL IMAGE COORDINATES WITH PROPER CENTERING
  const renderBoundingBoxes = () => {
    if (!showBboxes || results.length === 0 || !results[0].success || !results[0].regions) return null;
    if (!imageRef.current || !imageContainerRef.current) return null;

    const result = results[0];
    const img = imageRef.current;
    const container = imageContainerRef.current;

    // Get actual displayed dimensions
    const imgRect = img.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();

    // Original image dimensions
    const originalWidth = result.imageDimensions?.width || originalDimensions?.width || img.naturalWidth;
    const originalHeight = result.imageDimensions?.height || originalDimensions?.height || img.naturalHeight;

    // Calculate the actual displayed size with object-contain
    // object-contain maintains aspect ratio, so we need to find which dimension is constrained
    const containerWidth = containerRect.width;
    const containerHeight = containerRect.height;
    
    const containerAspect = containerWidth / containerHeight;
    const imageAspect = originalWidth / originalHeight;
    
    let displayedWidth, displayedHeight;
    if (imageAspect > containerAspect) {
      // Image is wider - constrained by width
      displayedWidth = containerWidth;
      displayedHeight = containerWidth / imageAspect;
    } else {
      // Image is taller - constrained by height
      displayedHeight = containerHeight;
      displayedWidth = containerHeight * imageAspect;
    }

    // Calculate centering offset
    const offsetX = (containerWidth - displayedWidth) / 2;
    const offsetY = (containerHeight - displayedHeight) / 2;

    // Calculate scale factors from ORIGINAL to DISPLAYED
    const scaleX = displayedWidth / originalWidth;
    const scaleY = displayedHeight / originalHeight;

    console.log('🖼️ Render Info:', {
      coordinateSystem: result.coordinateSystem,
      originalSize: `${originalWidth}×${originalHeight}`,
      containerSize: `${containerWidth.toFixed(0)}×${containerHeight.toFixed(0)}`,
      displayedSize: `${displayedWidth.toFixed(0)}×${displayedHeight.toFixed(0)}`,
      offset: `X=${offsetX.toFixed(1)}, Y=${offsetY.toFixed(1)}`,
      scale: `X=${scaleX.toFixed(3)}, Y=${scaleY.toFixed(3)}`
    });

    return result.regions.map((region, idx) => {
      const bbox = region.bbox;

      // Backend returns coords in ORIGINAL IMAGE SPACE
      // Scale to displayed size
      const displayX1 = bbox.x1 * scaleX;
      const displayY1 = bbox.y1 * scaleY;
      const displayX2 = bbox.x2 * scaleX;
      const displayY2 = bbox.y2 * scaleY;

      // Apply centering offset
      const left = offsetX + displayX1;
      const top = offsetY + displayY1;
      const width = displayX2 - displayX1;
      const height = displayY2 - displayY1;

      if (idx === 0) {
        console.log(`📦 Region #${idx} Calculation:`, {
          'Backend bbox': `(${bbox.x1}, ${bbox.y1}) → (${bbox.x2}, ${bbox.y2})`,
          'After scaling': `(${displayX1.toFixed(1)}, ${displayY1.toFixed(1)}) → (${displayX2.toFixed(1)}, ${displayY2.toFixed(1)})`,
          'After offset': `left=${left.toFixed(1)}px, top=${top.toFixed(1)}px`,
          'Final size': `${width.toFixed(1)}×${height.toFixed(1)}px`
        });
      }

      return (
        <div
          key={idx}
          className={cn(
            "absolute border-2 pointer-events-auto cursor-pointer transition-all duration-300",
            hoveredRegion === idx 
              ? "border-red-500 bg-red-500/30 shadow-[0_0_20px_rgba(239,68,68,0.5)] scale-105 z-20" 
              : "border-blue-500 bg-blue-500/15 hover:bg-blue-500/25"
          )}
          style={{
            left: `${left}px`,
            top: `${top}px`,
            width: `${width}px`,
            height: `${height}px`,
          }}
          onMouseEnter={() => setHoveredRegion(idx)}
          onMouseLeave={() => setHoveredRegion(null)}
          title={`${region.label}: ${region.text}`}
        >
          {/* Region label */}
          <div className={cn(
            "absolute -top-6 left-0 text-xs font-medium px-2 py-0.5 rounded whitespace-nowrap transition-all",
            hoveredRegion === idx 
              ? "bg-red-500 text-white" 
              : "bg-blue-500 text-white"
          )}>
            #{idx + 1} • {(region.confidence * 100).toFixed(0)}%
          </div>
        </div>
      );
    });
  };

  const currentResult = results.length > 0 ? results[0] : null;

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        {/* Animated Header */}
        {/* <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 p-8 text-white">
          <div className="absolute inset-0 bg-black/20"></div>
          <div className="relative z-10 flex items-center justify-between">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <Sparkles className="h-8 w-8 animate-pulse" />
                <h1 className="text-4xl font-bold tracking-tight">Discharge Paper OCR</h1>
              </div>
              <p className="text-white/90 text-lg">
                AI-powered text extraction • Coordinates in original image space
              </p>
              <div className="flex items-center gap-4 mt-4 text-sm">
                <span className="flex items-center gap-1.5 bg-white/20 px-3 py-1 rounded-full">
                  <Zap className="h-3.5 w-3.5" />
                  YOLO Detection
                </span>
                <span className="flex items-center gap-1.5 bg-white/20 px-3 py-1 rounded-full">
                  <Eye className="h-3.5 w-3.5" />
                  VietOCR
                </span>
              </div>
            </div>
            <Activity className="h-24 w-24 opacity-30" />
          </div>
        </div> */}
<div className="animate-fade-in">
    <div className="flex items-center gap-3 mb-2">
        <div className="p-2 rounded-lg bg-primary/10">
            <ScanText className="w-6 h-6 text-primary" />
        </div>
        <div>
            <span className="text-xs font-mono text-muted-foreground">AAA0_0103</span>
            <h1 className="text-2xl font-bold text-foreground">Discharge Paper OCR</h1>
        </div>
    </div>
    <p className="text-muted-foreground">
        AI-powered text extraction • Coordinates in original image space
    </p>
</div>
        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-5">
          <StatsCard
            title="Processed"
            value={results.length.toString()}
            icon={FileText}
            description="Total documents"
          />
          <StatsCard
            title="Text Regions"
            value={results.reduce((acc, r) => acc + (r.regions?.length || 0), 0).toString()}
            icon={Image}
            description="Detected areas"
          />
          <StatsCard
            title="Success Rate"
            value={results.length > 0 ? `${((results.filter(r => r.success).length / results.length) * 100).toFixed(0)}%` : '0%'}
            icon={CheckCircle}
            description="Extraction accuracy"
          />
          <StatsCard
            title="Avg Confidence"
            value={currentResult?.regions ? `${(currentResult.regions.reduce((acc, r) => acc + r.confidence, 0) / currentResult.regions.length * 100).toFixed(0)}%` : '0%'}
            icon={Zap}
            description="Detection quality"
          />
          <StatsCard
            title="Processing Time"
            value={currentResult?.processingTime ? `${currentResult.processingTime.toFixed(2)}s` : '0s'}
            icon={Activity}
            description="Latest result"
          />
        </div>

        {/* Main Layout: 2 Columns */}
        <div className="grid gap-6 lg:grid-cols-2">
          {/* LEFT COLUMN: Image Preview */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xl font-semibold">Document Preview</h3>
              {currentResult && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowBboxes(!showBboxes)}
                  className="gap-2"
                >
                  <Eye className="h-4 w-4" />
                  {showBboxes ? 'Hide' : 'Show'} Boxes
                </Button>
              )}
            </div>
            
            {imagePreview ? (
              <div className="relative rounded-xl border-2 border-gray-200 overflow-hidden bg-gradient-to-br from-gray-50 to-gray-100 shadow-lg">
                {imageLoading && (
                  <div className="absolute inset-0 bg-white dark:bg-gray-900 dark:text-white flex items-center justify-center z-30">
                    <Loader2 className="h-12 w-12 animate-spin text-blue-500" />
                  </div>
                )}
                
                <div ref={imageContainerRef} className="relative">
                  <img
                    ref={imageRef}
                    src={imagePreview}
                    alt="Document"
                    className="w-full h-auto object-contain"
                    style={{ maxHeight: '70vh' }}
                  />
                  
                  {/* Bounding boxes overlay */}
                  {renderBoundingBoxes()}
                </div>

                {/* Image info bar */}
                {originalDimensions && (
                  <div className="bg-gray-900 text-white text-xs px-4 py-2 flex items-center justify-between">
                    <span>📐 {originalDimensions.width} × {originalDimensions.height}</span>
                    {currentResult && (
                      <span className="text-green-400">
                        ✓ {currentResult.regions?.length || 0} regions • {currentResult.coordinateSystem || 'unknown'}
                      </span>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div
                className={cn(
                  "rounded-xl border-2 border-dashed p-16 text-center transition-all cursor-pointer",
                  dragActive 
                    ? "border-blue-500 bg-blue-50" 
                    : "border-gray-300 bg-gray-50 hover:border-gray-400 hover:bg-gray-100"
                )}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
                onClick={() => document.getElementById('file-input')?.click()}
              >
                <Upload className="mx-auto h-16 w-16 text-gray-400 mb-4" />
                <p className="text-lg font-medium text-gray-700 mb-2">
                  Drop your discharge paper here
                </p>
                <p className="text-sm text-gray-500">
                  or click to browse • PNG, JPG, JPEG up to 10MB
                </p>
                <input
                  id="file-input"
                  type="file"
                  accept="image/*"
                  onChange={handleFileInput}
                  className="hidden"
                />
              </div>
            )}
          </div>

          {/* RIGHT COLUMN: Controls & Results */}
          <div className="space-y-4">
            {/* Upload Info */}
            {selectedFile && (
              <div className="rounded-lg border bg-blue-50 border-blue-200 p-4">
                <div className="flex items-center gap-3">
                  <div className="h-12 w-12 rounded-lg bg-blue-100 flex items-center justify-center">
                    <FileText className="h-6 w-6 text-blue-600" />
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">{selectedFile.name}</p>
                    <p className="text-sm text-gray-600">
                      {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Parameters */}
            <div className="rounded-xl border bg-white shadow-sm p-6 space-y-4 bg-white dark:bg-gray-900 dark:text-white " >
              <h4 className="font-semibold text-lg flex items-center gap-2 rounded-md px-2 py-1">
                <Zap className="h-5 w-5 text-blue-500" />
                OCR Parameters
              </h4>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="confidence" className="text-sm font-medium">
                    Confidence
                  </Label>
                  <Input
                    id="confidence"
                    type="number"
                    min="0"
                    max="1"
                    step="0.1"
                    value={confidence}
                    onChange={(e) => setConfidence(parseFloat(e.target.value))}
                    className="mt-1.5"
                  />
                </div>
                <div>
                  <Label htmlFor="iou" className="text-sm font-medium">
                    IoU Threshold
                  </Label>
                  <Input
                    id="iou"
                    type="number"
                    min="0"
                    max="1"
                    step="0.05"
                    value={iou}
                    onChange={(e) => setIou(parseFloat(e.target.value))}
                    className="mt-1.5"
                  />
                </div>
              </div>
              
              <div>
                <Label htmlFor="maxDetections" className="text-sm font-medium">
                  Max Detections
                </Label>
                <Input
                  id="maxDetections"
                  type="number"
                  min="1"
                  max="100"
                  value={maxDetections}
                  onChange={(e) => setMaxDetections(parseInt(e.target.value))}
                  className="mt-1.5"
                />
              </div>
              
              <div className="">
                <Label htmlFor="worker" className="text-sm font-medium ">
                  Worker Type
                </Label>
                <select
                  id="worker"
                  value={worker}
                  onChange={(e) => setWorker(e.target.value as 'python' | 'cpp')}
                  className="w-full mt-1.5 px-3 py-2 border border-gray-300 bg-white dark:bg-gray-900 dark:text-white  rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="python">Python Worker</option>
                  <option value="cpp">C++ Worker</option>
                </select>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="space-y-3">
              <Button
                onClick={handleProcess}
                disabled={!selectedFile || loading}
                className="w-full h-12 text-lg font-semibold bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700"
                size="lg"
              >
                {loading ? (
                  <>
                    <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                    Processing...
                  </>
                ) : (
                  <>
                    <Sparkles className="mr-2 h-5 w-5" />
                    Extract Text
                  </>
                )}
              </Button>

           
            </div>

            {error && (
              <div className="rounded-lg border border-red-200 bg-red-50 p-4">
                <div className="flex items-center gap-2 text-red-800">
                  <XCircle className="h-5 w-5" />
                  <p className="text-sm font-medium">{error}</p>
                </div>
              </div>
            )}

            {/* Results */}
            <div className="rounded-xl border bg-white dark:bg-gray-900 dark:text-white  shadow-sm p-6 max-h-[600px] overflow-y-auto">
              <h4 className="font-semibold text-lg mb-4 flex items-center gap-2 pb-2">
                <FileText className="h-5 w-5 text-green-500" />
                Extracted Text
              </h4>
              
              {currentResult ? (
                <div className="space-y-4">
                  {/* Full extracted text */}
                  {currentResult.extracted_text && (
                    <div className="rounded-lg bg-gray-50 border p-4">
                      <p className="text-sm text-gray-700 whitespace-pre-wrap font-mono">
                        {currentResult.extracted_text}
                      </p>
                    </div>
                  )}

                  {/* Regions list */}
                  {currentResult.regions && currentResult.regions.length > 0 && (
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-gray-600">
                        {currentResult.regions.length} Text Regions:
                      </p>
                      {currentResult.regions.map((region, idx) => (
                        <div
                          key={idx}
                          className={cn(
                            "rounded-lg border p-3 cursor-pointer transition-all",
                            hoveredRegion === idx
                              ? "bg-red-50 border-red-300 shadow-md"
                              : "bg-white border-gray-200 hover:border-gray-300"
                          )}
                          onMouseEnter={() => setHoveredRegion(idx)}
                          onMouseLeave={() => setHoveredRegion(null)}
                        >
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-xs font-semibold text-gray-500">
                              #{idx + 1} • {region.label}
                            </span>
                            <span className={cn(
                              "text-xs font-medium px-2 py-0.5 rounded-full",
                              region.confidence > 0.8 
                                ? "bg-green-100 text-green-700"
                                : region.confidence > 0.6
                                ? "bg-yellow-100 text-yellow-700"
                                : "bg-red-100 text-red-700"
                            )}>
                              {(region.confidence * 100).toFixed(0)}%
                            </span>
                          </div>
                          <p className="text-sm text-gray-800 font-medium">
                            {region.text || '(empty)'}
                          </p>
                          <p className="text-xs text-gray-400 mt-1">
                            [{region.bbox.x1.toFixed(0)}, {region.bbox.y1.toFixed(0)}] → [{region.bbox.x2.toFixed(0)}, {region.bbox.y2.toFixed(0)}]
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <div className="text-center py-12 text-gray-400">
                  <FileText className="mx-auto h-12 w-12 mb-3 opacity-50" />
                  <p>No results yet</p>
                  <p className="text-sm mt-1">Upload and process a document</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default DischargePaperOcrPageFixed;