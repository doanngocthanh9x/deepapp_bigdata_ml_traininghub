// API Services for AA/A0 modules
export interface OcrRequest {
  engine?: 'vietocr' | 'paddleocr';
  language?: 'vi' | 'en';
  image?: string; // base64
  imagePath?: string;
}

export interface OcrResponse {
  success: boolean;
  text?: string;
  confidence?: number;
  error?: string;
  processingTime?: number;
}

export interface OcrPipelineRequest {
  image?: string;
  imagePath?: string;
  language?: 'vi' | 'en';
  detectText?: boolean;
  recognizeText?: boolean;
}

export interface OcrPipelineResponse {
  success: boolean;
  text?: string;
  confidence?: number;
  boundingBoxes?: Array<{
    x: number;
    y: number;
    width: number;
    height: number;
    text?: string;
  }>;
  error?: string;
  processingTime?: number;
}

export interface PaddleOcrRequest {
  model?: 'recognition';
  language?: 'vi' | 'en';
  image?: string;
  imagePath?: string;
}

export interface PaddleOcrResponse {
  success: boolean;
  text?: string;
  confidence?: number;
  error?: string;
  model?: string;
}

// Health check response interface
export interface HealthCheckResponse {
  status: string;
  message?: string;
  timestamp?: string;
}

// AAA0_0101 - OCR Services API
export const ocrApi = {
  async healthCheck(): Promise<HealthCheckResponse> {
    const response = await fetch('/AA/A0/AAA0_0101');
    return response.json();
  },

  async processOcr(request: OcrRequest): Promise<OcrResponse> {
    const response = await fetch('/AA/A0/AAA0_0101', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  async processVietOcr(request: OcrRequest): Promise<OcrResponse> {
    const response = await fetch('/AA/A0/AAA0_0101/vietocr', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  async processPaddleOcr(request: OcrRequest): Promise<OcrResponse> {
    const response = await fetch('/AA/A0/AAA0_0101/paddleocr', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },
};

// AAA0_0102 - OCR Pipeline API
export const ocrPipelineApi = {
  async process(request: OcrPipelineRequest): Promise<OcrPipelineResponse> {
    const response = await fetch('/AA/A0/AAA0_0102/process', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  async healthCheck(): Promise<OcrPipelineResponse> {
    const response = await fetch('/AA/A0/AAA0_0102/health');
    return response.json();
  },
};

// Models list response interface
export interface ModelsListResponse {
  models: string[];
  default?: string;
}

// AAA0_0201 - PaddleOCR API
export const paddleOcrApi = {
  async healthCheck(): Promise<HealthCheckResponse> {
    const response = await fetch('/AA/A0/AAA0_0201');
    return response.json();
  },

  async recognizeText(request: PaddleOcrRequest): Promise<PaddleOcrResponse> {
    const response = await fetch('/AA/A0/AAA0_0201', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  async recognizeWithModel(request: PaddleOcrRequest): Promise<PaddleOcrResponse> {
    const response = await fetch('/AA/A0/AAA0_0201/recognize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  async listModels(): Promise<ModelsListResponse> {
    const response = await fetch('/AA/A0/AAA0_0201/models');
    return response.json();
  },
};

// AAA0_0103 - Discharge Paper OCR API
export const dischargePaperOcrApi = {
  async process(request: FormData): Promise<any> {
    const response = await fetch('/AA/A0/AAA0_0103/process', {
      method: 'POST',
      body: request,
    });
    return response.json();
  },

  async healthCheck(): Promise<HealthCheckResponse> {
    const response = await fetch('/AA/A0/AAA0_0103');
    return response.json();
  },
};

// Document interfaces for AAA0_0104
export interface FileDTO {
  id: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
  pageCount: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageDTO {
  id: number;
  taskId: number;
  pageNumber: number;
  filePath: string;
  width: number;
  height: number;
  createdAt: string;
}

export interface BBoxDTO {
  id: number;
  pageId: number;
  coordinates: string; // JSON string of bbox coordinates
  class: string;
  confidence: number;
  filePath: string;
  createdAt: string;
}

export interface DocumentStats {
  totalFiles: number;
  totalPages: number;
  totalBboxes: number;
  totalSize: number;
}

// AAA0_0104 - Document Management API
export const documentManagementApi = {
  async uploadDocument(request: FormData): Promise<FileDTO> {
    const response = await fetch('/AA/A0/AAA0_0104/upload', {
      method: 'POST',
      body: request,
    });
    return response.json();
  },

  async getAllDocuments(): Promise<FileDTO[]> {
    const response = await fetch('/AA/A0/AAA0_0104/files');
    return response.json();
  },

  async getDocument(id: string): Promise<FileDTO> {
    const response = await fetch(`/AA/A0/AAA0_0104/files/${id}`);
    return response.json();
  },

  async getDocumentPages(id: string): Promise<PageDTO[]> {
    const response = await fetch(`/AA/A0/AAA0_0104/files/${id}/pages`);
    return response.json();
  },

  async getPageBboxes(pageId: string): Promise<BBoxDTO[]> {
    const response = await fetch(`/AA/A0/AAA0_0104/pages/${pageId}/bboxes`);
    return response.json();
  },

  async deleteDocument(id: string): Promise<{ success: boolean }> {
    const response = await fetch(`/AA/A0/AAA0_0104/files/${id}`, {
      method: 'DELETE',
    });
    return response.json();
  },

  async getStats(): Promise<DocumentStats> {
    const response = await fetch('/AA/A0/AAA0_0104/stats');
    return response.json();
  },

  async healthCheck(): Promise<HealthCheckResponse> {
    const response = await fetch('/AA/A0/AAA0_0104/health');
    return response.json();
  },
};

// YOLO interfaces
export interface YoloDetectionRequest {
  image?: string;
  imagePath?: string;
  confidence?: number;
  iou?: number;
  maxDetections?: number;
  model?: string;
}

export interface YoloDetectionResponse {
  success: boolean;
  detections?: Array<{
    class: string;
    confidence: number;
    bbox: [number, number, number, number]; // x, y, width, height
  }>;
  error?: string;
  processingTime?: number;
}

// AAA0_0105 - YOLO Object Detection API
export const yoloApi = {
  async detectObjects(request: FormData): Promise<YoloDetectionResponse> {
    const response = await fetch('/AA/A0/AAA0_0105/detect', {
      method: 'POST',
      body: request,
    });
    return response.json();
  },

  async detectObjectsJson(request: YoloDetectionRequest): Promise<YoloDetectionResponse> {
    const response = await fetch('/AA/A0/AAA0_0105/detect', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  async healthCheck(): Promise<HealthCheckResponse> {
    const response = await fetch('/AA/A0/AAA0_0105');
    return response.json();
  },
};

// Utility functions
export const fileToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      const base64 = reader.result as string;
      // Remove the data:image/jpeg;base64, prefix
      const base64Data = base64.split(',')[1];
      resolve(base64Data);
    };
    reader.onerror = error => reject(error);
  });
};

export const validateImageFile = (file: File): { valid: boolean; error?: string } => {
  const maxSize = 10 * 1024 * 1024; // 10MB
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];

  if (file.size > maxSize) {
    return { valid: false, error: 'File size must be less than 10MB' };
  }

  if (!allowedTypes.includes(file.type)) {
    return { valid: false, error: 'File must be PNG, JPG, JPEG, or PDF' };
  }

  return { valid: true };
};