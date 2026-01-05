import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Plus, Edit, Trash2, Tag, RefreshCw, AlertCircle, Sparkles } from 'lucide-react';
import { EntityType, EntityTypeFormData, EntityTypeAPI } from './entityTypeAPI';
import { cn } from '@/lib/utils';
import { Separator } from '@/components/ui/separator';

interface EntityTypeManagerProps {
  templateId?: string;
  onEntityTypesChange?: (entityTypes: EntityType[]) => void;
}

const AVAILABLE_COLORS = [
  { value: 'blue', label: 'Xanh dương', class: 'bg-blue-500' },
  { value: 'green', label: 'Xanh lá', class: 'bg-green-500' },
  { value: 'purple', label: 'Tím', class: 'bg-purple-500' },
  { value: 'orange', label: 'Cam', class: 'bg-orange-500' },
  { value: 'pink', label: 'Hồng', class: 'bg-pink-500' },
  { value: 'red', label: 'Đỏ', class: 'bg-red-500' },
  { value: 'indigo', label: 'Chàm', class: 'bg-indigo-500' },
  { value: 'yellow', label: 'Vàng', class: 'bg-yellow-500' },
  { value: 'cyan', label: 'Xanh lơ', class: 'bg-cyan-500' },
  { value: 'teal', label: 'Xanh ngọc', class: 'bg-teal-500' },
  { value: 'gray', label: 'Xám', class: 'bg-gray-500' }
];

const AVAILABLE_ICONS = [
  { value: '👤', label: 'User' },
  { value: '👥', label: 'Users' },
  { value: '❤️', label: 'Heart' },
  { value: '📅', label: 'Calendar' },
  { value: '📍', label: 'Location' },
  { value: '📞', label: 'Phone' },
  { value: '📄', label: 'Document' },
  { value: '💳', label: 'Card' },
  { value: '🩺', label: 'Stethoscope' },
  { value: '🏢', label: 'Building' },
  { value: '🏥', label: 'Hospital' },
  { value: '💊', label: 'Pill' },
  { value: '🧪', label: 'TestTube' },
  { value: '⚠️', label: 'Alert' },
  { value: '#️⃣', label: 'Hash' },
  { value: '🏷️', label: 'Tag' },
  { value: '⚡', label: 'Zap' },
  { value: '🩹', label: 'Treatment' }
];

