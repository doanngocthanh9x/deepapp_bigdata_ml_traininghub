import { DashboardLayout } from '@/components/DashboardLayout';
import { FileSearch, Cpu, Database, Settings, Plus, Edit, Trash2, Search, Eye, CheckCircle, AlertCircle, Info, FileText, Calendar, User, Stethoscope } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Separator } from '@/components/ui/separator';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { cn } from '@/lib/utils';
import { useState, useEffect } from 'react';

const RAGOcrPage = () => {
  const [activeTab, setActiveTab] = useState('process');
  const [templates, setTemplates] = useState([]);
  const [selectedTemplate, setSelectedTemplate] = useState('auto-detect');
  const [query, setQuery] = useState('');
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [filePreview, setFilePreview] = useState(null);
  const [processingResults, setProcessingResults] = useState([]);

  // Template configuration state
  const [templateConfig, setTemplateConfig] = useState({
    template_id: '',
    template_name: '',
    description: '',
    classifier_keywords: [],
    rules: []
  });

  // Rule editing state
  const [showRuleEditor, setShowRuleEditor] = useState(false);
  const [editingRuleIndex, setEditingRuleIndex] = useState(null);
  const [currentRule, setCurrentRule] = useState({
    field_name: '',
    display_name: '',
    keywords: [],
    strategy: 'keyword_value',
    patterns: [],
    priority: 5,
    query_aliases: []
  });

  // Template details view
  const [selectedTemplateDetails, setSelectedTemplateDetails] = useState(null);

  useEffect(() => {
    loadTemplates();
  }, []);

  const loadTemplates = async () => {
    try {
      const response = await fetch('/AA/A0/AAA0_0202/templates');
      const data = await response.json();
      setTemplates(data.templates || []);
      
      // Reset selectedTemplate if it's not valid anymore
      const validValues = ['auto-detect', ...(data.templates || []).map(t => t.id).filter(id => id && id.trim() !== '')];
      if (!validValues.includes(selectedTemplate) || selectedTemplate === 'no-templates') {
        const availableTemplates = data.templates || [];
        setSelectedTemplate(availableTemplates.length > 0 ? availableTemplates[0].id : 'auto-detect');
      }
    } catch (error) {
      console.error('Error loading templates:', error);
      setTemplates([]);
      setSelectedTemplate('auto-detect');
    }
  };

  const handleFileUpload = async (event) => {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) return;

    // Set uploaded files
    setUploadedFiles(files);
    
    // Set preview for first file
    if (files[0].type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => setFilePreview(e.target.result);
      reader.readAsDataURL(files[0]);
    } else {
      setFilePreview(null);
    }

    setLoading(true);
    setProcessingResults([]);
    
    try {
      // Process each file sequentially
      const results = [];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        console.log(`Processing file ${i + 1}/${files.length}: ${file.name}`);
        
        const formData = new FormData();
        formData.append('file', file);
        formData.append('template_id', selectedTemplate === 'auto-detect' ? '' : selectedTemplate);
        formData.append('query', query);

        const response = await fetch('/AA/A0/AAA0_0202/process', {
          method: 'POST',
          body: formData
        });

        const data = await response.json();
        results.push({ file: file.name, data });
        setProcessingResults([...results]); // Update UI after each file
      }
      
      // Set final results
      if (results.length === 1) {
        setResults(results[0].data);
      } else {
        setResults({ 
          message: `Processed ${results.length} files successfully`,
          results: results 
        });
      }
    } catch (error) {
      console.error('Error processing document:', error);
      setResults({
        error: 'Failed to process document',
        message: error.message
      });
    } finally {
      setLoading(false);
    }
  };

  const handleQuery = async () => {
    if (!query.trim()) return;

    setLoading(true);
    try {
      const response = await fetch('/AA/A0/AAA0_0202/query', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          query: query,
          template_id: selectedTemplate
        })
      });

      const data = await response.json();
      setResults(data);
    } catch (error) {
      console.error('Error querying document:', error);
    } finally {
      setLoading(false);
    }
  };

  const saveTemplate = async () => {
    try {
      const response = await fetch('/AA/A0/AAA0_0202/templates', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(templateConfig)
      });

      if (response.ok) {
        loadTemplates();
        resetTemplateConfig();
      }
    } catch (error) {
      console.error('Error saving template:', error);
    }
  };

  const deleteTemplate = async (templateId) => {
    try {
      const response = await fetch(`/AA/A0/AAA0_0202/templates/${templateId}`, {
        method: 'DELETE'
      });

      if (response.ok) {
        loadTemplates();
      }
    } catch (error) {
      console.error('Error deleting template:', error);
    }
  };

  // Rule management functions
  const addRule = () => {
    setCurrentRule({
      field_name: '',
      display_name: '',
      keywords: [],
      strategy: 'keyword_value',
      patterns: [],
      priority: 5,
      query_aliases: []
    });
    setEditingRuleIndex(null);
    setShowRuleEditor(true);
  };

  const editRule = (ruleIndex) => {
    const rule = templateConfig.rules[ruleIndex];
    setCurrentRule({
      ...rule,
      keywords: Array.isArray(rule.keywords) ? rule.keywords : [rule.keywords].filter(Boolean),
      patterns: rule.patterns || [],
      query_aliases: Array.isArray(rule.query_aliases) ? rule.query_aliases : [rule.query_aliases].filter(Boolean)
    });
    setEditingRuleIndex(ruleIndex);
    setShowRuleEditor(true);
  };

  const saveRule = () => {
    if (!currentRule.field_name || !currentRule.display_name) return;

    const updatedRules = [...templateConfig.rules];
    if (editingRuleIndex !== null) {
      updatedRules[editingRuleIndex] = currentRule;
    } else {
      updatedRules.push(currentRule);
    }

    setTemplateConfig(prev => ({
      ...prev,
      rules: updatedRules
    }));

    setShowRuleEditor(false);
    setCurrentRule({
      field_name: '',
      display_name: '',
      keywords: [],
      strategy: 'keyword_value',
      patterns: [],
      priority: 5,
      query_aliases: []
    });
  };

  const deleteRule = (ruleIndex) => {
    const updatedRules = templateConfig.rules.filter((_, index) => index !== ruleIndex);
    setTemplateConfig(prev => ({
      ...prev,
      rules: updatedRules
    }));
  };

  const viewTemplateDetails = (template) => {
    setSelectedTemplateDetails(template);
  };

  const editTemplate = (template) => {
    setTemplateConfig({
      template_id: template.id,
      template_name: template.name,
      description: template.description,
      classifier_keywords: template.classifier_keywords || [],
      rules: template.rules || []
    });
  };

  const resetTemplateConfig = () => {
    setTemplateConfig({
      template_id: '',
      template_name: '',
      description: '',
      classifier_keywords: [],
      rules: []
    });
  };

  const features = [
    {
      icon: FileSearch,
      title: 'OCR Processing',
      description: 'Advanced text extraction from documents',
      status: 'active',
    },
    {
      icon: Database,
      title: 'RAG System',
      description: 'Retrieval-Augmented Generation for document Q&A',
      status: 'active',
    },
    {
      icon: Settings,
      title: 'Template Config',
      description: 'Configure document templates and extraction rules',
      status: 'active',
    },
  ];

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        <div className="animate-fade-in">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-primary/10">
                <FileSearch className="w-6 h-6 text-primary" />
              </div>
              <div>
                <span className="text-xs font-mono text-muted-foreground">AAA0_0202</span>
                <h1 className="text-2xl font-bold text-foreground">OCR with RAG System</h1>
                <span className="inline-block mt-1 text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                  Active
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Badge variant="outline" className="text-xs">
                {templates.length} templates
              </Badge>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setResults(null);
                  setUploadedFiles([]);
                  setFilePreview(null);
                  setProcessingResults([]);
                  setQuery('');
                  setSelectedTemplate('auto-detect');
                }}
              >
                Clear All
              </Button>
            </div>
          </div>
          <p className="text-muted-foreground mt-2">
            Intelligent document processing with OCR, field extraction, and AI-powered Q&A using Retrieval-Augmented Generation
          </p>
        </div>

        {/* Features */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {features.map((feature, index) => (
            <Card key={index} className="border-border/50">
              <CardContent className="p-6">
                <div className="flex items-center gap-3">
                  <div className={cn(
                    "p-2 rounded-lg",
                    feature.status === 'active' ? "bg-primary/10" : "bg-muted"
                  )}>
                    <feature.icon className={cn(
                      "w-5 h-5",
                      feature.status === 'active' ? "text-primary" : "text-muted-foreground"
                    )} />
                  </div>
                  <div>
                    <h3 className="font-semibold text-sm">{feature.title}</h3>
                    <p className="text-xs text-muted-foreground">{feature.description}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Main Content */}
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="process">Process Document</TabsTrigger>
            <TabsTrigger value="query">Query Document</TabsTrigger>
            <TabsTrigger value="templates">Template Config</TabsTrigger>
          </TabsList>

          {/* Process Document Tab */}
          <TabsContent value="process" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Upload Section */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <FileSearch className="w-5 h-5 text-primary" />
                    Document Upload
                  </CardTitle>
                  <CardDescription>
                    Upload a document for OCR processing and information extraction
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-1 gap-4">
                    <div>
                      <Label htmlFor="template-select">Document Template</Label>
                      <Select value={selectedTemplate} onValueChange={setSelectedTemplate}>
                        <SelectTrigger>
                          <SelectValue placeholder="Auto-detect or select template" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="auto-detect">Auto-detect</SelectItem>
                          {templates
                            .filter(template => template.id && template.id.trim() !== '')
                            .map((template) => (
                              <SelectItem key={template.id} value={template.id}>
                                {template.name}
                              </SelectItem>
                            ))}
                        </SelectContent>
                      </Select>
                      <p className="text-xs text-muted-foreground mt-1">
                        Leave empty for automatic document type detection
                      </p>
                    </div>

                    <div>
                      <Label htmlFor="query-input">Query (Optional)</Label>
                      <Input
                        id="query-input"
                        placeholder="Ask a question about the document..."
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                      />
                      <p className="text-xs text-muted-foreground mt-1">
                        Get specific answers from the document using AI
                      </p>
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="file-upload">Upload Document(s)</Label>
                    <div className="mt-2">
                      <Input
                        id="file-upload"
                        type="file"
                        accept="image/*,.pdf,.doc,.docx"
                        multiple
                        onChange={handleFileUpload}
                        disabled={loading}
                        className="file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90"
                      />
                      <p className="text-xs text-muted-foreground mt-1">
                        Supported formats: Images, PDF, Word documents. You can select multiple files.
                      </p>
                    </div>
                  </div>

                  {loading && (
                    <div className="flex items-center gap-3 p-4 bg-primary/5 border border-primary/20 rounded-lg">
                      <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-primary"></div>
                      <div>
                        <p className="text-sm font-medium text-primary">
                          Processing {uploadedFiles.length} file(s)... ({processingResults.length}/{uploadedFiles.length})
                        </p>
                        <p className="text-xs text-muted-foreground">OCR → Field Extraction → RAG Analysis</p>
                      </div>
                    </div>
                  )}

                  {uploadedFiles.length > 0 && !loading && (
                    <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
                      <div className="flex items-center gap-2 text-green-800">
                        <CheckCircle className="w-4 h-4" />
                        <span className="text-sm font-medium">
                          {uploadedFiles.length === 1 
                            ? `File uploaded: ${uploadedFiles[0].name}`
                            : `${uploadedFiles.length} files uploaded`}
                        </span>
                      </div>
                      {uploadedFiles.length > 1 && (
                        <div className="mt-2 space-y-1">
                          {uploadedFiles.map((file, idx) => (
                            <div key={idx} className="text-xs text-green-700">
                              {idx + 1}. {file.name} ({(file.size / 1024 / 1024).toFixed(2)} MB)
                            </div>
                          ))}
                        </div>
                      )}
                      {uploadedFiles.length === 1 && (
                        <p className="text-xs text-green-600 mt-1">
                          {(uploadedFiles[0].size / 1024 / 1024).toFixed(2)} MB • Ready for processing
                        </p>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Document Preview */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Eye className="w-5 h-5 text-primary" />
                    Document Preview
                  </CardTitle>
                  <CardDescription>
                    Preview of the uploaded document
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  {filePreview ? (
                    <div className="space-y-4">
                      <div className="relative bg-gray-50 rounded-lg overflow-hidden">
                        <img
                          src={filePreview}
                          alt="Document preview"
                          className="w-full h-auto max-h-64 object-contain"
                        />
                      </div>
                      <div className="flex items-center justify-between text-sm text-muted-foreground">
                        <span>Document preview (First file)</span>
                        <Badge variant="outline">Image</Badge>
                      </div>
                    </div>
                  ) : uploadedFiles.length > 0 ? (
                    <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
                      <FileText className="w-12 h-12 mb-4 opacity-50" />
                      <p className="text-sm">{uploadedFiles[0].name}</p>
                      <p className="text-xs mt-1">Preview not available for this file type</p>
                      {uploadedFiles.length > 1 && (
                        <p className="text-xs mt-2">+ {uploadedFiles.length - 1} more file(s)</p>
                      )}
                    </div>
                  ) : (
                    <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
                      <Eye className="w-12 h-12 mb-4 opacity-50" />
                      <p className="text-sm">No document uploaded</p>
                      <p className="text-xs mt-1">Upload document(s) to see preview</p>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

            {/* Results */}
            {results && (
              <div className="space-y-6">
                {/* Document Overview */}
                <Card className="border-primary/20 bg-primary/5">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="p-2 rounded-lg bg-primary/10">
                          <FileText className="w-5 h-5 text-primary" />
                        </div>
                        <div>
                          <CardTitle className="text-lg">Document Processed Successfully</CardTitle>
                          <CardDescription>
                            {results.document_type || 'Document'} • {results.stats?.total_text_regions || 0} text regions detected
                          </CardDescription>
                        </div>
                      </div>
                      <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">
                        <CheckCircle className="w-3 h-3 mr-1" />
                        Complete
                      </Badge>
                    </div>
                  </CardHeader>
                </Card>

                {/* Extracted Fields - Enhanced Display */}
                {results.extracted_fields && Object.keys(results.extracted_fields).length > 0 && (
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2">
                        <Database className="w-5 h-5 text-primary" />
                        Extracted Information
                      </CardTitle>
                      <CardDescription>
                        Structured data extracted from the document
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {Object.entries(results.extracted_fields).map(([key, value]) => {
                          const getFieldIcon = (fieldName) => {
                            if (fieldName.includes('tên') || fieldName.includes('name')) return User;
                            if (fieldName.includes('tuổi') || fieldName.includes('age')) return Calendar;
                            if (fieldName.includes('chẩn_đoán') || fieldName.includes('diagnosis')) return Stethoscope;
                            if (fieldName.includes('ngày') || fieldName.includes('date')) return Calendar;
                            return Info;
                          };

                          const IconComponent = getFieldIcon(key.toLowerCase());
                          const hasValue = value && String(value).trim() !== '';

                          return (
                            <div key={key} className={cn(
                              "p-4 rounded-lg border transition-all duration-200",
                              hasValue
                                ? "bg-green-50 border-green-200 shadow-sm"
                                : "bg-gray-50 border-gray-200"
                            )}>
                              <div className="flex items-start gap-3">
                                <div className={cn(
                                  "p-2 rounded-lg",
                                  hasValue ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"
                                )}>
                                  <IconComponent className="w-4 h-4" />
                                </div>
                                <div className="flex-1 min-w-0">
                                  <div className="flex items-center gap-2 mb-1">
                                    <span className="text-sm font-medium text-gray-700 capitalize">
                                      {key.replace(/_/g, ' ')}
                                    </span>
                                    {hasValue && (
                                      <Badge variant="outline" className="text-xs bg-green-50 text-green-700 border-green-200">
                                        Extracted
                                      </Badge>
                                    )}
                                  </div>
                                  <div className="text-sm text-gray-900 font-medium break-words">
                                    {hasValue ? String(value) : 'Not found'}
                                  </div>
                                </div>
                              </div>
                            </div>
                          );
                        })}
                      </div>

                      {/* Extraction Stats */}
                      <Separator className="my-4" />
                      <div className="flex items-center justify-between text-sm text-muted-foreground">
                        <span>
                          {results.stats?.fields_extracted || 0} of {results.stats?.total_fields || 0} fields extracted
                        </span>
                        <div className="flex items-center gap-2">
                          <span>Extraction Rate:</span>
                          <Progress
                            value={results.stats?.total_fields ?
                              (results.stats.fields_extracted / results.stats.total_fields) * 100 : 0}
                            className="w-20 h-2"
                          />
                          <span className="text-xs">
                            {results.stats?.total_fields ?
                              Math.round((results.stats.fields_extracted / results.stats.total_fields) * 100) : 0}%
                          </span>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                )}

                {/* Query Response - Enhanced */}
                {results.query_response && (
                  <Card className="border-blue-200 bg-blue-50/50">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-blue-900">
                        <Search className="w-5 h-5" />
                        Query Response
                      </CardTitle>
                      <CardDescription className="text-blue-700">
                        Answer generated using hybrid search (Template + RAG)
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {/* Answer */}
                      <div className="p-4 bg-white rounded-lg border border-blue-200">
                        <div className="flex items-start gap-3">
                          <div className="p-2 bg-blue-100 rounded-lg">
                            <Info className="w-4 h-4 text-blue-700" />
                          </div>
                          <div className="flex-1">
                            <p className="text-gray-900 font-medium mb-2">
                              {results.query_response.answer}
                            </p>
                            <div className="flex items-center gap-2">
                              <Badge variant="outline" className={cn(
                                "text-xs",
                                results.query_response.confidence === 'HIGH' && "bg-green-50 text-green-700 border-green-200",
                                results.query_response.confidence === 'MEDIUM' && "bg-yellow-50 text-yellow-700 border-yellow-200",
                                results.query_response.confidence === 'LOW' && "bg-orange-50 text-orange-700 border-orange-200",
                                results.query_response.confidence === 'NONE' && "bg-red-50 text-red-700 border-red-200"
                              )}>
                                {results.query_response.confidence} Confidence
                              </Badge>
                              <span className="text-xs text-muted-foreground">
                                Source: {results.query_response.source}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* RAG Results */}
                      {results.query_response.rag_results && results.query_response.rag_results.length > 0 && (
                        <div>
                          <h4 className="font-semibold text-sm text-gray-700 mb-3 flex items-center gap-2">
                            <Database className="w-4 h-4" />
                            Supporting Evidence ({results.query_response.rag_results.length} results)
                          </h4>
                          <div className="space-y-2">
                            {results.query_response.rag_results.map((result, idx) => (
                              <div key={idx} className="p-3 bg-white rounded-lg border border-gray-200">
                                <div className="flex items-start justify-between mb-2">
                                  <span className="text-xs font-medium text-gray-500">
                                    Result {idx + 1}
                                  </span>
                                  <Badge variant="secondary" className="text-xs">
                                    Score: {result.score?.toFixed(3)}
                                  </Badge>
                                </div>
                                <p className="text-sm text-gray-700">{result.text}</p>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                )}

                {/* OCR Text - Collapsible */}
                {results.ocr_text && results.ocr_text.length > 0 && (
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2">
                        <Eye className="w-5 h-5 text-primary" />
                        Raw OCR Text
                      </CardTitle>
                      <CardDescription>
                        Original text extracted from the document ({results.ocr_text.length} text regions)
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="bg-gray-50 p-4 rounded-lg max-h-64 overflow-y-auto">
                        <div className="space-y-2">
                          {results.ocr_text.map((text, idx) => (
                            <div key={idx} className="flex items-start gap-2">
                              <span className="text-xs text-gray-500 font-mono min-w-[2rem]">
                                {idx + 1}.
                              </span>
                              <span className="text-sm text-gray-800 break-words">
                                {text}
                              </span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                )}
              </div>
            )}
          </TabsContent>

          {/* Query Document Tab */}
          <TabsContent value="query" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Search className="w-5 h-5 text-primary" />
                  Document Query
                </CardTitle>
                <CardDescription>
                  Ask questions about processed documents using AI-powered search
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <Label htmlFor="query-template" className="flex items-center gap-2">
                      <Settings className="w-4 h-4" />
                      Document Template
                    </Label>
                    <Select value={selectedTemplate} onValueChange={setSelectedTemplate} disabled={templates.length === 0}>
                      <SelectTrigger>
                        <SelectValue placeholder={templates.length === 0 ? "No templates available" : "Select template"} />
                      </SelectTrigger>
                      <SelectContent>
                        {templates.length === 0 ? (
                          <SelectItem value="no-templates" disabled>
                            No templates available
                          </SelectItem>
                        ) : (
                          templates
                            .filter(template => template.id && template.id.trim() !== '')
                            .map((template) => (
                              <SelectItem key={template.id} value={template.id}>
                                {template.name}
                              </SelectItem>
                            ))
                        )}
                      </SelectContent>
                    </Select>
                    <p className="text-xs text-muted-foreground">
                      Choose the document type to query
                    </p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="query-text" className="flex items-center gap-2">
                      <Search className="w-4 h-4" />
                      Your Question
                    </Label>
                    <Input
                      id="query-text"
                      placeholder="What information do you need?"
                      value={query}
                      onChange={(e) => setQuery(e.target.value)}
                      className="text-base"
                    />
                    <p className="text-xs text-muted-foreground">
                      Ask in natural language about the document content
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <Button
                    onClick={handleQuery}
                    disabled={loading || !query.trim() || !selectedTemplate}
                    size="lg"
                    className="flex-1"
                  >
                    <Search className="w-4 h-4 mr-2" />
                    Search Document
                  </Button>

                  {query && selectedTemplate && (
                    <Badge variant="outline" className="px-3 py-1">
                      Ready to query
                    </Badge>
                  )}
                </div>

                {loading && (
                  <div className="flex items-center gap-3 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
                    <div>
                      <p className="text-sm font-medium text-blue-900">Searching...</p>
                      <p className="text-xs text-blue-700">Analyzing document with AI</p>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Query Examples */}
            <Card className="border-dashed">
              <CardHeader>
                <CardTitle className="text-base">Query Examples</CardTitle>
                <CardDescription>
                  Try these example questions for different document types
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div className="space-y-2">
                    <h4 className="text-sm font-medium text-muted-foreground">For Discharge Documents:</h4>
                    <div className="space-y-1">
                      {[
                        "Tên bệnh nhân là gì?",
                        "Bệnh nhân mắc bệnh gì?",
                        "Ngày vào viện khi nào?",
                        "Ngày ra viện khi nào?"
                      ].map((example, idx) => (
                        <Button
                          key={idx}
                          variant="ghost"
                          size="sm"
                          className="h-auto p-2 text-left justify-start text-xs"
                          onClick={() => setQuery(example)}
                        >
                          "{example}"
                        </Button>
                      ))}
                    </div>
                  </div>

                  <div className="space-y-2">
                    <h4 className="text-sm font-medium text-muted-foreground">For Prescriptions:</h4>
                    <div className="space-y-1">
                      {[
                        "Thuốc gì được kê đơn?",
                        "Liều lượng như thế nào?",
                        "Cách dùng thuốc ra sao?"
                      ].map((example, idx) => (
                        <Button
                          key={idx}
                          variant="ghost"
                          size="sm"
                          className="h-auto p-2 text-left justify-start text-xs"
                          onClick={() => setQuery(example)}
                        >
                          "{example}"
                        </Button>
                      ))}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Template Configuration Tab */}
          <TabsContent value="templates" className="space-y-6">
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
              {/* Template List */}
              <div className="xl:col-span-2">
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Settings className="w-5 h-5 text-primary" />
                      Document Templates
                    </CardTitle>
                    <CardDescription>
                      Manage templates for different document types and their extraction rules
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    {templates.length === 0 ? (
                      <div className="text-center py-8 text-muted-foreground">
                        <Settings className="w-12 h-12 mx-auto mb-4 opacity-50" />
                        <p>No templates found</p>
                        <p className="text-sm">Create your first template to get started</p>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {templates.map((template) => (
                          <Card key={template.id} className="border-border/50 hover:border-primary/30 transition-colors">
                            <CardContent className="p-4">
                              <div className="flex items-start justify-between">
                                <div className="flex-1">
                                  <div className="flex items-center gap-3 mb-2">
                                    <div className="p-2 bg-primary/10 rounded-lg">
                                      <FileText className="w-4 h-4 text-primary" />
                                    </div>
                                    <div>
                                      <h4 className="font-semibold text-gray-900">{template.name}</h4>
                                      <p className="text-sm text-muted-foreground">{template.description}</p>
                                    </div>
                                  </div>

                                  <div className="flex items-center gap-4 text-sm text-muted-foreground">
                                    <div className="flex items-center gap-1">
                                      <Database className="w-3 h-3" />
                                      <span>{template.rules_count} rules</span>
                                    </div>
                                    <div className="flex items-center gap-1">
                                      <Settings className="w-3 h-3" />
                                      <span>ID: {template.id}</span>
                                    </div>
                                    {template.classifier_keywords && (
                                      <div className="flex items-center gap-1">
                                        <Search className="w-3 h-3" />
                                        <span>{template.classifier_keywords.length} keywords</span>
                                      </div>
                                    )}
                                  </div>

                                  {template.classifier_keywords && template.classifier_keywords.length > 0 && (
                                    <div className="mt-3">
                                      <p className="text-xs text-muted-foreground mb-2">Classifier Keywords:</p>
                                      <div className="flex flex-wrap gap-1">
                                        {template.classifier_keywords.slice(0, 5).map((keyword, idx) => (
                                          <Badge key={idx} variant="outline" className="text-xs">
                                            {keyword}
                                          </Badge>
                                        ))}
                                        {template.classifier_keywords.length > 5 && (
                                          <Badge variant="outline" className="text-xs">
                                            +{template.classifier_keywords.length - 5} more
                                          </Badge>
                                        )}
                                      </div>
                                    </div>
                                  )}

                                  {/* Show sample rules */}
                                  {template.rules && template.rules.length > 0 && (
                                    <div className="mt-3">
                                      <p className="text-xs text-muted-foreground mb-2">Sample Fields:</p>
                                      <div className="flex flex-wrap gap-1">
                                        {template.rules.slice(0, 3).map((rule, idx) => (
                                          <Badge key={idx} variant="secondary" className="text-xs">
                                            {rule.display_name || rule.field_name}
                                          </Badge>
                                        ))}
                                        {template.rules.length > 3 && (
                                          <Badge variant="secondary" className="text-xs">
                                            +{template.rules.length - 3} more
                                          </Badge>
                                        )}
                                      </div>
                                    </div>
                                  )}
                                </div>

                                <div className="flex gap-2 ml-4">
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="h-8 w-8 p-0"
                                    onClick={() => viewTemplateDetails(template)}
                                  >
                                    <Eye className="w-3 h-3" />
                                  </Button>
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="h-8 w-8 p-0"
                                    onClick={() => editTemplate(template)}
                                  >
                                    <Edit className="w-3 h-3" />
                                  </Button>
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                                    onClick={() => deleteTemplate(template.id)}
                                  >
                                    <Trash2 className="w-3 h-3" />
                                  </Button>
                                </div>
                              </div>
                            </CardContent>
                          </Card>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>

              {/* Template Editor */}
              <div>
                <Card className="sticky top-6">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Plus className="w-5 h-5 text-primary" />
                      Template Editor
                    </CardTitle>
                    <CardDescription>
                      Create or edit document templates with extraction rules
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <Label htmlFor="template-id">Template ID</Label>
                      <Input
                        id="template-id"
                        placeholder="unique_template_id"
                        value={templateConfig.template_id}
                        onChange={(e) => setTemplateConfig(prev => ({
                          ...prev,
                          template_id: e.target.value
                        }))}
                      />
                      <p className="text-xs text-muted-foreground mt-1">
                        Unique identifier for the template
                      </p>
                    </div>

                    <div>
                      <Label htmlFor="template-name">Template Name</Label>
                      <Input
                        id="template-name"
                        placeholder="Template Display Name"
                        value={templateConfig.template_name}
                        onChange={(e) => setTemplateConfig(prev => ({
                          ...prev,
                          template_name: e.target.value
                        }))}
                      />
                      <p className="text-xs text-muted-foreground mt-1">
                        Human-readable name for the template
                      </p>
                    </div>

                    <div>
                      <Label htmlFor="template-desc">Description</Label>
                      <Textarea
                        id="template-desc"
                        placeholder="Describe what type of documents this template handles..."
                        value={templateConfig.description}
                        onChange={(e) => setTemplateConfig(prev => ({
                          ...prev,
                          description: e.target.value
                        }))}
                        rows={3}
                      />
                    </div>

                    <div>
                      <Label htmlFor="classifier-keywords">Classifier Keywords</Label>
                      <Input
                        id="classifier-keywords"
                        placeholder="ra viện, tóm tắt bệnh án, xuất viện"
                        value={templateConfig.classifier_keywords?.join(', ') || ''}
                        onChange={(e) => setTemplateConfig(prev => ({
                          ...prev,
                          classifier_keywords: e.target.value.split(',').map(k => k.trim()).filter(k => k)
                        }))}
                      />
                      <p className="text-xs text-muted-foreground mt-1">
                        Comma-separated keywords to auto-detect this document type
                      </p>
                    </div>

                    <Separator />

                    <div>
                      <div className="flex items-center justify-between mb-3">
                        <Label>Extraction Rules ({templateConfig.rules.length})</Label>
                        <Button size="sm" variant="outline" onClick={addRule}>
                          <Plus className="w-3 h-3 mr-1" />
                          Add Rule
                        </Button>
                      </div>

                      {templateConfig.rules.length === 0 ? (
                        <div className="p-4 bg-muted/50 rounded-lg text-center">
                          <Database className="w-8 h-8 mx-auto mb-2 opacity-50" />
                          <p className="text-sm text-muted-foreground">
                            No extraction rules defined yet
                          </p>
                          <p className="text-xs text-muted-foreground mt-1">
                            Add rules to extract specific fields from documents
                          </p>
                        </div>
                      ) : (
                        <div className="space-y-2 max-h-48 overflow-y-auto">
                          {templateConfig.rules.map((rule, index) => (
                            <div key={index} className="flex items-center justify-between p-3 bg-muted/30 rounded-lg">
                              <div className="flex-1">
                                <div className="flex items-center gap-2">
                                  <span className="font-medium text-sm">{rule.display_name}</span>
                                  <Badge variant="outline" className="text-xs">
                                    {rule.strategy}
                                  </Badge>
                                  <Badge variant="secondary" className="text-xs">
                                    Priority: {rule.priority}
                                  </Badge>
                                </div>
                                <div className="flex items-center gap-2 mt-1">
                                  <span className="text-xs text-muted-foreground">Keywords:</span>
                                  <div className="flex flex-wrap gap-1">
                                    {rule.keywords.slice(0, 2).map((keyword, idx) => (
                                      <Badge key={idx} variant="outline" className="text-xs">
                                        {keyword}
                                      </Badge>
                                    ))}
                                    {rule.keywords.length > 2 && (
                                      <Badge variant="outline" className="text-xs">
                                        +{rule.keywords.length - 2}
                                      </Badge>
                                    )}
                                  </div>
                                </div>
                              </div>
                              <div className="flex gap-1">
                                <Button size="sm" variant="ghost" className="h-6 w-6 p-0" onClick={() => editRule(index)}>
                                  <Edit className="w-3 h-3" />
                                </Button>
                                <Button size="sm" variant="ghost" className="h-6 w-6 p-0 text-red-600" onClick={() => deleteRule(index)}>
                                  <Trash2 className="w-3 h-3" />
                                </Button>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>

                    <div className="flex gap-2">
                      <Button onClick={saveTemplate} className="flex-1" disabled={!templateConfig.template_id || !templateConfig.template_name}>
                        <Plus className="w-4 h-4 mr-2" />
                        Save Template
                      </Button>
                      <Button variant="outline" onClick={resetTemplateConfig}>
                        Reset
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </TabsContent>
        </Tabs>

        {/* Rule Editor Dialog */}
        <Dialog open={showRuleEditor} onOpenChange={setShowRuleEditor}>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <Settings className="w-5 h-5" />
                {editingRuleIndex !== null ? 'Edit Extraction Rule' : 'Add Extraction Rule'}
              </DialogTitle>
              <DialogDescription>
                Configure how to extract specific fields from documents
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="rule-field-name">Field Name</Label>
                  <Input
                    id="rule-field-name"
                    placeholder="field_name"
                    value={currentRule.field_name}
                    onChange={(e) => setCurrentRule(prev => ({
                      ...prev,
                      field_name: e.target.value
                    }))}
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    Internal field identifier
                  </p>
                </div>

                <div>
                  <Label htmlFor="rule-display-name">Display Name</Label>
                  <Input
                    id="rule-display-name"
                    placeholder="Display Name"
                    value={currentRule.display_name}
                    onChange={(e) => setCurrentRule(prev => ({
                      ...prev,
                      display_name: e.target.value
                    }))}
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    Human-readable field name
                  </p>
                </div>
              </div>

              <div>
                <Label htmlFor="rule-keywords">Keywords</Label>
                <Input
                  id="rule-keywords"
                  placeholder="keyword1, keyword2, keyword3"
                  value={currentRule.keywords.join(', ')}
                  onChange={(e) => setCurrentRule(prev => ({
                    ...prev,
                    keywords: e.target.value.split(',').map(k => k.trim()).filter(k => k)
                  }))}
                />
                <p className="text-xs text-muted-foreground mt-1">
                  Comma-separated keywords to find this field
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="rule-strategy">Extraction Strategy</Label>
                  <Select
                    value={currentRule.strategy}
                    onValueChange={(value) => setCurrentRule(prev => ({
                      ...prev,
                      strategy: value
                    }))}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="keyword_value">Keyword → Value</SelectItem>
                      <SelectItem value="regex">Regex Pattern</SelectItem>
                      <SelectItem value="multi_line">Multi-line Text</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <Label htmlFor="rule-priority">Priority</Label>
                  <Input
                    id="rule-priority"
                    type="number"
                    min="1"
                    max="10"
                    value={currentRule.priority}
                    onChange={(e) => setCurrentRule(prev => ({
                      ...prev,
                      priority: parseInt(e.target.value) || 5
                    }))}
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    Higher priority rules are tried first
                  </p>
                </div>
              </div>

              {currentRule.strategy === 'regex' && (
                <div>
                  <Label htmlFor="rule-patterns">Regex Patterns</Label>
                  <Textarea
                    id="rule-patterns"
                    placeholder="Enter regex patterns, one per line..."
                    value={currentRule.patterns.join('\n')}
                    onChange={(e) => setCurrentRule(prev => ({
                      ...prev,
                      patterns: e.target.value.split('\n').map(p => p.trim()).filter(p => p)
                    }))}
                    rows={3}
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    Regular expressions to match field values
                  </p>
                </div>
              )}

              {currentRule.strategy === 'multi_line' && (
                <div>
                  <Label htmlFor="rule-stop-keywords">Stop Keywords</Label>
                  <Input
                    id="rule-stop-keywords"
                    placeholder="end_keyword1, end_keyword2"
                    value={currentRule.stop_keywords?.join(', ') || ''}
                    onChange={(e) => setCurrentRule(prev => ({
                      ...prev,
                      stop_keywords: e.target.value.split(',').map(k => k.trim()).filter(k => k)
                    }))}
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    Keywords that indicate the end of multi-line text
                  </p>
                </div>
              )}

              <div>
                <Label htmlFor="rule-aliases">Query Aliases</Label>
                <Input
                  id="rule-aliases"
                  placeholder="alias1, alias2, alias3"
                  value={currentRule.query_aliases.join(', ')}
                  onChange={(e) => setCurrentRule(prev => ({
                    ...prev,
                    query_aliases: e.target.value.split(',').map(k => k.trim()).filter(k => k)
                  }))}
                />
                <p className="text-xs text-muted-foreground mt-1">
                  Alternative ways users can query this field
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-4">
              <Button variant="outline" onClick={() => setShowRuleEditor(false)}>
                Cancel
              </Button>
              <Button onClick={saveRule} disabled={!currentRule.field_name || !currentRule.display_name}>
                {editingRuleIndex !== null ? 'Update Rule' : 'Add Rule'}
              </Button>
            </div>
          </DialogContent>
        </Dialog>

        {/* Template Details Dialog */}
        <Dialog open={!!selectedTemplateDetails} onOpenChange={() => setSelectedTemplateDetails(null)}>
          <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <FileText className="w-5 h-5" />
                Template Details: {selectedTemplateDetails?.name}
              </DialogTitle>
              <DialogDescription>
                {selectedTemplateDetails?.description}
              </DialogDescription>
            </DialogHeader>

            {selectedTemplateDetails && (
              <div className="space-y-6">
                {/* Template Info */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-sm font-medium">Template ID</Label>
                    <p className="text-sm text-muted-foreground">{selectedTemplateDetails.id}</p>
                  </div>
                  <div>
                    <Label className="text-sm font-medium">Rules Count</Label>
                    <p className="text-sm text-muted-foreground">{selectedTemplateDetails.rules_count}</p>
                  </div>
                </div>

                {/* Classifier Keywords */}
                {selectedTemplateDetails.classifier_keywords && selectedTemplateDetails.classifier_keywords.length > 0 && (
                  <div>
                    <Label className="text-sm font-medium mb-2 block">Classifier Keywords</Label>
                    <div className="flex flex-wrap gap-2">
                      {selectedTemplateDetails.classifier_keywords.map((keyword, idx) => (
                        <Badge key={idx} variant="outline">
                          {keyword}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}

                {/* Rules Details */}
                <div>
                  <Label className="text-sm font-medium mb-3 block">Extraction Rules</Label>
                  <div className="space-y-3">
                    {selectedTemplateDetails.rules?.map((rule, index) => (
                      <Card key={index} className="border-border/50">
                        <CardContent className="p-4">
                          <div className="flex items-start justify-between mb-3">
                            <div>
                              <h4 className="font-medium">{rule.display_name}</h4>
                              <p className="text-sm text-muted-foreground">Field: {rule.field_name}</p>
                            </div>
                            <div className="flex gap-2">
                              <Badge variant="outline">{rule.strategy}</Badge>
                              <Badge variant="secondary">Priority: {rule.priority}</Badge>
                            </div>
                          </div>

                          <div className="space-y-2">
                            <div>
                              <span className="text-xs font-medium text-muted-foreground">Keywords:</span>
                              <div className="flex flex-wrap gap-1 mt-1">
                                {rule.keywords.map((keyword, idx) => (
                                  <Badge key={idx} variant="outline" className="text-xs">
                                    {keyword}
                                  </Badge>
                                ))}
                              </div>
                            </div>

                            {rule.query_aliases && rule.query_aliases.length > 0 && (
                              <div>
                                <span className="text-xs font-medium text-muted-foreground">Query Aliases:</span>
                                <div className="flex flex-wrap gap-1 mt-1">
                                  {rule.query_aliases.map((alias, idx) => (
                                    <Badge key={idx} variant="secondary" className="text-xs">
                                      {alias}
                                    </Badge>
                                  ))}
                                </div>
                              </div>
                            )}

                            {rule.patterns && rule.patterns.length > 0 && (
                              <div>
                                <span className="text-xs font-medium text-muted-foreground">Regex Patterns:</span>
                                <div className="mt-1 space-y-1">
                                  {rule.patterns.map((pattern, idx) => (
                                    <code key={idx} className="text-xs bg-muted px-2 py-1 rounded">
                                      {pattern}
                                    </code>
                                  ))}
                                </div>
                              </div>
                            )}
                          </div>
                        </CardContent>
                      </Card>
                    )) || (
                      <p className="text-sm text-muted-foreground text-center py-4">
                        No rules defined for this template
                      </p>
                    )}
                  </div>
                </div>
              </div>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </DashboardLayout>
  );
};

export default RAGOcrPage;