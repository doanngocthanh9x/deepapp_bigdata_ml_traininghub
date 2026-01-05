import React, { useState, useRef, useEffect, useCallback } from 'react';
import { 
  MousePointer2, 
  Square, 
  Type, 
  Undo2, 
  Redo2, 
  ZoomIn, 
  ZoomOut, 
  RotateCcw,
  Trash2,
  Move
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select';
import { cn } from '@/lib/utils';
import type { Dataset, Annotation } from '@/services/aab0Api';

interface AnnotatorCanvasProps {
  dataset?: Dataset | null;
  annotations: Annotation[];
  onAnnotationsChange: (annotations: Annotation[]) => void;
  projectType?: 'YOLO' | 'OCR';
}

type Tool = 'select' | 'bbox' | 'ocr_region' | 'pan';

interface BBox {
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  type: 'bbox' | 'ocr_region';
}

const CLASSES = ['object', 'text', 'table', 'figure', 'header', 'footer', 'paragraph'];

const AnnotatorCanvas: React.FC<AnnotatorCanvasProps> = ({
  dataset,
  annotations,
  onAnnotationsChange,
  projectType = 'YOLO',
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [tool, setTool] = useState<Tool>('select');
  const [selectedClass, setSelectedClass] = useState(CLASSES[0]);
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [bboxes, setBboxes] = useState<BBox[]>([]);
  const [selectedBbox, setSelectedBbox] = useState<string | null>(null);
  const [drawing, setDrawing] = useState(false);
  const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  const [currentRect, setCurrentRect] = useState<BBox | null>(null);
  const [history, setHistory] = useState<BBox[][]>([[]]);
  const [historyIndex, setHistoryIndex] = useState(0);
  const [image, setImage] = useState<HTMLImageElement | null>(null);
  const [isPanning, setIsPanning] = useState(false);
  const [lastPanPos, setLastPanPos] = useState({ x: 0, y: 0 });

  // Load image when dataset changes
  useEffect(() => {
    if (dataset?.url) {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        setImage(img);
        setZoom(1);
        setPan({ x: 0, y: 0 });
      };
      img.src = dataset.url;
    } else {
      setImage(null);
    }
  }, [dataset?.url]);

  // Convert annotations to bboxes
  useEffect(() => {
    const newBboxes = annotations.map(a => ({
      id: a.id,
      x: a.coordinates.x,
      y: a.coordinates.y,
      width: a.coordinates.width,
      height: a.coordinates.height,
      label: a.label,
      type: a.type,
    }));
    setBboxes(newBboxes);
  }, [annotations]);

  // Render canvas
  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;

    const container = containerRef.current;
    if (container) {
      canvas.width = container.clientWidth;
      canvas.height = container.clientHeight;
    }

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw background
    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw image
    if (image) {
      ctx.save();
      ctx.translate(pan.x, pan.y);
      ctx.scale(zoom, zoom);
      ctx.drawImage(image, 0, 0);

      // Draw bboxes
      bboxes.forEach((bbox) => {
        const isSelected = selectedBbox === bbox.id;
        ctx.strokeStyle = bbox.type === 'bbox' ? '#22c55e' : '#3b82f6';
        ctx.lineWidth = isSelected ? 3 / zoom : 2 / zoom;
        ctx.setLineDash(isSelected ? [] : [5 / zoom, 5 / zoom]);
        ctx.strokeRect(bbox.x, bbox.y, bbox.width, bbox.height);

        // Draw label
        ctx.fillStyle = bbox.type === 'bbox' ? 'rgba(34, 197, 94, 0.8)' : 'rgba(59, 130, 246, 0.8)';
        const labelHeight = 16 / zoom;
        ctx.fillRect(bbox.x, bbox.y - labelHeight, bbox.label.length * 8 / zoom + 8 / zoom, labelHeight);
        ctx.fillStyle = '#fff';
        ctx.font = `${12 / zoom}px sans-serif`;
        ctx.fillText(bbox.label, bbox.x + 4 / zoom, bbox.y - 4 / zoom);
      });

      // Draw current drawing rectangle
      if (currentRect) {
        ctx.strokeStyle = tool === 'bbox' ? '#22c55e' : '#3b82f6';
        ctx.lineWidth = 2 / zoom;
        ctx.setLineDash([]);
        ctx.strokeRect(currentRect.x, currentRect.y, currentRect.width, currentRect.height);
      }

      ctx.restore();
    } else {
      // No image placeholder
      ctx.fillStyle = '#64748b';
      ctx.font = '16px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('Select an image from the dataset', canvas.width / 2, canvas.height / 2);
    }
  }, [image, zoom, pan, bboxes, selectedBbox, currentRect, tool]);

  const getMousePos = (e: React.MouseEvent) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    return {
      x: (e.clientX - rect.left - pan.x) / zoom,
      y: (e.clientY - rect.top - pan.y) / zoom,
    };
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    if (!image) return;

    const pos = getMousePos(e);

    if (tool === 'pan') {
      setIsPanning(true);
      setLastPanPos({ x: e.clientX, y: e.clientY });
      return;
    }

    if (tool === 'select') {
      // Check if clicking on a bbox
      const clicked = bboxes.find(
        b => pos.x >= b.x && pos.x <= b.x + b.width && pos.y >= b.y && pos.y <= b.y + b.height
      );
      setSelectedBbox(clicked?.id || null);
      return;
    }

    if (tool === 'bbox' || tool === 'ocr_region') {
      setDrawing(true);
      setStartPos(pos);
      setCurrentRect({
        id: `temp-${Date.now()}`,
        x: pos.x,
        y: pos.y,
        width: 0,
        height: 0,
        label: selectedClass,
        type: tool,
      });
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (isPanning && tool === 'pan') {
      const dx = e.clientX - lastPanPos.x;
      const dy = e.clientY - lastPanPos.y;
      setPan(prev => ({ x: prev.x + dx, y: prev.y + dy }));
      setLastPanPos({ x: e.clientX, y: e.clientY });
      return;
    }

    if (!drawing || !currentRect) return;

    const pos = getMousePos(e);
    setCurrentRect({
      ...currentRect,
      width: pos.x - startPos.x,
      height: pos.y - startPos.y,
    });
  };

  const handleMouseUp = () => {
    setIsPanning(false);

    if (!drawing || !currentRect) return;

    setDrawing(false);

    // Normalize rectangle (handle negative width/height)
    const normalized: BBox = {
      ...currentRect,
      id: `bbox-${Date.now()}`,
      x: currentRect.width < 0 ? currentRect.x + currentRect.width : currentRect.x,
      y: currentRect.height < 0 ? currentRect.y + currentRect.height : currentRect.y,
      width: Math.abs(currentRect.width),
      height: Math.abs(currentRect.height),
    };

    // Only add if size is significant
    if (normalized.width > 5 && normalized.height > 5) {
      const newBboxes = [...bboxes, normalized];
      setBboxes(newBboxes);
      saveToHistory(newBboxes);
      syncAnnotations(newBboxes);
    }

    setCurrentRect(null);
  };

  const saveToHistory = (newBboxes: BBox[]) => {
    const newHistory = history.slice(0, historyIndex + 1);
    newHistory.push(newBboxes);
    setHistory(newHistory);
    setHistoryIndex(newHistory.length - 1);
  };

  const syncAnnotations = (newBboxes: BBox[]) => {
    const newAnnotations: Annotation[] = newBboxes.map(b => ({
      id: b.id,
      datasetId: dataset?.id || '',
      type: b.type,
      coordinates: {
        x: b.x,
        y: b.y,
        width: b.width,
        height: b.height,
      },
      label: b.label,
    }));
    onAnnotationsChange(newAnnotations);
  };

  const handleUndo = () => {
    if (historyIndex > 0) {
      const newIndex = historyIndex - 1;
      setHistoryIndex(newIndex);
      setBboxes(history[newIndex]);
      syncAnnotations(history[newIndex]);
    }
  };

  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      const newIndex = historyIndex + 1;
      setHistoryIndex(newIndex);
      setBboxes(history[newIndex]);
      syncAnnotations(history[newIndex]);
    }
  };

  const handleZoomIn = () => setZoom(z => Math.min(z * 1.2, 5));
  const handleZoomOut = () => setZoom(z => Math.max(z / 1.2, 0.2));
  const handleReset = () => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  };

  const handleDelete = () => {
    if (selectedBbox) {
      const newBboxes = bboxes.filter(b => b.id !== selectedBbox);
      setBboxes(newBboxes);
      saveToHistory(newBboxes);
      syncAnnotations(newBboxes);
      setSelectedBbox(null);
    }
  };

  return (
    <div className="bg-card border border-border rounded-xl flex flex-col h-full">
      {/* Toolbar */}
      <div className="flex items-center gap-2 p-3 border-b border-border flex-wrap">
        <div className="flex items-center gap-1 border-r border-border pr-2">
          <Button
            variant={tool === 'select' ? 'default' : 'ghost'}
            size="icon"
            className="h-8 w-8"
            onClick={() => setTool('select')}
            title="Select"
          >
            <MousePointer2 className="w-4 h-4" />
          </Button>
          <Button
            variant={tool === 'bbox' ? 'default' : 'ghost'}
            size="icon"
            className="h-8 w-8"
            onClick={() => setTool('bbox')}
            title="YOLO Bounding Box"
          >
            <Square className="w-4 h-4" />
          </Button>
          <Button
            variant={tool === 'ocr_region' ? 'default' : 'ghost'}
            size="icon"
            className="h-8 w-8"
            onClick={() => setTool('ocr_region')}
            title="OCR Region"
          >
            <Type className="w-4 h-4" />
          </Button>
          <Button
            variant={tool === 'pan' ? 'default' : 'ghost'}
            size="icon"
            className="h-8 w-8"
            onClick={() => setTool('pan')}
            title="Pan"
          >
            <Move className="w-4 h-4" />
          </Button>
        </div>

        <Select value={selectedClass} onValueChange={setSelectedClass}>
          <SelectTrigger className="w-32 h-8">
            <SelectValue placeholder="Class" />
          </SelectTrigger>
          <SelectContent>
            {CLASSES.map(c => (
              <SelectItem key={c} value={c}>{c}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex items-center gap-1 border-l border-border pl-2">
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handleUndo} disabled={historyIndex <= 0}>
            <Undo2 className="w-4 h-4" />
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handleRedo} disabled={historyIndex >= history.length - 1}>
            <Redo2 className="w-4 h-4" />
          </Button>
        </div>

        <div className="flex items-center gap-1 border-l border-border pl-2">
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handleZoomOut}>
            <ZoomOut className="w-4 h-4" />
          </Button>
          <span className="text-xs w-12 text-center">{Math.round(zoom * 100)}%</span>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handleZoomIn}>
            <ZoomIn className="w-4 h-4" />
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handleReset}>
            <RotateCcw className="w-4 h-4" />
          </Button>
        </div>

        <div className="flex-1" />

        <Button 
          variant="destructive" 
          size="sm" 
          onClick={handleDelete} 
          disabled={!selectedBbox}
        >
          <Trash2 className="w-4 h-4 mr-1" />
          Delete
        </Button>
      </div>

      {/* Canvas */}
      <div ref={containerRef} className="flex-1 overflow-hidden relative">
        <canvas
          ref={canvasRef}
          className={cn(
            'w-full h-full',
            tool === 'pan' ? 'cursor-grab' : tool === 'select' ? 'cursor-default' : 'cursor-crosshair',
            isPanning && 'cursor-grabbing'
          )}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
        />
      </div>

      {/* Status bar */}
      <div className="flex items-center justify-between px-3 py-2 border-t border-border text-xs text-muted-foreground">
        <span>
          {bboxes.length} annotation{bboxes.length !== 1 ? 's' : ''}
          {selectedBbox && ' • 1 selected'}
        </span>
        <span>
          {dataset?.name || 'No image selected'}
        </span>
      </div>
    </div>
  );
};

export default AnnotatorCanvas;