export function EntityTypeManager({ templateId, onEntityTypesChange }: EntityTypeManagerProps) {
  const [entityTypes, setEntityTypes] = useState<EntityType[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showDialog, setShowDialog] = useState(false);
  const [editingEntity, setEditingEntity] = useState<EntityType | null>(null);
  const [formData, setFormData] = useState<EntityTypeFormData>({
    entity_code: '',
    display_label: '',
    description: '',
    color: 'blue',
    icon: '🏷️',
    display_order: 0,
    examples: ''
  });

  useEffect(() => {
    loadEntityTypes();
  }, [templateId]);

  const loadEntityTypes = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const types = await EntityTypeAPI.getEntityTypes(templateId);
      setEntityTypes(types);
      onEntityTypesChange?.(types);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingEntity(null);
    setFormData({
      template_id: templateId,
      entity_code: '',
      display_label: '',
      description: '',
      color: 'blue',
      icon: 'Tag',
      display_order: entityTypes.length,
      examples: ''
    });
    setShowDialog(true);
  };

  const handleEdit = (entity: EntityType) => {
    setEditingEntity(entity);
    setFormData({
      entity_code: entity.entity_code,
      display_label: entity.display_label,
      description: entity.description || '',
      color: entity.color || 'blue',
      icon: entity.icon || 'Tag',
      display_order: entity.display_order,
      examples: entity.examples || ''
    });
    setShowDialog(true);
  };

  const handleSave = async () => {
    setError(null);
    
    try {
      if (editingEntity) {
        await EntityTypeAPI.updateEntityType(editingEntity.id, formData);
      } else {
        await EntityTypeAPI.createEntityType(formData);
      }
      
      setShowDialog(false);
      loadEntityTypes();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Bạn có chắc muốn xóa entity type này?')) return;
    
    setError(null);
    
    try {
      await EntityTypeAPI.deleteEntityType(id);
      loadEntityTypes();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleInitializeDefaults = async () => {
    if (!confirm('Tạo các entity types mặc định cho văn bản y tế?')) return;
    
    setError(null);
    
    try {
      await EntityTypeAPI.initializeDefaultEntityTypes();
      loadEntityTypes();
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Section */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-2xl font-bold flex items-center gap-2">
            <Tag className="h-6 w-6" />
            Quản lý Entity Types
          </h3>
          <p className="text-muted-foreground mt-1">
            {templateId 
              ? `Quản lý entity types cho template: ${templateId}` 
              : 'Quản lý entity types toàn cục (áp dụng cho tất cả datasets)'
            }
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadEntityTypes} disabled={loading}>
            <RefreshCw className={cn("h-4 w-4", loading && "animate-spin")} />
          </Button>
          {!templateId && entityTypes.length === 0 && (
            <Button variant="outline" size="sm" onClick={handleInitializeDefaults}>
              <Sparkles className="h-4 w-4 mr-1" />
              Tạo mặc định
            </Button>
          )}
          <Button size="sm" onClick={handleCreate}>
            <Plus className="h-4 w-4 mr-1" />
            Thêm mới
          </Button>
        </div>
      </div>

      <Separator />

      {/* Error Message */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-red-600 mt-0.5 flex-shrink-0" />
          <div>
            <p className="font-medium text-red-800">Có lỗi xảy ra</p>
            <p className="text-sm text-red-700">{error}</p>
          </div>
        </div>
      )}

      {/* Content Section */}
      {loading ? (
        <Card>
          <CardContent className="py-12">
            <div className="text-center">
              <RefreshCw className="h-8 w-8 mx-auto mb-3 animate-spin text-muted-foreground" />
              <p className="text-muted-foreground">Đang tải entity types...</p>
            </div>
          </CardContent>
        </Card>
      ) : entityTypes.length === 0 ? (
        <Card>
          <CardContent className="py-12">
            <div className="text-center">
              <Tag className="h-12 w-12 mx-auto mb-4 text-muted-foreground opacity-50" />
              <h4 className="font-semibold mb-2">Chưa có Entity Types</h4>
              <p className="text-sm text-muted-foreground mb-4">
                Bắt đầu bằng cách tạo entity types mặc định hoặc thêm mới
              </p>
              <div className="flex gap-2 justify-center">
                {!templateId && (
                  <Button variant="outline" onClick={handleInitializeDefaults}>
                    <Sparkles className="h-4 w-4 mr-1" />
                    Tạo 15 types mặc định
                  </Button>
                )}
                <Button onClick={handleCreate}>
                  <Plus className="h-4 w-4 mr-1" />
                  Thêm thủ công
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      ) : (
        <>
          {/* Stats */}
          <div className="flex items-center gap-6 text-sm">
            <div className="flex items-center gap-2">
              <span className="font-medium">Tổng số:</span>
              <Badge variant="secondary">{entityTypes.length}</Badge>
            </div>
            <div className="flex items-center gap-2">
              <span className="font-medium">Active:</span>
              <Badge variant="secondary">{entityTypes.filter(e => e.active).length}</Badge>
            </div>
          </div>

          {/* Entity Types Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {entityTypes.map((entity) => {
              const colorConfig = AVAILABLE_COLORS.find(c => c.value === entity.color);
              return (
                <Card key={entity.id} className="hover:shadow-md transition-shadow">
                  <CardHeader className="pb-3">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2 flex-1">
                        <div className={cn("w-3 h-3 rounded-full flex-shrink-0", colorConfig?.class || 'bg-gray-400')} />
                        <div className="flex items-center gap-2 flex-1">
                          {entity.icon && (
                            <span className="text-lg flex-shrink-0">{entity.icon}</span>
                          )}
                          <div className="flex flex-col gap-1 flex-1 min-w-0">
                            <h4 className="font-semibold text-sm leading-tight truncate">
                              {entity.display_label}
                            </h4>
                            <Badge variant="outline" className="font-mono text-xs w-fit">
                              {entity.entity_code}
                            </Badge>
                          </div>
                        </div>
                      </div>
                      <div className="flex gap-1 flex-shrink-0 ml-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleEdit(entity)}
                          className="h-8 w-8 p-0"
                          title="Chỉnh sửa"
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(entity.id)}
                          className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                          title="Xóa"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent className="pt-0">
                    {entity.description && (
                      <p className="text-sm text-muted-foreground mb-2 line-clamp-2">
                        {entity.description}
                      </p>
                    )}
                    {entity.examples && (
                      <div className="mt-3 pt-3 border-t">
                        <p className="text-xs text-muted-foreground mb-1">Ví dụ:</p>
                        <p className="text-xs font-medium line-clamp-1">{entity.examples}</p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </>
      )}

      {/* Create/Edit Dialog */}
        <Dialog open={showDialog} onOpenChange={setShowDialog}>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>
                {editingEntity ? 'Chỉnh sửa Entity Type' : 'Thêm Entity Type mới'}
              </DialogTitle>
              <DialogDescription>
                {templateId ? `Cho template: ${templateId}` : 'Global entity type (áp dụng cho tất cả)'}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-4">
              {/* Basic Info Section */}
              <div className="space-y-3">
                <h4 className="text-sm font-medium text-gray-700 flex items-center gap-2">
                  <Sparkles className="w-4 h-4" />
                  Thông tin cơ bản
                </h4>
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1.5">
                    <Label htmlFor="entity_code">
                      Mã Entity <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="entity_code"
                      value={formData.entity_code}
                      onChange={(e) => setFormData({ ...formData, entity_code: e.target.value.toUpperCase() })}
                      placeholder="VD: PERSON_NAME"
                      disabled={!!editingEntity}
                      className={!formData.entity_code ? 'border-red-300' : ''}
                    />
                    {!formData.entity_code && (
                      <p className="text-xs text-red-500">Vui lòng nhập mã entity</p>
                    )}
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="display_label">
                      Tên hiển thị <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="display_label"
                      value={formData.display_label}
                      onChange={(e) => setFormData({ ...formData, display_label: e.target.value })}
                      placeholder="VD: Tên người bệnh"
                      className={!formData.display_label ? 'border-red-300' : ''}
                    />
                    {!formData.display_label && (
                      <p className="text-xs text-red-500">Vui lòng nhập tên hiển thị</p>
                    )}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="description">Mô tả</Label>
                  <Textarea
                    id="description"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Mô tả chi tiết về entity type này..."
                    rows={2}
                    className="resize-none"
                  />
                </div>
              </div>

              <Separator />

              {/* Visual Settings Section */}
              <div className="space-y-3">
                <h4 className="text-sm font-medium text-gray-700">Cài đặt giao diện</h4>
                <div className="grid grid-cols-3 gap-3">
                  <div className="space-y-1.5">
                    <Label htmlFor="color">
                      Màu sắc <span className="text-red-500">*</span>
                    </Label>
                    <Select
                      value={formData.color}
                      onValueChange={(value) => setFormData({ ...formData, color: value })}
                    >
                      <SelectTrigger className={!formData.color ? 'border-red-300' : ''}>
                        <SelectValue placeholder="Chọn màu">
                          {formData.color && (
                            <div className="flex items-center gap-2">
                              <div className={`w-3 h-3 rounded ${AVAILABLE_COLORS.find(c => c.value === formData.color)?.class || 'bg-gray-500'}`}></div>
                              <span>{AVAILABLE_COLORS.find(c => c.value === formData.color)?.label || formData.color}</span>
                            </div>
                          )}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {AVAILABLE_COLORS.map((colorConfig) => (
                          <SelectItem key={colorConfig.value} value={colorConfig.value}>
                            <div className="flex items-center gap-2">
                              <div className={`w-3 h-3 rounded ${colorConfig.class}`}></div>
                              <span>{colorConfig.label}</span>
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="icon">Biểu tượng</Label>
                    <Select
                      value={formData.icon}
                      onValueChange={(value) => setFormData({ ...formData, icon: value })}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Chọn icon">
                          {formData.icon && (
                            <div className="flex items-center gap-2">
                              <span className="text-lg">{formData.icon}</span>
                              <span className="text-sm text-muted-foreground">
                                {AVAILABLE_ICONS.find(ic => ic.value === formData.icon)?.label}
                              </span>
                            </div>
                          )}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {AVAILABLE_ICONS.map((iconConfig) => (
                          <SelectItem key={iconConfig.value} value={iconConfig.value}>
                            <div className="flex items-center gap-2">
                              <span className="text-lg">{iconConfig.value}</span>
                              <span className="text-sm">{iconConfig.label}</span>
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="display_order">Thứ tự hiển thị</Label>
                    <Input
                      id="display_order"
                      type="number"
                      min="0"
                      value={formData.display_order}
                      onChange={(e) => setFormData({ ...formData, display_order: parseInt(e.target.value) || 0 })}
                      placeholder="0"
                    />
                  </div>
                </div>
              </div>

              <Separator />

              {/* Examples Section */}
              <div className="space-y-3">
                <h4 className="text-sm font-medium text-gray-700">Ví dụ</h4>
                <div className="space-y-1.5">
                  <Label htmlFor="examples">Các ví dụ mẫu (phân cách bằng dấu phẩy)</Label>
                  <Input
                    id="examples"
                    value={formData.examples}
                    onChange={(e) => setFormData({ ...formData, examples: e.target.value })}
                    placeholder="VD: Nguyễn Văn A, Trần Thị B, Lê Văn C"
                  />
                  <p className="text-xs text-gray-500">
                    Nhập các ví dụ để hướng dẫn người dùng khi gán nhãn
                  </p>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button 
                variant="outline" 
                onClick={() => {
                  setShowDialog(false);
                  setEditingEntity(null);
                  setFormData({
                    entity_code: '',
                    display_label: '',
                    description: '',
                    color: 'blue',
                    icon: '👤',
                    display_order: 0,
                    examples: ''
                  });
                }}
              >
                Hủy bỏ
              </Button>
              <Button 
                onClick={handleSave}
                disabled={!formData.entity_code || !formData.display_label || !formData.color}
              >
                {editingEntity ? '💾 Cập nhật' : '✨ Tạo mới'}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
    </div>
  );
}
