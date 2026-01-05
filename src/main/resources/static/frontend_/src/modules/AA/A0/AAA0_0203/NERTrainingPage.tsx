import { DashboardLayout } from '@/components/DashboardLayout';
import { Brain, Database, FileText, Download, CheckCircle, AlertCircle, Eye, Settings, Plus, Search, BarChart3, Target, Edit, Save, X, ChevronDown, ChevronRight, Tag, Zap, Activity, Users, Stethoscope, Calendar, MapPin, FileCheck, User, Hash, Copy, Clipboard, Globe, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue, SelectSeparator } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Progress } from '@/components/ui/progress';
import { cn } from '@/lib/utils';
import { useState, useEffect } from 'react';
import { EntityTypeManager } from './EntityTypeManager';
import { EntityType, EntityTypeAPI } from './entityTypeAPI';

// Legacy entity types for backward compatibility (will be replaced by dynamic loading)
const MEDICAL_ENTITY_TYPES = {
  'PERSON': { label: 'Tên người bệnh', color: 'bg-blue-100 text-blue-800 border-blue-200', icon: Users },
  'HOSPITAL': { label: 'Tên bệnh viện', color: 'bg-green-100 text-green-800 border-green-200', icon: Stethoscope },
  'DATE': { label: 'Ngày tháng', color: 'bg-purple-100 text-purple-800 border-purple-200', icon: Calendar },
  'AGE': { label: 'Tuổi', color: 'bg-orange-100 text-orange-800 border-orange-200', icon: Hash },
  'GENDER': { label: 'Giới tính', color: 'bg-pink-100 text-pink-800 border-pink-200', icon: User },
  'DIAGNOSIS': { label: 'Chẩn đoán', color: 'bg-red-100 text-red-800 border-red-200', icon: Activity },
  'TREATMENT': { label: 'Phương pháp điều trị', color: 'bg-indigo-100 text-indigo-800 border-indigo-200', icon: Zap },
  'ADDRESS': { label: 'Địa chỉ', color: 'bg-yellow-100 text-yellow-800 border-yellow-200', icon: MapPin },
  'ID_NUMBER': { label: 'Số CMND/CCCD', color: 'bg-cyan-100 text-cyan-800 border-cyan-200', icon: FileCheck },
  'INSURANCE': { label: 'BHXH/BHYT', color: 'bg-lime-100 text-lime-800 border-lime-200', icon: FileCheck },
  'OCCUPATION': { label: 'Nghề nghiệp', color: 'bg-emerald-100 text-emerald-800 border-emerald-200', icon: Users },
  'ETHNICITY': { label: 'Dân tộc', color: 'bg-violet-100 text-violet-800 border-violet-200', icon: Users },
  'DOCUMENT_TYPE': { label: 'Loại giấy tờ', color: 'bg-gray-100 text-gray-800 border-gray-200', icon: FileText }
};

