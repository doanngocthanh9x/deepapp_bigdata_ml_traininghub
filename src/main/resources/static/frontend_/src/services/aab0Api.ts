// API Service for LabelStudio Module (AAB0_0100)
// All functions are stubs that return mock data

export interface Model {
  id: string;
  name: string;
  type: 'YOLO' | 'OCR';
  version: string;
  createdAt: string;
  accuracy?: number;
}

export interface Dataset {
  id: string;
  name: string;
  type: 'image' | 'pdf';
  url: string;
  thumbnailUrl?: string;
  annotationCount: number;
  status: 'pending' | 'annotated' | 'reviewed';
  createdAt: string;
}

export interface Annotation {
  id: string;
  datasetId: string;
  type: 'bbox' | 'ocr_region';
  coordinates: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
  label: string;
  confidence?: number;
  text?: string; // For OCR
}

export interface Project {
  id: string;
  name: string;
  type: 'YOLO' | 'OCR';
  modelId?: string;
  datasetCount: number;
  annotationCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ActivityLog {
  id: string;
  action: 'create' | 'annotate' | 'export' | 'inference' | 'upload';
  description: string;
  userId: string;
  userName: string;
  timestamp: string;
  metadata?: Record<string, unknown>;
}

export interface InferenceResult {
  annotations: Annotation[];
  processingTime: number;
  modelUsed: string;
}

// Mock delay helper
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

// API Functions

export const getModels = async (): Promise<Model[]> => {
  await delay(500);
  return [
    {
      id: 'model-1',
      name: 'YOLOv8-nano',
      type: 'YOLO',
      version: '1.0.0',
      createdAt: '2024-01-15T10:00:00Z',
      accuracy: 0.92,
    },
    {
      id: 'model-2',
      name: 'YOLOv8-small',
      type: 'YOLO',
      version: '1.1.0',
      createdAt: '2024-02-01T14:30:00Z',
      accuracy: 0.95,
    },
    {
      id: 'model-3',
      name: 'PaddleOCR-v3',
      type: 'OCR',
      version: '3.0.0',
      createdAt: '2024-01-20T09:00:00Z',
      accuracy: 0.98,
    },
    {
      id: 'model-4',
      name: 'TrOCR-base',
      type: 'OCR',
      version: '2.0.0',
      createdAt: '2024-02-10T11:00:00Z',
      accuracy: 0.96,
    },
  ];
};

export const uploadModel = async (file: File): Promise<Model> => {
  await delay(1500);
  return {
    id: `model-${Date.now()}`,
    name: file.name.replace(/\.[^.]+$/, ''),
    type: 'YOLO',
    version: '1.0.0',
    createdAt: new Date().toISOString(),
  };
};

export const getDatasets = async (page: number = 1, pageSize: number = 10): Promise<{
  data: Dataset[];
  total: number;
  page: number;
  pageSize: number;
}> => {
  await delay(400);
  const mockDatasets: Dataset[] = Array.from({ length: 25 }, (_, i) => ({
    id: `dataset-${i + 1}`,
    name: `Image_${String(i + 1).padStart(3, '0')}.jpg`,
    type: 'image' as const,
    url: `https://picsum.photos/seed/${i + 1}/800/600`,
    thumbnailUrl: `https://picsum.photos/seed/${i + 1}/200/150`,
    annotationCount: Math.floor(Math.random() * 10),
    status: ['pending', 'annotated', 'reviewed'][Math.floor(Math.random() * 3)] as Dataset['status'],
    createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
  }));

  const start = (page - 1) * pageSize;
  const end = start + pageSize;

  return {
    data: mockDatasets.slice(start, end),
    total: mockDatasets.length,
    page,
    pageSize,
  };
};

export const uploadDataset = async (files: File[]): Promise<Dataset[]> => {
  await delay(1000);
  return files.map((file, index) => ({
    id: `dataset-${Date.now()}-${index}`,
    name: file.name,
    type: file.type.includes('pdf') ? 'pdf' as const : 'image' as const,
    url: URL.createObjectURL(file),
    thumbnailUrl: URL.createObjectURL(file),
    annotationCount: 0,
    status: 'pending' as const,
    createdAt: new Date().toISOString(),
  }));
};

export const saveAnnotations = async (datasetId: string, annotations: Annotation[]): Promise<boolean> => {
  await delay(300);
  console.log('Saving annotations for dataset:', datasetId, annotations);
  return true;
};

export const runInference = async (datasetId: string, modelId: string): Promise<InferenceResult> => {
  await delay(2000);
  return {
    annotations: [
      {
        id: `anno-${Date.now()}-1`,
        datasetId,
        type: 'bbox',
        coordinates: { x: 100, y: 100, width: 200, height: 150 },
        label: 'object',
        confidence: 0.95,
      },
      {
        id: `anno-${Date.now()}-2`,
        datasetId,
        type: 'bbox',
        coordinates: { x: 350, y: 200, width: 180, height: 120 },
        label: 'text',
        confidence: 0.88,
      },
    ],
    processingTime: 1.5,
    modelUsed: modelId,
  };
};

export const exportAnnotations = async (
  projectId: string,
  format: 'yolo' | 'coco' | 'ocr_json' | 'hocr'
): Promise<Blob> => {
  await delay(1000);
  
  let content = '';
  let mimeType = 'text/plain';
  
  switch (format) {
    case 'yolo':
      content = '0 0.5 0.5 0.25 0.2\n1 0.7 0.6 0.15 0.1';
      break;
    case 'coco':
      content = JSON.stringify({
        images: [],
        annotations: [],
        categories: [],
      }, null, 2);
      mimeType = 'application/json';
      break;
    case 'ocr_json':
      content = JSON.stringify({
        version: '1.0',
        regions: [],
        text: '',
      }, null, 2);
      mimeType = 'application/json';
      break;
    case 'hocr':
      content = '<?xml version="1.0" encoding="UTF-8"?>\n<html><body><div class="ocr_page"></div></body></html>';
      mimeType = 'application/xml';
      break;
  }
  
  return new Blob([content], { type: mimeType });
};

export const getActivityLogs = async (projectId?: string): Promise<ActivityLog[]> => {
  await delay(300);
  return [
    {
      id: 'log-1',
      action: 'create',
      description: 'Created new YOLO project',
      userId: 'user-1',
      userName: 'John Doe',
      timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: 'log-2',
      action: 'upload',
      description: 'Uploaded 15 images',
      userId: 'user-1',
      userName: 'John Doe',
      timestamp: new Date(Date.now() - 1.5 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: 'log-3',
      action: 'annotate',
      description: 'Annotated 10 images with 45 bounding boxes',
      userId: 'user-2',
      userName: 'Jane Smith',
      timestamp: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: 'log-4',
      action: 'inference',
      description: 'Ran inference using YOLOv8-nano',
      userId: 'user-1',
      userName: 'John Doe',
      timestamp: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
    },
    {
      id: 'log-5',
      action: 'export',
      description: 'Exported annotations in YOLO format',
      userId: 'user-2',
      userName: 'Jane Smith',
      timestamp: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
    },
  ];
};

export const createProject = async (data: {
  name: string;
  type: 'YOLO' | 'OCR';
  modelId?: string;
}): Promise<Project> => {
  await delay(500);
  return {
    id: `project-${Date.now()}`,
    name: data.name,
    type: data.type,
    modelId: data.modelId,
    datasetCount: 0,
    annotationCount: 0,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
};

export const getProjects = async (): Promise<Project[]> => {
  await delay(400);
  return [
    {
      id: 'project-1',
      name: 'Document Detection',
      type: 'YOLO',
      modelId: 'model-1',
      datasetCount: 150,
      annotationCount: 1250,
      createdAt: '2024-01-10T08:00:00Z',
      updatedAt: '2024-02-15T16:30:00Z',
    },
    {
      id: 'project-2',
      name: 'Invoice OCR',
      type: 'OCR',
      modelId: 'model-3',
      datasetCount: 80,
      annotationCount: 640,
      createdAt: '2024-02-01T10:00:00Z',
      updatedAt: '2024-02-14T12:00:00Z',
    },
  ];
};
