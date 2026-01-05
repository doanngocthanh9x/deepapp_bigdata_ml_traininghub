import { useState, useEffect, useCallback } from 'react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { FileText, Upload, Trash2, Download, Search, BarChart3, Loader2, CheckCircle, XCircle, File, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StatsCard } from '@/components/StatsCard';
import { useApi } from '@/hooks/useApi';
import { documentManagementApi, fileToBase64, validateImageFile, type FileDTO, type PageDTO, type BBoxDTO, type DocumentStats } from '@/services/aaApi';
import { cn } from '@/lib/utils';
import { useToast } from '@/hooks/use-toast';

const DocumentManagementPage = () => {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [documents, setDocuments] = useState<FileDTO[]>([]);
  const [stats, setStats] = useState<DocumentStats | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [uploading, setUploading] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState<FileDTO | null>(null);
  const [documentPages, setDocumentPages] = useState<PageDTO[]>([]);
  const [pageBboxes, setPageBboxes] = useState<BBoxDTO[]>([]);
  const { toast } = useToast();

  // Load documents and stats on mount
  const { execute: loadDocuments } = useApi<FileDTO[]>(
    () => documentManagementApi.getAllDocuments(),
    {
      onSuccess: (data) => setDocuments(data),
    }
  );

  const { execute: loadStats } = useApi<DocumentStats>(
    () => documentManagementApi.getStats(),
    {
      onSuccess: (data) => setStats(data),
    }
  );

  useEffect(() => {
    loadDocuments();
    loadStats();
  }, [loadDocuments, loadStats]);

  const { execute: uploadDocument } = useApi<FileDTO>(
    async () => {
      if (!selectedFile) throw new Error('No file selected');

      const formData = new FormData();
      formData.append('file', selectedFile);

      return documentManagementApi.uploadDocument(formData);
    },
    {
      onSuccess: (data) => {
        setDocuments(prev => [data, ...prev]);
        setSelectedFile(null);
        loadStats(); // Refresh stats
        toast({
          title: "Upload Complete",
          description: `${data.fileName} uploaded successfully`,
        });
      },
      onError: (error) => {
        toast({
          title: "Upload Failed",
          description: error,
          variant: "destructive",
        });
      },
    }
  );

  const { execute: deleteDocument } = useApi<{ success: boolean }>(
    async (id: string) => documentManagementApi.deleteDocument(id),
    {
      onSuccess: () => {
        setDocuments(prev => prev.filter(doc => doc.id.toString() !== arguments[0]));
        setSelectedDocument(null);
        setDocumentPages([]);
        setPageBboxes([]);
        loadStats(); // Refresh stats
        toast({
          title: "Document Deleted",
          description: "Document removed successfully",
        });
      },
      onError: (error) => {
        toast({
          title: "Delete Failed",
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

  const handleUpload = useCallback(async () => {
    if (!selectedFile) {
      toast({
        title: "No File Selected",
        description: "Please select a file first",
        variant: "destructive",
      });
      return;
    }

    setUploading(true);
    try {
      await uploadDocument();
    } finally {
      setUploading(false);
    }
  }, [selectedFile, uploadDocument, toast]);

  const { execute: loadDocumentPages } = useApi<PageDTO[]>(
    async (fileId: string) => documentManagementApi.getDocumentPages(fileId),
    {
      onSuccess: (data) => setDocumentPages(data),
    }
  );

  const { execute: loadPageBboxes } = useApi<BBoxDTO[]>(
    async (pageId: string) => documentManagementApi.getPageBboxes(pageId),
    {
      onSuccess: (data) => setPageBboxes(data),
    }
  );

  useEffect(() => {
    loadDocuments();
    loadStats();
  }, [loadDocuments, loadStats]);

  const handleViewDocument = useCallback(async (document: FileDTO) => {
    setSelectedDocument(document);
    setDocumentPages([]);
    setPageBboxes([]);
    await loadDocumentPages(document.id.toString());
  }, [loadDocumentPages]);

  const handleViewPageBboxes = useCallback(async (pageId: number) => {
    setPageBboxes([]);
    await loadPageBboxes(pageId.toString());
  }, [loadPageBboxes]);

  const handleDelete = useCallback(async (id: number) => {
    if (confirm('Are you sure you want to delete this document?')) {
      await deleteDocument(id.toString());
    }
  }, [deleteDocument]);

  const filteredDocuments = documents.filter(doc =>
    doc.fileName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Document Management</h1>
            <p className="text-muted-foreground">
              Upload, manage, and process OCR documents
            </p>
          </div>
          <FileText className="h-8 w-8 text-muted-foreground" />
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <StatsCard
              title="Total Files"
              value={stats.totalFiles.toString()}
              icon={FileText}
              description="Files in system"
            />
            <StatsCard
              title="Total Pages"
              value={stats.totalPages.toString()}
              icon={File}
              description="Pages processed"
            />
            <StatsCard
              title="Total BBoxes"
              value={stats.totalBboxes.toString()}
              icon={BarChart3}
              description="Detected regions"
            />
            <StatsCard
              title="Total Size"
              value={formatFileSize(stats.totalSize)}
              icon={BarChart3}
              description="Storage used"
            />
          </div>
        )}

        {/* Main Content */}
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Upload Section */}
          <div className="lg:col-span-1 space-y-4">
            <div className="rounded-lg border-2 border-dashed border-muted-foreground/25 p-6 text-center">
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
                <Upload className="mx-auto h-10 w-10 text-muted-foreground" />
                <div className="mt-4">
                  <p className="text-lg font-medium">
                    {selectedFile ? selectedFile.name : 'Drop files here'}
                  </p>
                  <p className="text-sm text-muted-foreground mt-1">
                    or click to browse
                  </p>
                  <p className="text-xs text-muted-foreground mt-2">
                    Supports images and PDFs up to 10MB
                  </p>
                </div>
              </div>
              <input
                id="file-input"
                type="file"
                accept="image/*,.pdf"
                onChange={handleFileInput}
                className="hidden"
              />
            </div>

            {selectedFile && (
              <div className="rounded-lg border p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">{selectedFile.name}</p>
                    <p className="text-sm text-muted-foreground">
                      {formatFileSize(selectedFile.size)}
                    </p>
                  </div>
                  <Button
                    onClick={handleUpload}
                    disabled={uploading}
                    size="sm"
                  >
                    {uploading ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Upload className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </div>
            )}
          </div>

          {/* Documents List */}
          <div className="lg:col-span-2 space-y-4">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search documents..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border rounded-md"
              />
            </div>

            {/* Documents */}
            <div className="space-y-2 max-h-96 overflow-y-auto">
              {filteredDocuments.length === 0 ? (
                <div className="rounded-lg border border-dashed p-8 text-center text-muted-foreground">
                  <File className="mx-auto h-12 w-12 mb-4" />
                  <p>No documents found</p>
                  <p className="text-sm">Upload some documents to get started</p>
                </div>
              ) : (
                filteredDocuments.map((doc) => (
                  <div key={doc.id} className="rounded-lg border p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <div className="flex items-center">
                          <FileText className="h-4 w-4 mr-2 text-muted-foreground" />
                          <span className="font-medium">{doc.fileName}</span>
                          <span className={cn(
                            "ml-2 px-2 py-1 rounded-full text-xs",
                            doc.status === 'COMPLETED' && "bg-green-100 text-green-800",
                            doc.status === 'PROCESSING' && "bg-yellow-100 text-yellow-800",
                            doc.status === 'FAILED' && "bg-red-100 text-red-800",
                            doc.status === 'UPLOADED' && "bg-blue-100 text-blue-800"
                          )}>
                            {doc.status}
                          </span>
                        </div>

                        <div className="mt-2 text-sm text-muted-foreground">
                          <span>Size: {formatFileSize(doc.fileSize)} • </span>
                          <span>Pages: {doc.pageCount} • </span>
                          <span>Uploaded: {formatDate(doc.createdAt)}</span>
                        </div>

                        <div className="mt-2 text-sm text-muted-foreground">
                          <span>Type: {doc.mimeType}</span>
                        </div>
                      </div>

                      <div className="flex items-center space-x-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleViewDocument(doc)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleDelete(doc.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Document Details */}
        {selectedDocument && (
          <div className="rounded-lg border p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold">Document Details: {selectedDocument.fileName}</h2>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSelectedDocument(null);
                  setDocumentPages([]);
                  setPageBboxes([]);
                }}
              >
                Close
              </Button>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
              {/* Pages */}
              <div>
                <h3 className="text-lg font-medium mb-3">Pages ({documentPages.length})</h3>
                <div className="space-y-2 max-h-64 overflow-y-auto">
                  {documentPages.length === 0 ? (
                    <p className="text-muted-foreground">Loading pages...</p>
                  ) : (
                    documentPages.map((page) => (
                      <div key={page.id} className="rounded border p-3">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="font-medium">Page {page.pageNumber}</p>
                            <p className="text-sm text-muted-foreground">
                              {page.width} x {page.height} px
                            </p>
                          </div>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleViewPageBboxes(page.id)}
                          >
                            View BBoxes
                          </Button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* BBoxes */}
              <div>
                <h3 className="text-lg font-medium mb-3">Bounding Boxes ({pageBboxes.length})</h3>
                <div className="space-y-2 max-h-64 overflow-y-auto">
                  {pageBboxes.length === 0 ? (
                    <p className="text-muted-foreground">Select a page to view bounding boxes</p>
                  ) : (
                    pageBboxes.map((bbox) => (
                      <div key={bbox.id} className="rounded border p-3">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="font-medium">{bbox.class}</p>
                            <p className="text-sm text-muted-foreground">
                              Confidence: {(bbox.confidence * 100).toFixed(1)}%
                            </p>
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default DocumentManagementPage;