const NERTrainingPage = () => {
  const [trainingData, setTrainingData] = useState([]);
  const [canTrain, setCanTrain] = useState(false);
  const [totalApprovedSamples, setTotalApprovedSamples] = useState(0);
  const [entityStatistics, setEntityStatistics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState(null);
  const [logs, setLogs] = useState([]);
  const [refreshing, setRefreshing] = useState(false);
  const [lastRefreshTime, setLastRefreshTime] = useState(null);
  const [selectedDataset, setSelectedDataset] = useState(null);
  const [editingDataset, setEditingDataset] = useState(null);
  const [validationErrors, setValidationErrors] = useState([]);
  
  // Dynamic entity types
  const [entityTypes, setEntityTypes] = useState<EntityType[]>([]);
  const [entityTypesMap, setEntityTypesMap] = useState({});
  const [showEntityTypeManager, setShowEntityTypeManager] = useState(false);
  const [initializingEntityTypes, setInitializingEntityTypes] = useState(false);
  
  const [expandedSections, setExpandedSections] = useState({
    overview: true,
    datasets: true,
    annotation: false,
    training: false,
    logs: false
  });

  // Text selection state
  const [selectionStart, setSelectionStart] = useState(null);
  const [selectionEnd, setSelectionEnd] = useState(null);
  const [isSelecting, setIsSelecting] = useState(false);

  // Annotation state
  const [selectedText, setSelectedText] = useState('');
  const [selectedEntityType, setSelectedEntityType] = useState('');
  const [annotations, setAnnotations] = useState([]);
  const [currentAnnotationText, setCurrentAnnotationText] = useState('');
  const [currentSample, setCurrentSample] = useState(null);
  const [selectedDatasetForAnnotation, setSelectedDatasetForAnnotation] = useState('');
  const [samplesList, setSamplesList] = useState([]);
  const [isEditingText, setIsEditingText] = useState(false);
  const [editedText, setEditedText] = useState('');

  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalSamples, setTotalSamples] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  // Approval filter state
  const [approvalFilter, setApprovalFilter] = useState<'all' | 'approved' | 'pending'>('all');

  // Training state
  const [trainingConfig, setTrainingConfig] = useState({
    epochs: 50,
    batch_size: 16,
    learning_rate: 0.001,
    model_type: 'bert',
    entity_types: Object.keys(MEDICAL_ENTITY_TYPES)
  });
  const [trainingProgress, setTrainingProgress] = useState(null);

  // Evaluation state
  const [selectedModel, setSelectedModel] = useState('');
  const [predictionText, setPredictionText] = useState('');
  const [predictionResults, setPredictionResults] = useState(null);
  const [trainedModels, setTrainedModels] = useState([]);

  // Bulk operations state
  const [selectedDatasets, setSelectedDatasets] = useState([]);
  const [bulkOperationMode, setBulkOperationMode] = useState(false);

  // Sample operations state
  const [selectedSamples, setSelectedSamples] = useState([]);
  const [sampleOperationMode, setSampleOperationMode] = useState(false);

  // Dataset samples cache - stores loaded samples for each dataset
  const [datasetSamplesCache, setDatasetSamplesCache] = useState({});

  // Load entity types on mount and when dataset changes
  useEffect(() => {
    loadEntityTypes(selectedDatasetForAnnotation);
  }, [selectedDatasetForAnnotation]);

  useEffect(() => {
    loadTrainingData();
    loadTrainedModels();
    
    // Auto-refresh every 5 seconds to catch new data from AAA0_0202
    const refreshInterval = setInterval(() => {
      console.log('[Auto-refresh] Checking for new training data...');
      loadTrainingData();
      
      // Only refresh samples if on first page (to avoid losing "Load More" progress)
      if (selectedDatasetForAnnotation && currentPage === 0) {
        console.log('[Auto-refresh] Reloading samples for dataset:', selectedDatasetForAnnotation);
        loadSamplesForDataset(selectedDatasetForAnnotation, 0, false);
      } else if (selectedDatasetForAnnotation && currentPage > 0) {
        console.log('[Auto-refresh] Skipping sample reload - user is on page', currentPage + 1);
      }
    }, 5000); // 5 seconds for faster updates
    
    return () => clearInterval(refreshInterval);
  }, [selectedDatasetForAnnotation, currentPage]);

  // Auto-save annotations when they change
  useEffect(() => {
    if (currentSample && annotations.length >= 0 && !isEditingText) {
      const timeoutId = setTimeout(async () => {
        try {
          const response = await fetch('/AA/A0/AAA0_0203/process', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              event_type: 'update_sample_annotations',
              payload: {
                template_id: selectedDatasetForAnnotation,
                sample_id: currentSample.id,
                text: currentAnnotationText,
                annotations: annotations
              }
            })
          });

          const result = await response.json();
          if (result.status === 'success') {
            // Auto-saved successfully - could show a subtle indicator
            console.log('Auto-saved annotations');
          }
        } catch (error) {
          console.error('Error auto-saving annotations:', error);
        }
      }, 1000); // Auto-save after 1 second of inactivity

      return () => clearTimeout(timeoutId);
    }
  }, [annotations, currentSample, currentAnnotationText, selectedDatasetForAnnotation, isEditingText]);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e) => {
      // Only handle shortcuts when not editing text
      if (isEditingText) return;

      if (e.ctrlKey || e.metaKey) {
        switch (e.key) {
          case 'a':
            e.preventDefault();
            if (currentAnnotationText) selectAll();
            break;
          case 'c':
            if (selectedText) {
              e.preventDefault();
              navigator.clipboard.writeText(selectedText);
            }
            break;
        }
      } else {
        switch (e.key) {
          case 'Escape':
            clearSelection();
            break;
          case 'Delete':
          case 'Backspace':
            if (selectedText && !selectedEntityType) {
              e.preventDefault();
              // Could add quick delete functionality here
            }
            break;
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isEditingText, currentAnnotationText, selectedText, selectedEntityType]);

  // Color mapping for entity types (must be static for Tailwind)
  const COLOR_CLASSES = {
    'blue': 'bg-blue-100 text-blue-800 border-blue-200',
    'green': 'bg-green-100 text-green-800 border-green-200',
    'purple': 'bg-purple-100 text-purple-800 border-purple-200',
    'orange': 'bg-orange-100 text-orange-800 border-orange-200',
    'pink': 'bg-pink-100 text-pink-800 border-pink-200',
    'red': 'bg-red-100 text-red-800 border-red-200',
    'indigo': 'bg-indigo-100 text-indigo-800 border-indigo-200',
    'yellow': 'bg-yellow-100 text-yellow-800 border-yellow-200',
    'cyan': 'bg-cyan-100 text-cyan-800 border-cyan-200',
    'teal': 'bg-teal-100 text-teal-800 border-teal-200',
    'gray': 'bg-gray-100 text-gray-800 border-gray-200'
  };

  // Helper function to convert dynamic entity types to legacy format
  const convertEntityTypeToLegacy = (entityType: EntityType) => {
    const colorClass = COLOR_CLASSES[entityType.color] || COLOR_CLASSES['gray'];
    return {
      label: entityType.display_label,
      color: colorClass,
      icon: entityType.icon || '🏷️'
    };
  };

  // Helper function to get entity type config (supports both dynamic and legacy)
  const getEntityTypeConfig = (entityCode: string) => {
    // Try dynamic first
    if (entityTypesMap[entityCode]) {
      return convertEntityTypeToLegacy(entityTypesMap[entityCode]);
    }
    // Fall back to legacy
    return MEDICAL_ENTITY_TYPES[entityCode] || { 
      label: entityCode, 
      color: 'bg-gray-100 text-gray-800 border-gray-200',
      icon: Tag 
    };
  };

  // Get all available entity types as array for dropdowns
  const getAvailableEntityTypes = () => {
    if (entityTypes.length > 0) {
      return entityTypes.map(et => ({
        code: et.entity_code,
        label: et.display_label,
        config: convertEntityTypeToLegacy(et)
      }));
    }
    // Fall back to legacy
    return Object.entries(MEDICAL_ENTITY_TYPES).map(([key, config]) => ({
      code: key,
      label: config.label,
      config: config
    }));
  };

  // Load entity types from API
  const loadEntityTypes = async (templateId?: string) => {
    try {
      let types = await EntityTypeAPI.getEntityTypes(templateId);
      
      // If no entity types exist, initialize default ones
      if (types.length === 0 && !templateId) {
        console.log('No entity types found, initializing defaults...');
        setInitializingEntityTypes(true);
        try {
          await EntityTypeAPI.initializeDefaultEntityTypes();
          // Reload after initialization
          types = await EntityTypeAPI.getEntityTypes(templateId);
          console.log(`Initialized ${types.length} default entity types`);
        } catch (initError) {
          console.error('Failed to initialize default entity types:', initError);
        } finally {
          setInitializingEntityTypes(false);
        }
      }
      
      setEntityTypes(types);
      
      // Create a map for quick lookup
      const typesMap = {};
      types.forEach(type => {
        typesMap[type.entity_code] = type;
      });
      setEntityTypesMap(typesMap);
      
      // Update training config with available entity types
      setTrainingConfig(prev => ({
        ...prev,
        entity_types: types.map(t => t.entity_code)
      }));
    } catch (error) {
      console.error('Error loading entity types:', error);
      // Fall back to legacy types
      setEntityTypesMap({});
    }
  };

  const loadTrainingData = async () => {
    try {
      const response = await fetch('/AA/A0/AAA0_0203/training-data');
      const result = await response.json();
      if (result.status === 'success') {
        const newData = result.data.training_data || [];
        const newTotal = result.data.total_approved_samples || 0;
        
        // Check if there's new data
        if (trainingData.length > 0 && newTotal > totalApprovedSamples) {
          const diff = newTotal - totalApprovedSamples;
          console.log(`[Data Update] +${diff} new samples detected! (${totalApprovedSamples} → ${newTotal})`);
          // Show toast notification
          setResults({ message: `✨ Received ${diff} new training samples from AAA0_0202!` });
        }
        
        setTrainingData(newData);
        // Entity statistics removed to avoid loading all data at once
        setEntityStatistics(null);
        setCanTrain(result.data.can_train || false);
        setTotalApprovedSamples(newTotal);
        setLastRefreshTime(new Date());
      }
    } catch (error) {
      console.error('Error loading training data:', error);
    }
  };

  const handleManualRefresh = async () => {
    setRefreshing(true);
    try {
      await loadTrainingData();
      if (selectedDatasetForAnnotation) {
        await loadSamplesForDataset(selectedDatasetForAnnotation, 0, false);
      }
      setResults({ message: 'Data refreshed successfully' });
    } finally {
      setRefreshing(false);
    }
  };

  const loadTrainedModels = async () => {
    try {
      const response = await fetch('/AA/A0/AAA0_0203/models');
      const result = await response.json();
      if (result.status === 'success') {
        setTrainedModels(result.data.models || []);
      }
    } catch (error) {
      console.error('Error loading trained models:', error);
    }
  };

  const predictEntities = async () => {
    if (!selectedModel || !predictionText.trim()) return;

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0203/predict', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model_path: selectedModel,
          text: predictionText
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        setPredictionResults(result.data);
      } else {
        alert('Lỗi khi dự đoán: ' + (result.message || 'Unknown error'));
      }
    } catch (error) {
      console.error('Error predicting entities:', error);
      alert('Lỗi kết nối khi dự đoán');
    } finally {
      setLoading(false);
    }
  };

  const evaluateModel = async (model) => {
    // For now, show a simple evaluation using some test data
    // In a real implementation, this would use a proper test dataset
    alert(`Đánh giá mô hình ${model.training_id} - Tính năng đang được phát triển`);
  };

  const startTraining = async () => {
    if (!canTrain) {
      alert(`Không đủ dữ liệu huấn luyện. Cần ít nhất 50 mẫu đã được duyệt, hiện có ${totalApprovedSamples} mẫu.`);
      return;
    }

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0203/train', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          config: trainingConfig,
          template_ids: trainingData.map(d => d.template_id)
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        setTrainingProgress({ status: 'running', progress: 0 });
        // Poll for progress
        pollTrainingProgress(result.data.training_id);
      } else {
        alert('Lỗi khi bắt đầu training: ' + (result.message || 'Unknown error'));
      }
    } catch (error) {
      console.error('Error starting training:', error);
      alert('Lỗi kết nối khi bắt đầu training');
    } finally {
      setLoading(false);
    }
  };

  const pollTrainingProgress = (trainingId) => {
    const interval = setInterval(async () => {
      try {
        const response = await fetch(`/AA/A0/AAA0_0203/training/${trainingId}/status`);
        const result = await response.json();
        
        if (result.status === 'success') {
          setTrainingProgress(result.data);
          if (result.data.status === 'completed' || result.data.status === 'failed') {
            clearInterval(interval);
            loadTrainedModels(); // Refresh model list
          }
        }
      } catch (error) {
        console.error('Error polling training progress:', error);
        clearInterval(interval);
      }
    }, 2000);
  };

  const renderPredictedText = (text, entities) => {
    if (!entities || entities.length === 0) return text;

    const sortedEntities = [...entities].sort((a, b) => a.start - b.start);
    const parts = [];
    let lastIndex = 0;

    sortedEntities.forEach((entity, idx) => {
      // Add text before entity
      if (entity.start > lastIndex) {
        parts.push(text.slice(lastIndex, entity.start));
      }

      // Add highlighted entity
      const entityType = getEntityTypeConfig(entity.label);
      parts.push(
        <span key={idx} className={cn("inline-block px-1 py-0.5 mx-0.5 rounded border text-xs font-medium", entityType.color)}>
          {text.slice(entity.start, entity.end)}
          <Badge variant="outline" className="ml-1 text-xs">
            {entityType.label}
          </Badge>
        </span>
      );

      lastIndex = entity.end;
    });

    // Add remaining text
    if (lastIndex < text.length) {
      parts.push(text.slice(lastIndex));
    }

    return parts;
  };

  const loadSamplesForDataset = async (templateId, page = 0, append = false) => {
    console.log('Loading samples for dataset:', templateId, 'page:', page);
    setLoading(true);
    try {
      const response = await fetch(`/AA/A0/AAA0_0203/samples/${templateId}?page=${page}&page_size=${pageSize}`);
      const result = await response.json();
      console.log('Load samples response:', result);
      if (result.status === 'success') {
        const newSamples = result.data.samples || [];

        // If append mode (for "Load More"), add to existing samples
        if (append) {
          setSamplesList(prev => [...prev, ...newSamples]);
        } else {
          // Otherwise replace samples (initial load)
          setSamplesList(newSamples);
        }

        setSelectedDatasetForAnnotation(templateId);
        setTotalSamples(result.data.total_samples || 0);
        setTotalPages(result.data.total_pages || 0);
        setCurrentPage(result.data.page || 0);
        setHasMore(result.data.has_more || false);

        console.log('Samples loaded:', newSamples.length, '/', result.data.total_samples);
      }
    } catch (error) {
      console.error('Error loading samples:', error);
      // Fallback: try to load from training data if endpoint doesn't exist
      const dataset = trainingData.find(d => d.template_id === templateId);
      if (dataset && dataset.samples) {
        setSamplesList(dataset.samples);
        setSelectedDatasetForAnnotation(templateId);
        setTotalSamples(dataset.samples.length);
      }
    } finally {
      setLoading(false);
    }
  };

  const loadMoreSamples = async () => {
    if (!selectedDatasetForAnnotation || !hasMore || loading) return;
    await loadSamplesForDataset(selectedDatasetForAnnotation, currentPage + 1, true);
  };

  // Load samples for viewing in dataset list (not for annotation)
  const loadDatasetSamplesForView = async (templateId) => {
    // Check if already in cache
    if (datasetSamplesCache[templateId]) {
      console.log('Using cached samples for:', templateId);
      return;
    }

    console.log('Loading samples for view:', templateId);
    setLoading(true);
    try {
      const response = await fetch(`/AA/A0/AAA0_0203/samples/${templateId}?page=0&page_size=5`);
      const result = await response.json();
      if (result.status === 'success') {
        const samples = result.data.samples || [];
        // Store in cache
        setDatasetSamplesCache(prev => ({
          ...prev,
          [templateId]: samples.slice(0, 3) // Only keep first 3 for preview
        }));
        console.log('Loaded', samples.length, 'samples for view');
      }
    } catch (error) {
      console.error('Error loading samples for view:', error);
    } finally {
      setLoading(false);
    }
  };

  const selectSampleForAnnotation = (sample) => {
    setCurrentSample(sample);
    setCurrentAnnotationText(sample.text);
    setEditedText(sample.text);
    setAnnotations(sample.annotations || []);
    setSelectedText('');
    setSelectedEntityType('');
    setSelectionStart(null);
    setSelectionEnd(null);
    setIsSelecting(false);
    setIsEditingText(false);
  };

  const saveSampleAnnotations = async () => {
    if (!currentSample) return;

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0203/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          event_type: 'update_sample_annotations',
          payload: {
            template_id: selectedDatasetForAnnotation,
            sample_id: currentSample.id || currentSample.text.substring(0, 50),
            text: currentAnnotationText,
            annotations: annotations
          }
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        setResults({ message: 'Đã lưu nhãn cho mẫu' });
        await loadSamplesForDataset(selectedDatasetForAnnotation);
      }
    } catch (error) {
      console.error('Error saving annotations:', error);
    } finally {
      setLoading(false);
    }
  };

  const approveSample = async () => {
    if (!currentSample) return;

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0203/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          event_type: 'approve_sample',
          payload: {
            template_id: selectedDatasetForAnnotation,
            sample_id: currentSample.id
          }
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        setResults({ message: 'Đã duyệt mẫu' });
        await loadSamplesForDataset(selectedDatasetForAnnotation);
      }
    } catch (error) {
      console.error('Error approving sample:', error);
    } finally {
      setLoading(false);
    }
  };

  const bulkApproveSelectedSamples = async (sampleIds?: string[] | string) => {
    // Ensure we have an array
    let samplesToApprove: any[] = [];
    if (sampleIds) {
      samplesToApprove = Array.isArray(sampleIds) ? sampleIds : [sampleIds];
    } else {
      samplesToApprove = Array.isArray(selectedSamples) ? selectedSamples : [];
    }
    
    if (samplesToApprove.length === 0) {
      alert('Vui lòng chọn mẫu cần duyệt');
      return;
    }

    // Extract IDs if they are objects, otherwise use as-is
    const validSampleIds = samplesToApprove
      .map(item => {
        // If item is an object, extract the ID
        if (typeof item === 'object' && item !== null) {
          return item.id || item.sample_id || null;
        }
        // If item is a string, use it directly
        return item;
      })
      .filter(id => id != null && id !== '');
    
    if (validSampleIds.length === 0) {
      alert('Không có mẫu hợp lệ để duyệt');
      return;
    }

    if (!confirm(`Bạn có chắc muốn duyệt ${validSampleIds.length} mẫu đã chọn?`)) {
      return;
    }

    setLoading(true);
    try {
      // Call bulk approve endpoint (single transaction, avoids SQLite locking)
      const response = await fetch('/AA/A0/AAA0_0203/bulk-approve-samples', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          template_id: selectedDatasetForAnnotation,
          sample_ids: validSampleIds,
          approved: true
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        const data = result.data || {};
        const successCount = data.success_count || validSampleIds.length;
        setResults({ 
          message: `Đã duyệt ${successCount}/${validSampleIds.length} mẫu thành công` 
        });
        
        // Clear selection and reload
        setSelectedSamples([]);
        await loadSamplesForDataset(selectedDatasetForAnnotation);
        await loadTrainingData(); // Refresh counts
      } else {
        alert('Lỗi khi duyệt mẫu: ' + (result.message || 'Unknown error'));
      }
    } catch (error) {
      console.error('Error bulk approving samples:', error);
      alert('Có lỗi xảy ra khi duyệt mẫu hàng loạt');
    } finally {
      setLoading(false);
    }
  };

  const bulkApproveAllSamples = async () => {
    if (!selectedDatasetForAnnotation) {
      alert('Vui lòng chọn dataset');
      return;
    }

    if (!confirm(`Bạn có chắc muốn duyệt TẤT CẢ mẫu trong dataset này?`)) {
      return;
    }

    setLoading(true);
    try {
      // Get all samples in current dataset
      const allSampleIds = samples.map(s => s.id || s.sample_id).filter(id => id);
      
      if (allSampleIds.length === 0) {
        alert('Không có mẫu nào để duyệt');
        return;
      }

      // Call bulk approve endpoint (single transaction)
      const response = await fetch('/AA/A0/AAA0_0203/bulk-approve-samples', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          template_id: selectedDatasetForAnnotation,
          sample_ids: allSampleIds,
          approved: true
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        const data = result.data || {};
        const successCount = data.success_count || allSampleIds.length;
        setResults({ 
          message: `Đã duyệt ${successCount}/${allSampleIds.length} mẫu thành công` 
        });
        
        await loadSamplesForDataset(selectedDatasetForAnnotation);
        await loadTrainingData(); // Refresh counts
      } else {
        alert('Lỗi khi duyệt tất cả mẫu: ' + (result.message || 'Unknown error'));
      }
    } catch (error) {
      console.error('Error bulk approving all samples:', error);
      alert('Có lỗi xảy ra khi duyệt tất cả mẫu');
    } finally {
      setLoading(false);
    }
  };

  const rejectSample = async () => {
    if (!currentSample) return;

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0203/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          event_type: 'reject_sample',
          payload: {
            template_id: selectedDatasetForAnnotation,
            sample_id: currentSample.id
          }
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        setResults({ message: 'Đã từ chối mẫu' });
        await loadSamplesForDataset(selectedDatasetForAnnotation);
      }
    } catch (error) {
      console.error('Error rejecting sample:', error);
    } finally {
      setLoading(false);
    }
  };

  const validateDataset = (dataset) => {
    const errors = [];

    if (!dataset.samples || !Array.isArray(dataset.samples)) {
      errors.push('Dataset must have samples array');
      return errors;
    }

    dataset.samples.forEach((sample, idx) => {
      if (!sample.text || typeof sample.text !== 'string') {
        errors.push(`Sample ${idx + 1}: Missing or invalid text`);
      }

      if (!sample.entities || !Array.isArray(sample.entities)) {
        errors.push(`Sample ${idx + 1}: Missing or invalid entities array`);
      } else {
        sample.entities.forEach((entity, eidx) => {
          if (!entity.start || !entity.end || !entity.label) {
            errors.push(`Sample ${idx + 1}, Entity ${eidx + 1}: Missing start, end, or label`);
          }
          if (entity.start >= entity.end) {
            errors.push(`Sample ${idx + 1}, Entity ${eidx + 1}: start >= end`);
          }
          if (entity.start < 0 || entity.end > sample.text.length) {
            errors.push(`Sample ${idx + 1}, Entity ${eidx + 1}: Entity spans outside text bounds`);
          }
        });
      }
    });

    return errors;
  };

  const saveDataset = async (dataset) => {
    const errors = validateDataset(dataset);
    if (errors.length > 0) {
      setValidationErrors(errors);
      return;
    }

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0203/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          event_type: 'update_dataset',
          payload: {
            template_id: dataset.template_id,
            dataset: dataset
          }
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        setResults({ message: 'Dataset updated successfully' });
        setEditingDataset(null);
        setValidationErrors([]);
        await loadTrainingData();
      }
    } catch (error) {
      console.error('Error saving dataset:', error);
    } finally {
      setLoading(false);
    }
  };

  const exportDataset = async (templateId, format = 'json') => {
    setLoading(true);
    try {
      const response = await fetch(`/AA/A0/AAA0_0203/export/${templateId}?format=${format}`);
      const result = await response.json();
      if (result.status === 'success') {
        // Trigger download
        const downloadResponse = await fetch(`/api/download/${result.data.filepath.split('/').pop()}`);
        const blob = await downloadResponse.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = result.data.filepath.split('/').pop();
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }
    } catch (error) {
      console.error('Error exporting dataset:', error);
    } finally {
      setLoading(false);
    }
  };

  // Bulk dataset operations
  const toggleDatasetSelection = (templateId) => {
    setSelectedDatasets(prev => 
      prev.includes(templateId) 
        ? prev.filter(id => id !== templateId)
        : [...prev, templateId]
    );
  };

  const selectAllDatasets = () => {
    setSelectedDatasets(trainingData.map(d => d.template_id));
  };

  const clearDatasetSelection = () => {
    setSelectedDatasets([]);
  };

  const deleteSelectedDatasets = async () => {
    if (selectedDatasets.length === 0) return;

    if (!confirm(`Bạn có chắc muốn xóa ${selectedDatasets.length} dataset đã chọn?`)) {
      return;
    }

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0203/bulk-delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          template_ids: selectedDatasets
        })
      });

      const result = await response.json();
      if (result.status === 'success') {
        setResults({ message: `Đã xóa ${selectedDatasets.length} dataset` });
        setSelectedDatasets([]);
        setBulkOperationMode(false);
        await loadTrainingData();
      }
    } catch (error) {
      console.error('Error deleting datasets:', error);
    } finally {
      setLoading(false);
    }
  };

  const exportSelectedDatasets = async () => {
    if (selectedDatasets.length === 0) return;

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0203/bulk-export', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          template_ids: selectedDatasets,
          format: 'json'
        })
      });

      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ner_datasets_${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        setResults({ message: `Đã xuất ${selectedDatasets.length} dataset` });
      }
    } catch (error) {
      console.error('Error exporting datasets:', error);
    } finally {
      setLoading(false);
    }
  };

  // Sample operations
  const toggleSampleSelection = (sampleId) => {
    console.log('toggleSampleSelection called with:', typeof sampleId, sampleId);
    setSelectedSamples(prev => {
      const newSelection = prev.includes(sampleId) 
        ? prev.filter(id => id !== sampleId)
        : [...prev, sampleId];
      console.log('selectedSamples updated to:', newSelection);
      return newSelection;
    });
  };

  const selectAllSamples = () => {
    if (samplesList.length > 0) {
      setSelectedSamples(samplesList.map(sample => sample.id));
    }
  };

  const clearSampleSelection = () => {
    setSelectedSamples([]);
  };

const deleteSelectedSamples = async (sampleIds?: string[] | string) => {
    // Ensure we have an array
    let samplesToDelete: any[] = [];
    if (sampleIds) {
      samplesToDelete = Array.isArray(sampleIds) ? sampleIds : [sampleIds];
    } else {
      samplesToDelete = Array.isArray(selectedSamples) ? selectedSamples : [];
    }
    
    if (samplesToDelete.length === 0) {
      alert('Vui lòng chọn mẫu cần xóa');
      return;
    }

    // Extract IDs if they are objects, otherwise use as-is
    const validSampleIds = samplesToDelete
      .map(item => {
        // If item is an object, extract the ID
        if (typeof item === 'object' && item !== null) {
          return item.id || item.sample_id || null;
        }
        // If item is a string, use it directly
        return item;
      })
      .filter(id => id != null && id !== '');
    
    if (validSampleIds.length === 0) {
      alert('Không có mẫu hợp lệ để xóa');
      return;
    }

    const count = validSampleIds.length;
    const message = count === 1 
      ? 'Bạn có chắc muốn xóa mẫu này?' 
      : `Bạn có chắc muốn xóa ${count} mẫu đã chọn?`;
    
    if (!confirm(message)) {
      return;
    }

    setLoading(true);
    try {
      console.log('Deleting samples:', validSampleIds, 'from dataset:', selectedDatasetForAnnotation);
      
      const response = await fetch('/AA/A0/AAA0_0203/bulk-delete-samples', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          template_id: selectedDatasetForAnnotation,
          sample_ids: validSampleIds
        })
      });

      console.log('Delete response status:', response.status);
      const result = await response.json();
      console.log('Delete result:', result);
      
      if (result.status === 'success') {
        const deletedCount = result.data?.deleted_count || validSampleIds.length;
        const msg = deletedCount === 1 ? 'Đã xóa 1 mẫu' : `Đã xóa ${deletedCount} mẫu`;
        setResults({ message: msg });
        setSelectedSamples([]);
        setSampleOperationMode(false);
        await loadSamplesForDataset(selectedDatasetForAnnotation);
        await loadTrainingData(); // Refresh counts
      } else {
        console.error('Delete failed:', result);
        alert('Lỗi khi xóa mẫu: ' + (result.message || 'Unknown error'));
      }
    } catch (error) {
      console.error('Error deleting samples:', error);
      alert('Có lỗi xảy ra khi xóa mẫu. Vui lòng kiểm tra console.');
    } finally {
      setLoading(false);
    }
  };

  const addAnnotation = () => {
    if (!selectedText || !selectedEntityType) return;

    const newAnnotation = {
      text: selectedText,
      label: selectedEntityType,
      start: currentAnnotationText.indexOf(selectedText),
      end: currentAnnotationText.indexOf(selectedText) + selectedText.length
    };

    setAnnotations(prev => [...prev, newAnnotation]);
    setSelectedText('');
    setSelectedEntityType('');
  };

  const removeAnnotation = (index) => {
    setAnnotations(prev => prev.filter((_, i) => i !== index));
  };

  const toggleEditMode = () => {
    if (isEditingText) {
      // Save changes
      setCurrentAnnotationText(editedText);
      // Update annotation positions if text changed
      updateAnnotationPositions(currentAnnotationText, editedText);
    } else {
      // Start editing
      setEditedText(currentAnnotationText);
    }
    setIsEditingText(!isEditingText);
  };

  const updateAnnotationPositions = (oldText, newText) => {
    if (oldText === newText) return;

    // Simple approach: try to find annotations in new text
    // This is a basic implementation - more sophisticated diffing could be added
    const updatedAnnotations = [];

    for (const annotation of annotations) {
      const oldEntityText = oldText.slice(annotation.start, annotation.end);
      const newStart = newText.indexOf(oldEntityText);

      if (newStart !== -1) {
        updatedAnnotations.push({
          ...annotation,
          start: newStart,
          end: newStart + oldEntityText.length
        });
      }
    }

    setAnnotations(updatedAnnotations);
  };

  // Text selection handlers
  const handleCharacterMouseDown = (charIndex) => {
    setSelectionStart(charIndex);
    setSelectionEnd(charIndex);
    setIsSelecting(true);
    setSelectedText(currentAnnotationText.slice(charIndex, charIndex + 1));
  };

  const handleCharacterMouseEnter = (charIndex) => {
    if (isSelecting && selectionStart !== null) {
      setSelectionEnd(charIndex);
      const start = Math.min(selectionStart, charIndex);
      const end = Math.max(selectionStart, charIndex) + 1;
      setSelectedText(currentAnnotationText.slice(start, end));
    }
  };

  const handleCharacterMouseUp = () => {
    setIsSelecting(false);
  };

  const clearSelection = () => {
    setSelectionStart(null);
    setSelectionEnd(null);
    setSelectedText('');
  };

  const selectAll = () => {
    setSelectionStart(0);
    setSelectionEnd(currentAnnotationText.length - 1);
    setSelectedText(currentAnnotationText);
  };

  const selectWord = (charIndex) => {
    // Find word boundaries
    let start = charIndex;
    let end = charIndex;

    // Find start of word
    while (start > 0 && /\w/.test(currentAnnotationText[start - 1])) {
      start--;
    }

    // Find end of word
    while (end < currentAnnotationText.length - 1 && /\w/.test(currentAnnotationText[end + 1])) {
      end++;
    }

    setSelectionStart(start);
    setSelectionEnd(end);
    setSelectedText(currentAnnotationText.slice(start, end + 1));
  };

  const handleTextChange = (newText) => {
    setEditedText(newText);
  };

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(currentAnnotationText);
      setResults({ message: 'Đã sao chép văn bản vào clipboard' });
    } catch (err) {
      console.error('Failed to copy text: ', err);
    }
  };

  const pasteFromClipboard = async () => {
    try {
      const text = await navigator.clipboard.readText();
      setEditedText(text);
      if (!isEditingText) {
        setCurrentAnnotationText(text);
      }
    } catch (err) {
      console.error('Failed to paste text: ', err);
    }
  };

  const renderAnnotatedText = (text, annotations) => {
    if (!text) return null;

    const sortedAnnotations = [...(annotations || [])].sort((a, b) => a.start - b.start);
    const parts = [];
    let lastIndex = 0;

    // Add annotated entities
    sortedAnnotations.forEach((annotation, idx) => {
      // Add text before annotation
      if (annotation.start > lastIndex) {
        parts.push({
          text: text.slice(lastIndex, annotation.start),
          type: 'plain',
          start: lastIndex,
          end: annotation.start
        });
      }

      // Add annotated text
      const entityConfig = getEntityTypeConfig(annotation.label);
      parts.push({
        text: text.slice(annotation.start, annotation.end),
        type: 'annotated',
        label: annotation.label,
        start: annotation.start,
        end: annotation.end,
        index: idx
      });

      lastIndex = annotation.end;
    });

    // Add remaining text
    if (lastIndex < text.length) {
      parts.push({
        text: text.slice(lastIndex),
        type: 'plain',
        start: lastIndex,
        end: text.length
      });
    }

    return (
      <div className="text-sm leading-relaxed whitespace-pre-wrap select-none">
        {parts.map((part, partIndex) => {
          if (part.type === 'annotated') {
            const entityConfig = getEntityTypeConfig(part.label);
            const IconComponent = entityConfig?.icon;
            return (
              <span
                key={partIndex}
                className={cn(
                  "inline px-0.5 py-0.5 rounded text-xs font-medium cursor-pointer hover:opacity-80",
                  entityConfig?.color || 'bg-gray-200'
                )}
                onClick={() => removeAnnotation(part.index)}
                title={`Click để xóa nhãn ${entityConfig?.label || part.label}`}
              >
                {part.text.split('').map((char, charIndex) => {
                  const globalIndex = part.start + charIndex;
                  const isSelected = selectionStart !== null && selectionEnd !== null &&
                    globalIndex >= Math.min(selectionStart, selectionEnd) &&
                    globalIndex <= Math.max(selectionStart, selectionEnd);

                  return (
                    <span
                      key={charIndex}
                      className={cn(
                        "cursor-pointer hover:bg-blue-200",
                        isSelected && "bg-blue-300"
                      )}
                      onMouseDown={(e) => {
                        e.preventDefault();
                        handleCharacterMouseDown(globalIndex);
                      }}
                      onMouseEnter={() => handleCharacterMouseEnter(globalIndex)}
                      onMouseUp={handleCharacterMouseUp}
                      onDoubleClick={() => selectWord(globalIndex)}
                    >
                      {char}
                    </span>
                  );
                })}
              </span>
            );
          }

          // Render plain text character by character
          return part.text.split('').map((char, charIndex) => {
            const globalIndex = part.start + charIndex;
            const isSelected = selectionStart !== null && selectionEnd !== null &&
              globalIndex >= Math.min(selectionStart, selectionEnd) &&
              globalIndex <= Math.max(selectionStart, selectionEnd);

            return (
              <span
                key={charIndex}
                className={cn(
                  "cursor-pointer hover:bg-blue-100",
                  isSelected && "bg-blue-200"
                )}
                onMouseDown={(e) => {
                  e.preventDefault();
                  handleCharacterMouseDown(globalIndex);
                }}
                onMouseEnter={() => handleCharacterMouseEnter(globalIndex)}
                onMouseUp={handleCharacterMouseUp}
                onDoubleClick={() => selectWord(globalIndex)}
              >
                {char}
              </span>
            );
          });
        })}
      </div>
    );
  };

  const toggleSection = (section) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  const renderDatasetAnnotatedText = (text, entities) => {
    if (!entities || entities.length === 0) return text;

    const sortedEntities = [...entities].sort((a, b) => a.start - b.start);
    const parts = [];
    let lastIndex = 0;

    sortedEntities.forEach((entity, idx) => {
      // Add text before entity
      if (entity.start > lastIndex) {
        parts.push(text.slice(lastIndex, entity.start));
      }

      // Add annotated entity
      const entityType = getEntityTypeConfig(entity.label);
      parts.push(
        <span key={idx} className={cn("inline-block px-1 py-0.5 mx-0.5 rounded border text-xs font-medium", entityType.color)}>
          {text.slice(entity.start, entity.end)}
          <Badge variant="outline" className="ml-1 text-xs">
            {entityType.label}
          </Badge>
        </span>
      );

      lastIndex = entity.end;
    });

    // Add remaining text
    if (lastIndex < text.length) {
      parts.push(text.slice(lastIndex));
    }

    return parts;
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
              <Brain className="h-8 w-8 text-blue-600" />
              NER Training - Y Tế
            </h1>
            <p className="text-muted-foreground">
              Huấn luyện mô hình nhận diện thực thể y tế từ giấy ra viện và tài liệu bệnh án
            </p>
            {lastRefreshTime && (
              <p className="text-xs text-muted-foreground mt-1">
                Last refresh: {lastRefreshTime.toLocaleTimeString()} • Auto-refresh: 30s
              </p>
            )}
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleManualRefresh}
              disabled={refreshing}
            >
              {refreshing ? (
                <>
                  <div className="animate-spin mr-2 h-4 w-4 border-2 border-primary border-t-transparent rounded-full" />
                  Đang tải...
                </>
              ) : (
                <>
                  <Activity className="h-4 w-4 mr-2" />
                  Làm mới
                </>
              )}
            </Button>
            <Badge variant={canTrain ? "default" : "secondary"}>
              {totalApprovedSamples} mẫu đã duyệt
            </Badge>
          </div>
        </div>

        {/* Overview Section */}
        <Collapsible open={expandedSections.overview} onOpenChange={() => toggleSection('overview')}>
          <Card>
            <CollapsibleTrigger asChild>
              <CardHeader className="cursor-pointer hover:bg-gray-50">
                <CardTitle className="flex items-center gap-2">
                  {expandedSections.overview ? <ChevronDown className="h-5 w-5" /> : <ChevronRight className="h-5 w-5" />}
                  <BarChart3 className="h-5 w-5" />
                  Tổng quan
                </CardTitle>
                <CardDescription>
                  Thống kê dữ liệu huấn luyện và tiến độ training
                </CardDescription>
              </CardHeader>
            </CollapsibleTrigger>
            <CollapsibleContent>
              <CardContent>
                {/* Training Data Summary */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                  <div className="text-center p-4 border rounded-lg">
                    <div className="text-2xl font-bold text-blue-600">{trainingData.length}</div>
                    <div className="text-sm text-muted-foreground">Tập dữ liệu</div>
                  </div>
                  <div className="text-center p-4 border rounded-lg">
                    <div className="text-2xl font-bold text-green-600">
                      {entityStatistics?.total_samples || 0}
                    </div>
                    <div className="text-sm text-muted-foreground">Tổng mẫu</div>
                  </div>
                  <div className="text-center p-4 border rounded-lg">
                    <div className="text-2xl font-bold text-purple-600">
                      {entityStatistics?.total_entities || 0}
                    </div>
                    <div className="text-sm text-muted-foreground">Tổng thực thể</div>
                  </div>
                  <div className="text-center p-4 border rounded-lg">
                    <div className="text-2xl font-bold text-orange-600">
                      {trainingProgress?.status === 'running' ? 'Đang train' : 'Sẵn sàng'}
                    </div>
                    <div className="text-sm text-muted-foreground">Trạng thái</div>
                  </div>
                </div>

                {/* Entity Statistics */}
                <div className="space-y-4">
                  <h4 className="font-semibold text-lg">Phân bố các loại thực thể</h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {entityStatistics?.entity_breakdown?.map((entity) => {
                      const entityConfig = getEntityTypeConfig(entity.entity_type);
                      const bgColor = entityConfig.color?.split(' ')[0] || 'bg-gray-400';
                      return (
                        <div key={entity.entity_type} className="flex items-center justify-between p-3 border rounded-lg">
                          <div className="flex items-center gap-2">
                            <div className={`w-3 h-3 rounded ${bgColor}`}></div>
                            <div>
                              <div className="font-medium text-sm">{entity.vietnamese_label}</div>
                              <div className="text-xs text-muted-foreground">{entity.entity_type}</div>
                            </div>
                          </div>
                          <div className="text-lg font-bold text-blue-600">
                            {entity.count}
                          </div>
                        </div>
                      );
                    }) || (
                      <div className="col-span-full text-center py-8 text-muted-foreground">
                        <BarChart3 className="h-12 w-12 mx-auto mb-4 opacity-50" />
                        <p>Chưa có dữ liệu thống kê thực thể</p>
                        <p className="text-sm">Dữ liệu sẽ được cập nhật khi có training data</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Training Progress */}
                {trainingProgress && (
                  <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-semibold text-blue-900">Tiến độ huấn luyện</h4>
                      <Badge variant={trainingProgress.status === 'running' ? 'default' : trainingProgress.status === 'completed' ? 'secondary' : 'destructive'}>
                        {trainingProgress.status}
                      </Badge>
                    </div>
                    <Progress value={trainingProgress.progress || 0} className="mb-2" />
                    <div className="text-sm text-blue-700">
                      Epoch {trainingProgress.current_epoch || 0} / {trainingConfig.epochs} • 
                      Loss: {trainingProgress.loss?.toFixed(4) || 'N/A'} • 
                      F1: {trainingProgress.f1_score?.toFixed(4) || 'N/A'}
                    </div>
                  </div>
                )}
              </CardContent>
            </CollapsibleContent>
          </Card>
        </Collapsible>

        {/* Entity Types Reference */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Tag className="h-5 w-5" />
              Loại thực thể
            </CardTitle>
            <CardDescription>
              {initializingEntityTypes 
                ? 'Đang khởi tạo entity types mặc định...' 
                : entityTypes.length > 0 
                  ? `${entityTypes.length} entity types được load động` 
                  : 'Các loại thông tin sẽ được nhận diện trong tài liệu'
              }
            </CardDescription>
          </CardHeader>
          <CardContent>
            {initializingEntityTypes ? (
              <div className="text-center py-8 text-muted-foreground">
                <Tag className="h-12 w-12 mx-auto mb-4 opacity-50 animate-pulse" />
                <p>Đang tạo entity types mặc định...</p>
                <p className="text-sm">Vui lòng đợi...</p>
              </div>
            ) : entityTypes.length > 0 ? (
              <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
                {entityTypes.map((entityType) => {
                  const config = convertEntityTypeToLegacy(entityType);
                  return (
                    <div key={entityType.entity_code} className={cn("p-3 rounded-lg border text-center", config.color)}>
                      <Tag className="h-6 w-6 mx-auto mb-2" />
                      <div className="text-xs font-medium">{config.label}</div>
                      <div className="text-xs opacity-75">{entityType.entity_code}</div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
                {Object.entries(MEDICAL_ENTITY_TYPES).map(([key, config]) => {
                  const IconComponent = config.icon;
                  return (
                    <div key={key} className={cn("p-3 rounded-lg border text-center", config.color)}>
                      <IconComponent className="h-6 w-6 mx-auto mb-2" />
                      <div className="text-xs font-medium">{config.label}</div>
                      <div className="text-xs opacity-75">{key}</div>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>

        <Tabs defaultValue="datasets" className="space-y-6">
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="datasets">Dữ liệu</TabsTrigger>
            <TabsTrigger value="annotation">Gán nhãn</TabsTrigger>
            <TabsTrigger value="entity-types">Entity Types</TabsTrigger>
            <TabsTrigger value="training">Huấn luyện</TabsTrigger>
            <TabsTrigger value="evaluation">Đánh giá</TabsTrigger>
          </TabsList>

          {/* Datasets Tab */}
          <TabsContent value="datasets" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Dataset List */}
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle className="flex items-center gap-2">
                        <Database className="h-5 w-5" />
                        Tập dữ liệu huấn luyện
                      </CardTitle>
                      <CardDescription>
                        Dữ liệu từ AAA0_0202 được chuyển đổi tự động
                      </CardDescription>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button
                        variant={bulkOperationMode ? "default" : "outline"}
                        size="sm"
                        onClick={() => setBulkOperationMode(!bulkOperationMode)}
                      >
                        {bulkOperationMode ? <X className="h-4 w-4 mr-2" /> : <CheckCircle className="h-4 w-4 mr-2" />}
                        {bulkOperationMode ? 'Hủy' : 'Chọn nhiều'}
                      </Button>
                    </div>
                  </div>

                  {bulkOperationMode && (
                    <div className="flex items-center gap-2 mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                      <div className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={selectedDatasets.length === trainingData.length && trainingData.length > 0}
                          onChange={(e) => e.target.checked ? selectAllDatasets() : clearDatasetSelection()}
                          className="rounded"
                        />
                        <span className="text-sm font-medium">Chọn tất cả ({selectedDatasets.length}/{trainingData.length})</span>
                      </div>
                      <div className="flex gap-2 ml-auto">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={exportSelectedDatasets}
                          disabled={selectedDatasets.length === 0 || loading}
                        >
                          <Download className="h-4 w-4 mr-2" />
                          Xuất ({selectedDatasets.length})
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={deleteSelectedDatasets}
                          disabled={selectedDatasets.length === 0 || loading}
                        >
                          <X className="h-4 w-4 mr-2" />
                          Xóa ({selectedDatasets.length})
                        </Button>
                      </div>
                    </div>
                  )}
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {trainingData.map((dataset) => (
                      <Card key={dataset.template_id} className={cn("p-4", bulkOperationMode && selectedDatasets.includes(dataset.template_id) && "ring-2 ring-blue-500 bg-blue-50")}>
                        <div className="flex items-center justify-between mb-4">
                          <div className="flex items-center gap-3">
                            {bulkOperationMode && (
                              <input
                                type="checkbox"
                                checked={selectedDatasets.includes(dataset.template_id)}
                                onChange={() => toggleDatasetSelection(dataset.template_id)}
                                className="rounded"
                              />
                            )}
                            <div>
                              <h3 className="font-semibold text-lg">{dataset.template_id}</h3>
                              <p className="text-sm text-muted-foreground">
                                {dataset.samples_count} mẫu tổng • {dataset.approved_count} mẫu đã duyệt • Cập nhật: {new Date(dataset.created_at).toLocaleDateString()}
                              </p>
                            </div>
                          </div>
                          {!bulkOperationMode && (
                            <div className="flex gap-2">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={async () => {
                                  if (selectedDataset === dataset.template_id) {
                                    // Hide samples
                                    setSelectedDataset(null);
                                  } else {
                                    // Load and show samples
                                    await loadDatasetSamplesForView(dataset.template_id);
                                    setSelectedDataset(dataset.template_id);
                                  }
                                }}
                                disabled={loading}
                              >
                                <Eye className="h-4 w-4 mr-2" />
                                {selectedDataset === dataset.template_id ? 'Ẩn' : 'Xem'}
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setEditingDataset(dataset)}
                              >
                                <Edit className="h-4 w-4 mr-2" />
                                Chỉnh sửa
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => exportDataset(dataset.template_id)}
                                disabled={loading}
                              >
                                <Download className="h-4 w-4 mr-2" />
                                Xuất
                              </Button>
                            </div>
                          )}
                        </div>

                        {selectedDataset === dataset.template_id && (
                          <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                            <h4 className="font-medium mb-2">Chi tiết mẫu (3 mẫu đầu tiên)</h4>
                            {datasetSamplesCache[dataset.template_id] ? (
                              <div className="space-y-3 max-h-60 overflow-y-auto">
                                {datasetSamplesCache[dataset.template_id].map((sample, idx) => (
                                  <div key={idx} className="p-3 bg-white rounded border">
                                    <div className="text-sm text-gray-600 mb-2 line-clamp-3">
                                      {renderAnnotatedText(sample.text, sample.annotations)}
                                    </div>
                                    <div className="flex flex-wrap gap-1">
                                      {sample.annotations?.map((entity, eidx) => {
                                        const entityType = getEntityTypeConfig(entity.label);
                                        return (
                                          <Badge key={eidx} variant="outline" className={cn("text-xs", entityType?.color)}>
                                            {entityType?.label || entity.label}: {sample.text.slice(entity.start, entity.end)}
                                          </Badge>
                                        );
                                      })}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <div className="text-center py-4 text-sm text-muted-foreground">
                                Đang tải...
                              </div>
                            )}
                          </div>
                        )}
                      </Card>
                    ))}

                    {trainingData.length === 0 && (
                      <div className="text-center py-8 text-muted-foreground">
                        <Database className="h-12 w-12 mx-auto mb-4 opacity-50" />
                        <p>Chưa có dữ liệu huấn luyện</p>
                        <p className="text-sm">Dữ liệu sẽ được tạo tự động từ AAA0_0202</p>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>

              {/* Dataset Stats */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Target className="h-5 w-5" />
                    Thống kê thực thể
                  </CardTitle>
                  <CardDescription>
                    Phân bố các loại thực thể trong dữ liệu
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {getAvailableEntityTypes().map(({code, label, config}) => {
                      const count = trainingData.reduce((sum, dataset) => {
                        return sum + (dataset.samples?.reduce((sampleSum, sample) => {
                          return sampleSum + (sample.entities?.filter(e => e.label === code).length || 0);
                        }, 0) || 0);
                      }, 0);

                      const IconComponent = config.icon;
                      
                      return (
                        <div key={code} className="flex items-center justify-between p-3 border rounded-lg">
                          <div className="flex items-center gap-3">
                            <div className={cn("p-2 rounded-lg", config.color)}>
                              <IconComponent className="h-4 w-4" />
                            </div>
                            <div>
                              <div className="font-medium text-sm">{label}</div>
                              <div className="text-xs text-muted-foreground">{code}</div>
                            </div>
                          </div>
                          <Badge variant="secondary">{count}</Badge>
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* Annotation Tab */}
          <TabsContent value="annotation" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Sample Browser */}
              <Card className="lg:col-span-1">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle className="flex items-center gap-2">
                        <FileText className="h-5 w-5" />
                        Duyệt mẫu huấn luyện
                      </CardTitle>
                      <CardDescription>
                        Chọn mẫu để xem và chỉnh sửa nhãn
                      </CardDescription>
                    </div>
                    {selectedDatasetForAnnotation && (
                      <Button
                        variant={sampleOperationMode ? "default" : "outline"}
                        size="sm"
                        onClick={() => {
                          setSampleOperationMode(!sampleOperationMode);
                          setSelectedSamples([]);
                        }}
                      >
                        {sampleOperationMode ? <X className="h-4 w-4 mr-2" /> : <CheckCircle className="h-4 w-4 mr-2" />}
                        {sampleOperationMode ? 'Hủy' : 'Chọn nhiều'}
                      </Button>
                    )}
                  </div>

                  {sampleOperationMode && selectedDatasetForAnnotation && (
                    <div className="mt-3 p-2 bg-red-50 border border-red-200 rounded-lg">
                      <div className="flex items-center gap-2 mb-2">
                        <input
                          type="checkbox"
                          checked={selectedSamples.length === samplesList.length && samplesList.length > 0}
                          onChange={(e) => e.target.checked ? selectAllSamples() : clearSampleSelection()}
                          className="rounded"
                        />
                        <span className="text-sm font-medium text-red-800">Chọn tất cả ({selectedSamples.length}/{samplesList.length})</span>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          variant="default"
                          size="sm"
                          onClick={() => bulkApproveSelectedSamples()}
                          disabled={selectedSamples.length === 0 || loading}
                        >
                          <CheckCircle className="h-4 w-4 mr-2" />
                          Duyệt ({selectedSamples.length})
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => deleteSelectedSamples()}
                          disabled={selectedSamples.length === 0 || loading}
                        >
                          <X className="h-4 w-4 mr-2" />
                          Xóa ({selectedSamples.length})
                        </Button>
                      </div>
                    </div>
                  )}
                </CardHeader>
                <CardContent>
                  {/* Approval Status Filter */}
                  {selectedDatasetForAnnotation && samplesList.length > 0 && (
                    <div className="mb-4 flex items-center gap-2">
                      <Label className="text-sm font-medium">Lọc theo trạng thái:</Label>
                      <div className="flex gap-1">
                        <Button
                          variant={approvalFilter === 'all' ? 'default' : 'outline'}
                          size="sm"
                          onClick={() => setApprovalFilter('all')}
                          className="h-8"
                        >
                          Tất cả ({samplesList.length})
                        </Button>
                        <Button
                          variant={approvalFilter === 'approved' ? 'default' : 'outline'}
                          size="sm"
                          onClick={() => setApprovalFilter('approved')}
                          className="h-8"
                        >
                          <CheckCircle className="h-3 w-3 mr-1" />
                          Đã duyệt ({samplesList.filter(s => s.approved).length})
                        </Button>
                        <Button
                          variant={approvalFilter === 'pending' ? 'default' : 'outline'}
                          size="sm"
                          onClick={() => setApprovalFilter('pending')}
                          className="h-8"
                        >
                          <AlertCircle className="h-3 w-3 mr-1" />
                          Chưa duyệt ({samplesList.filter(s => !s.approved).length})
                        </Button>
                      </div>
                    </div>
                  )}

                  <div className="space-y-3 max-h-96 overflow-y-auto">
                    {trainingData.map((dataset) => (
                      <div key={dataset.template_id} className="space-y-2">
                        <div className="flex items-center justify-between">
                          <h4 className="font-medium text-sm text-gray-700">{dataset.template_id}</h4>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => loadSamplesForDataset(dataset.template_id)}
                            disabled={loading}
                          >
                            <Eye className="h-3 w-3 mr-1" />
                            Tải
                          </Button>
                        </div>

                        {selectedDatasetForAnnotation === dataset.template_id && (
                          <div className="space-y-1 ml-4">
                            {samplesList
                              .filter(sample => {
                                if (approvalFilter === 'all') return true;
                                if (approvalFilter === 'approved') return sample.approved === true;
                                if (approvalFilter === 'pending') return sample.approved !== true;
                                return true;
                              })
                              .map((sample, idx) => {
                              const sampleId = sample.id || sample.sample_id || `sample_${idx}`;
                              const isSelected = selectedSamples.includes(sampleId);

                              return (
                                <div
                                  key={idx}
                                  className={cn(
                                    "text-xs p-2 rounded cursor-pointer border transition-all",
                                    sampleOperationMode && isSelected && "ring-2 ring-red-500 bg-red-50 border-red-300",
                                    currentSample === sample ? "bg-blue-100 border-blue-300" : "bg-gray-50 hover:bg-blue-50"
                                  )}
                                  onClick={() => sampleOperationMode ? toggleSampleSelection(sampleId) : selectSampleForAnnotation(sample)}
                                >
                                  {sampleOperationMode && (
                                    <div className="flex items-center gap-2 mb-1">
                                      <input
                                        type="checkbox"
                                        checked={isSelected}
                                        onChange={(e) => {
                                          e.stopPropagation();
                                          toggleSampleSelection(sampleId);
                                        }}
                                        className="rounded"
                                      />
                                      <span className="text-xs font-medium text-red-600">#{idx + 1}</span>
                                    </div>
                                  )}
                                  <div className="truncate">
                                    {sample.text.substring(0, 60)}...
                                  </div>
                                  <div className="text-gray-500 flex justify-between items-center">
                                    <div className="flex items-center gap-2">
                                      <span>{sample.annotations?.length || 0} nhãn</span>
                                      {sample.approved ? (
                                        <Badge variant="default" className="bg-green-100 text-green-800 text-[10px] px-1.5 py-0">
                                          ✓ Đã duyệt
                                        </Badge>
                                      ) : (
                                        <Badge variant="secondary" className="bg-yellow-100 text-yellow-800 text-[10px] px-1.5 py-0">
                                          ⏳ Chưa duyệt
                                        </Badge>
                                      )}
                                    </div>
                                    {!sampleOperationMode && (
                                      <Button
                                        variant="ghost"
                                        size="sm"
                                        className="h-4 w-4 p-0 text-red-500 hover:text-red-700"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          if (confirm('Bạn có chắc muốn xóa mẫu này?')) {
                                            deleteSelectedSamples([sampleId]);
                                          }
                                        }}
                                      >
                                        <X className="h-3 w-3" />
                                      </Button>
                                    )}
                                  </div>
                                </div>
                              );
                            })}

                            {/* Pagination info and Load More button */}
                            <div className="pt-2 border-t">
                              <div className="text-xs text-gray-500 text-center py-1">
                                Đã tải: {samplesList.length} / {totalSamples} mẫu
                              </div>
                              {hasMore && (
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="w-full text-xs"
                                  onClick={loadMoreSamples}
                                  disabled={loading}
                                >
                                  {loading ? 'Đang tải...' : 'Tải thêm'}
                                </Button>
                              )}
                            </div>
                          </div>
                        )}
                      </div>
                    ))}

                    {trainingData.length === 0 && (
                      <div className="text-center py-8 text-muted-foreground">
                        <FileText className="h-8 w-8 mx-auto mb-2 opacity-50" />
                        <p>Chưa có dữ liệu huấn luyện</p>
                        <p className="text-sm">Dữ liệu sẽ được thu thập từ AAA0_0202</p>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>

              {/* Annotation Editor */}
              <Card className="lg:col-span-2">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle className="flex items-center gap-2">
                        <Tag className="h-5 w-5" />
                        Chỉnh sửa nhãn thực thể
                      </CardTitle>
                      <CardDescription>
                        Chọn văn bản để gán nhãn hoặc chỉnh sửa nhãn hiện có
                      </CardDescription>
                    </div>
                    {currentSample && (
                      <div className="flex items-center gap-2 text-sm text-green-600">
                        <CheckCircle className="h-4 w-4" />
                        <span>Tự động lưu</span>
                      </div>
                    )}
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {/* Text Display/Edit */}
                  <div className="min-h-32 p-6 bg-white rounded-lg border-2 border-gray-200 shadow-inner">
                    {currentAnnotationText ? (
                      isEditingText ? (
                        <Textarea
                          value={editedText}
                          onChange={(e) => handleTextChange(e.target.value)}
                          className="min-h-32 text-sm leading-relaxed resize-none border-0 shadow-none focus:ring-0 bg-transparent"
                          placeholder="Chỉnh sửa văn bản..."
                        />
                      ) : (
                        <div className="text-sm leading-relaxed whitespace-pre-wrap">
                          {renderAnnotatedText(currentAnnotationText, annotations)}
                        </div>
                      )
                    ) : (
                      <div className="text-center text-muted-foreground py-8">
                        <FileText className="h-8 w-8 mx-auto mb-2 opacity-50" />
                        <p>Chọn một mẫu từ danh sách bên trái</p>
                      </div>
                    )}
                  </div>

                  {/* Text Edit Controls */}
                  {currentAnnotationText && (
                    <div className="flex gap-2 flex-wrap">
                      <Button
                        onClick={toggleEditMode}
                        variant={isEditingText ? "default" : "outline"}
                        size="sm"
                      >
                        {isEditingText ? (
                          <>
                            <Save className="h-4 w-4 mr-2" />
                            Lưu chỉnh sửa
                          </>
                        ) : (
                          <>
                            <Edit className="h-4 w-4 mr-2" />
                            Chỉnh sửa văn bản
                          </>
                        )}
                      </Button>

                      <Button
                        onClick={copyToClipboard}
                        variant="outline"
                        size="sm"
                      >
                        <Copy className="h-4 w-4 mr-2" />
                        Sao chép
                      </Button>

                      <Button
                        onClick={pasteFromClipboard}
                        variant="outline"
                        size="sm"
                      >
                        <Clipboard className="h-4 w-4 mr-2" />
                        Dán
                      </Button>
                    </div>
                  )}

                  {/* Selection Controls */}
                  {currentAnnotationText && !isEditingText && (
                    <div className="bg-blue-50 p-3 rounded-lg border border-blue-200">
                      <div className="flex items-center gap-2 mb-2">
                        <Tag className="h-4 w-4 text-blue-600" />
                        <Label className="text-sm font-medium text-blue-800">Công cụ chọn văn bản:</Label>
                      </div>
                      <div className="flex gap-2 flex-wrap">
                        <Button
                          onClick={clearSelection}
                          variant="outline"
                          size="sm"
                          disabled={selectionStart === null}
                          className="border-blue-300 text-blue-700 hover:bg-blue-100"
                        >
                          <X className="h-4 w-4 mr-2" />
                          Xóa chọn
                        </Button>

                        <Button
                          onClick={selectAll}
                          variant="outline"
                          size="sm"
                          className="border-blue-300 text-blue-700 hover:bg-blue-100"
                        >
                          <CheckCircle className="h-4 w-4 mr-2" />
                          Chọn tất cả
                        </Button>
                      </div>
                      <div className="text-xs text-blue-600 mt-2 flex items-center gap-1">
                        <span className="font-medium">💡 Hướng dẫn:</span>
                        <span>Click để chọn ký tự • Kéo để chọn nhiều • Double-click để chọn từ</span>
                      </div>
                    </div>
                  )}

                  {/* Selection Display */}
                  {currentAnnotationText && !isEditingText && (
                    <div className="bg-green-50 p-4 rounded-lg border border-green-200">
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <Label className="text-sm font-medium text-green-800 flex items-center gap-2">
                            <Target className="h-4 w-4" />
                            Văn bản được chọn ({selectedText.length} ký tự)
                          </Label>
                          <Input
                            value={selectedText}
                            readOnly
                            placeholder="Click và kéo trên văn bản để chọn..."
                            className={cn(
                              "bg-white border-green-300 mt-1",
                              selectedText && "bg-green-50 border-green-400"
                            )}
                          />
                          {selectionStart !== null && selectionEnd !== null && (
                            <div className="text-xs text-green-600 mt-1">
                              Vị trí: {Math.min(selectionStart, selectionEnd)} - {Math.max(selectionStart, selectionEnd)}
                            </div>
                          )}
                        </div>
                        <div>
                          <Label className="text-sm font-medium text-green-800">Loại thực thể</Label>
                          <Select value={selectedEntityType} onValueChange={setSelectedEntityType}>
                            <SelectTrigger className="mt-1 border-green-300">
                              <SelectValue placeholder="Chọn loại...">
                                {selectedEntityType && (
                                  <div className="flex items-center gap-2">
                                    {entityTypesMap[selectedEntityType]?.icon && (
                                      <span className="text-base">{entityTypesMap[selectedEntityType].icon}</span>
                                    )}
                                    <span>{entityTypesMap[selectedEntityType]?.display_label || selectedEntityType}</span>
                                  </div>
                                )}
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {getAvailableEntityTypes().map(({code, label}) => {
                                const entityType = entityTypesMap[code];
                                return (
                                  <SelectItem key={code} value={code}>
                                    <div className="flex items-center gap-2">
                                      {entityType?.icon && (
                                        <span className="text-base">{entityType.icon}</span>
                                      )}
                                      <span>{label}</span>
                                    </div>
                                  </SelectItem>
                                );
                              })}
                            </SelectContent>
                          </Select>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Action Buttons */}
                  <div className="flex gap-2">
                    <Button
                      onClick={addAnnotation}
                      disabled={!selectedText || !selectedEntityType}
                      variant="outline"
                    >
                      <Plus className="h-4 w-4 mr-2" />
                      Thêm nhãn
                    </Button>

                    <Button
                      onClick={saveSampleAnnotations}
                      disabled={!currentAnnotationText || loading}
                      variant="outline"
                    >
                      <Save className="h-4 w-4 mr-2" />
                      Lưu thay đổi
                    </Button>

                    <Button
                      onClick={approveSample}
                      disabled={!currentSample || loading}
                      variant="outline"
                    >
                      <CheckCircle className="h-4 w-4 mr-2" />
                      Duyệt mẫu
                    </Button>

                    <Button
                      onClick={rejectSample}
                      disabled={!currentSample || loading}
                      variant="outline"
                    >
                      <X className="h-4 w-4 mr-2" />
                      Từ chối mẫu
                    </Button>
                  </div>

                  {/* Current Annotations */}
                  {annotations.length > 0 && (
                    <div className="space-y-2">
                      <Label>Nhãn hiện có ({annotations.length})</Label>
                      <div className="flex flex-wrap gap-2">
                        {annotations.map((annotation, idx) => {
                          const entityType = getEntityTypeConfig(annotation.label);
                          return (
                            <Badge
                              key={idx}
                              variant="outline"
                              className={cn("cursor-pointer hover:bg-red-50", entityType?.color)}
                              onClick={() => removeAnnotation(idx)}
                            >
                              {annotation.text} ({entityType?.label})
                              <X className="h-3 w-3 ml-1" />
                            </Badge>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

            {/* Bulk Actions */}
            <Card>
              <CardHeader>
                <CardTitle>Thao tác hàng loạt</CardTitle>
                <CardDescription>
                  Xử lý nhiều mẫu cùng lúc
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex gap-4">
                  <Button 
                    variant="outline"
                    onClick={bulkApproveAllSamples}
                    disabled={!selectedDatasetForAnnotation || loading}
                  >
                    <CheckCircle className="h-4 w-4 mr-2" />
                    Duyệt tất cả mẫu
                  </Button>
                  <Button variant="outline">
                    <Download className="h-4 w-4 mr-2" />
                    Xuất dữ liệu đã duyệt
                  </Button>
                  <Button variant="outline">
                    <Settings className="h-4 w-4 mr-2" />
                    Tự động gán nhãn
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Entity Types Tab */}
          <TabsContent value="entity-types" className="space-y-6">
            {/* Dataset Selector for Entity Types */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Database className="h-5 w-5" />
                  Chọn Dataset
                </CardTitle>
                <CardDescription>
                  Chọn dataset để quản lý entity types. Global entity types áp dụng cho tất cả datasets.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-4">
                  <div className="flex-1">
                    <Label htmlFor="entity-dataset-select">Dataset</Label>
                    <Select
                      value={selectedDatasetForAnnotation || 'global'}
                      onValueChange={(value) => {
                        const newValue = value === 'global' ? '' : value;
                        setSelectedDatasetForAnnotation(newValue);
                        loadEntityTypes(newValue);
                      }}
                    >
                      <SelectTrigger id="entity-dataset-select">
                        <SelectValue placeholder="Chọn dataset..." />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="global">
                          <div className="flex items-center gap-2">
                            <Globe className="h-4 w-4" />
                            <span>Global (Tất cả datasets)</span>
                          </div>
                        </SelectItem>
                        <SelectSeparator />
                        {trainingData.map((dataset) => (
                          <SelectItem key={dataset.template_id} value={dataset.template_id}>
                            <div className="flex items-center gap-2">
                              <FileText className="h-4 w-4" />
                              <span>{dataset.template_id}</span>
                              <Badge variant="secondary" className="ml-2 text-xs">
                                {dataset.num_samples || 0} samples
                              </Badge>
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="pt-6">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => loadEntityTypes(selectedDatasetForAnnotation)}
                    >
                      <RefreshCw className="h-4 w-4 mr-2" />
                      Tải lại
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            <EntityTypeManager
              templateId={selectedDatasetForAnnotation}
              onEntityTypesChange={(types) => {
                setEntityTypes(types);
                const typesMap = {};
                types.forEach(type => {
                  typesMap[type.entity_code] = type;
                });
                setEntityTypesMap(typesMap);
                setTrainingConfig(prev => ({
                  ...prev,
                  entity_types: types.map(t => t.entity_code)
                }));
              }}
            />
          </TabsContent>

          {/* Training Tab */}
          <TabsContent value="training" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Training Config */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Settings className="h-5 w-5" />
                    Cấu hình huấn luyện
                  </CardTitle>
                  <CardDescription>
                    Thiết lập tham số cho việc huấn luyện mô hình NER
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="epochs">Số epochs</Label>
                      <Input
                        id="epochs"
                        type="number"
                        value={trainingConfig.epochs}
                        onChange={(e) => setTrainingConfig(prev => ({
                          ...prev,
                          epochs: parseInt(e.target.value) || 50
                        }))}
                      />
                    </div>
                    <div>
                      <Label htmlFor="batch-size">Batch size</Label>
                      <Input
                        id="batch-size"
                        type="number"
                        value={trainingConfig.batch_size}
                        onChange={(e) => setTrainingConfig(prev => ({
                          ...prev,
                          batch_size: parseInt(e.target.value) || 16
                        }))}
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="learning-rate">Learning rate</Label>
                    <Input
                      id="learning-rate"
                      type="number"
                      step="0.0001"
                      value={trainingConfig.learning_rate}
                      onChange={(e) => setTrainingConfig(prev => ({
                        ...prev,
                        learning_rate: parseFloat(e.target.value) || 0.001
                      }))}
                    />
                  </div>

                  <div>
                    <Label htmlFor="model-type">Loại mô hình</Label>
                    <Select 
                      value={trainingConfig.model_type} 
                      onValueChange={(value) => setTrainingConfig(prev => ({ ...prev, model_type: value }))}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="bert">BERT (Phobert)</SelectItem>
                        <SelectItem value="bilstm">BiLSTM-CRF</SelectItem>
                        <SelectItem value="transformer">Transformer</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div>
                    <Label>Loại thực thể cần train</Label>
                    <div className="grid grid-cols-2 gap-2 mt-2 max-h-32 overflow-y-auto">
                      {getAvailableEntityTypes().map(({code, label}) => (
                        <div key={code} className="flex items-center space-x-2">
                          <input
                            type="checkbox"
                            id={code}
                            checked={trainingConfig.entity_types.includes(code)}
                            onChange={(e) => {
                              const newTypes = e.target.checked
                                ? [...trainingConfig.entity_types, code]
                                : trainingConfig.entity_types.filter(t => t !== code);
                              setTrainingConfig(prev => ({ ...prev, entity_types: newTypes }));
                            }}
                            className="rounded"
                          />
                          <Label htmlFor={code} className="text-sm">{label}</Label>
                        </div>
                      ))}
                    </div>
                  </div>

                  <Button 
                    onClick={startTraining} 
                    disabled={loading || !canTrain}
                    className="w-full"
                    size="lg"
                  >
                    <Zap className="h-4 w-4 mr-2" />
                    {loading ? 'Đang khởi tạo...' : 'Bắt đầu huấn luyện'}
                  </Button>

                  <div className="text-sm text-muted-foreground text-center">
                    {totalApprovedSamples < 50 ? (
                      <span className="text-red-600">
                        ⚠️ Cần ít nhất 50 mẫu đã duyệt. Hiện có: {totalApprovedSamples} mẫu
                      </span>
                    ) : (
                      <span className="text-green-600">
                        ✓ Sẵn sàng huấn luyện với {totalApprovedSamples} mẫu đã duyệt
                      </span>
                    )}
                  </div>
                </CardContent>
              </Card>

              {/* Training Info */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Activity className="h-5 w-5" />
                    Thông tin huấn luyện
                  </CardTitle>
                  <CardDescription>
                    Hướng dẫn và lưu ý quan trọng
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-3">
                    <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                      <h4 className="font-medium text-blue-900 mb-1">Dữ liệu yêu cầu</h4>
                      <p className="text-sm text-blue-700">
                        Cần ít nhất 50 mẫu đã được duyệt để bắt đầu huấn luyện. 
                        Mẫu phải được đánh dấu "approved" trong quá trình gán nhãn.
                      </p>
                    </div>

                    <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
                      <h4 className="font-medium text-green-900 mb-1">Thời gian train</h4>
                      <p className="text-sm text-green-700">
                        Với 50 epochs và 1000 mẫu, thời gian train khoảng 2-3 giờ trên GPU.
                      </p>
                    </div>

                    <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                      <h4 className="font-medium text-yellow-900 mb-1">Kết quả mong đợi</h4>
                      <p className="text-sm text-yellow-700">
                        F1-score &gt; 0.85 cho các thực thể phổ biến như tên người bệnh, chẩn đoán.
                      </p>
                    </div>

                    <div className="p-3 bg-purple-50 border border-purple-200 rounded-lg">
                      <h4 className="font-medium text-purple-900 mb-1">Mô hình BERT</h4>
                      <p className="text-sm text-purple-700">
                        Sử dụng PhoBERT pre-trained cho tiếng Việt, phù hợp với tài liệu y tế.
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* Evaluation Tab */}
          <TabsContent value="evaluation" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Model Selection & Prediction */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Target className="h-5 w-5" />
                    Dự đoán NER
                  </CardTitle>
                  <CardDescription>
                    Sử dụng mô hình đã train để nhận diện thực thể
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label>Chọn mô hình</Label>
                    <Select value={selectedModel} onValueChange={setSelectedModel}>
                      <SelectTrigger>
                        <SelectValue placeholder="Chọn mô hình..." />
                      </SelectTrigger>
                      <SelectContent>
                        {trainedModels.map((model) => (
                          <SelectItem key={model.training_id} value={model.model_path}>
                            {model.training_id} ({new Date(model.created_at).toLocaleDateString()})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div>
                    <Label>Văn bản cần phân tích</Label>
                    <Textarea
                      value={predictionText}
                      onChange={(e) => setPredictionText(e.target.value)}
                      rows={6}
                      placeholder="Nhập văn bản y tế để nhận diện thực thể..."
                    />
                  </div>

                  <Button 
                    onClick={predictEntities} 
                    disabled={!selectedModel || !predictionText.trim() || loading}
                    className="w-full"
                  >
                    <Target className="h-4 w-4 mr-2" />
                    {loading ? 'Đang phân tích...' : 'Phân tích thực thể'}
                  </Button>
                </CardContent>
              </Card>

              {/* Prediction Results */}
              <Card>
                <CardHeader>
                  <CardTitle>Kết quả nhận diện</CardTitle>
                  <CardDescription>
                    Các thực thể được tìm thấy trong văn bản
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  {predictionResults ? (
                    <div className="space-y-4">
                      <div className="p-4 bg-gray-50 rounded-lg">
                        <div className="text-sm leading-relaxed whitespace-pre-wrap">
                          {renderPredictedText(predictionResults.text, predictionResults.entities)}
                        </div>
                      </div>

                      <div className="space-y-2">
                        <h4 className="font-semibold">Thực thể tìm thấy ({predictionResults.entities?.length || 0})</h4>
                        <div className="space-y-1">
                          {predictionResults.entities?.map((entity, idx) => {
                            const entityConfig = getEntityTypeConfig(entity.label);
                            const entityType = entityTypesMap[entity.label];
                            return (
                              <div key={idx} className="flex items-center justify-between p-2 bg-white border rounded">
                                <div className="flex items-center gap-2">
                                  {entityType?.icon && (
                                    <span className="text-base">{entityType.icon}</span>
                                  )}
                                  <Badge variant="outline" className={cn("text-xs", entityConfig.color)}>
                                    {entityConfig.label}
                                  </Badge>
                                  <span className="text-sm font-medium">"{entity.text}"</span>
                                </div>
                                <span className="text-xs text-gray-500">
                                  {entity.start}-{entity.end}
                                </span>
                              </div>
                            );
                          }) || (
                            <p className="text-sm text-gray-500">Không tìm thấy thực thể nào</p>
                          )}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="text-center py-8 text-muted-foreground">
                      <Target className="h-12 w-12 mx-auto mb-4 opacity-50" />
                      <p>Chọn mô hình và nhập văn bản để bắt đầu phân tích</p>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

            {/* Model Management */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Brain className="h-5 w-5" />
                  Quản lý mô hình
                </CardTitle>
                <CardDescription>
                  Danh sách các mô hình NER đã được huấn luyện
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {trainedModels.length > 0 ? (
                    <div className="grid gap-4">
                      {trainedModels.map((model) => (
                        <Card key={model.training_id} className="p-4">
                          <div className="flex items-center justify-between">
                            <div className="space-y-1">
                              <h4 className="font-semibold">{model.training_id}</h4>
                              <p className="text-sm text-muted-foreground">
                                Tạo: {new Date(model.created_at).toLocaleString()} • 
                                Train: {model.train_samples} mẫu • 
                                Dev: {model.dev_samples} mẫu
                              </p>
                              <div className="flex items-center gap-4 text-sm">
                                <span>Status: <Badge variant={model.status === 'completed' ? 'default' : 'secondary'}>{model.status}</Badge></span>
                                {model.config && (
                                  <span>Epochs: {model.config.epochs}</span>
                                )}
                              </div>
                            </div>
                            <div className="flex gap-2">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setSelectedModel(model.model_path)}
                                disabled={model.status !== 'completed'}
                              >
                                <Target className="h-4 w-4 mr-2" />
                                Sử dụng
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => evaluateModel(model)}
                                disabled={model.status !== 'completed'}
                              >
                                <BarChart3 className="h-4 w-4 mr-2" />
                                Đánh giá
                              </Button>
                            </div>
                          </div>
                        </Card>
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-8 text-muted-foreground">
                      <Brain className="h-12 w-12 mx-auto mb-4 opacity-50" />
                      <p>Chưa có mô hình nào được huấn luyện</p>
                      <p className="text-sm">Sử dụng tab "Huấn luyện" để tạo mô hình mới</p>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        {/* Edit Dataset Dialog */}
        <Dialog open={!!editingDataset} onOpenChange={() => setEditingDataset(null)}>
          <DialogContent className="max-w-4xl max-h-[80vh] overflow-auto">
            <DialogHeader>
              <DialogTitle>Chỉnh sửa tập dữ liệu: {editingDataset?.template_id}</DialogTitle>
              <DialogDescription>
                Chỉnh sửa JSON và kiểm tra cú pháp
              </DialogDescription>
            </DialogHeader>

            {editingDataset && (
              <div className="space-y-4">
                <div>
                  <Label>Dataset JSON</Label>
                  <Textarea
                    value={JSON.stringify(editingDataset, null, 2)}
                    onChange={(e) => {
                      try {
                        const parsed = JSON.parse(e.target.value);
                        setEditingDataset(parsed);
                        setValidationErrors([]);
                      } catch (err) {
                        setValidationErrors(['Cú pháp JSON không hợp lệ']);
                      }
                    }}
                    rows={20}
                    className="font-mono text-sm"
                  />
                </div>

                {validationErrors.length > 0 && (
                  <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                    <h4 className="font-medium text-red-800 mb-2">Lỗi xác thực:</h4>
                    <ul className="list-disc list-inside text-sm text-red-700">
                      {validationErrors.map((error, idx) => (
                        <li key={idx}>{error}</li>
                      ))}
                    </ul>
                  </div>
                )}

                <div className="flex justify-end gap-2">
                  <Button variant="outline" onClick={() => setEditingDataset(null)}>
                    <X className="h-4 w-4 mr-2" />
                    Hủy
                  </Button>
                  <Button onClick={() => saveDataset(editingDataset)} disabled={loading || validationErrors.length > 0}>
                    <Save className="h-4 w-4 mr-2" />
                    {loading ? 'Đang lưu...' : 'Lưu thay đổi'}
                  </Button>
                </div>
              </div>
            )}
          </DialogContent>
        </Dialog>

        {/* Results Display */}
        {results && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CheckCircle className="h-5 w-5 text-green-600" />
                Hoàn thành
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p>{results.message}</p>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
};

export default NERTrainingPage;