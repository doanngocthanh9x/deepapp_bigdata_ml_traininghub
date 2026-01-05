import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Tag, Plus, FolderOpen, Image, FileText, Clock, MoreVertical, Trash2 } from 'lucide-react';
import DashboardLayout from '@/components/DashboardLayout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { useToast } from '@/hooks/use-toast';
import { getProjects, createProject, type Project } from '@/services/aab0Api';

const ProjectListPage: React.FC = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [newProject, setNewProject] = useState({ name: '', type: 'YOLO' as 'YOLO' | 'OCR' });
  const [creating, setCreating] = useState(false);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const loadProjects = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getProjects();
      if (mountedRef.current) {
        setProjects(data);
      }
    } catch (error) {
      console.error('Failed to load projects:', error);
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  const handleCreateProject = async () => {
    if (!newProject.name.trim()) {
      toast({ title: 'Lỗi', description: 'Vui lòng nhập tên dự án', variant: 'destructive' });
      return;
    }

    setCreating(true);
    try {
      const project = await createProject(newProject);
      if (mountedRef.current) {
        setProjects(prev => [project, ...prev]);
        setDialogOpen(false);
        setNewProject({ name: '', type: 'YOLO' });
        toast({ title: 'Thành công', description: 'Đã tạo dự án mới' });
        // Navigate to model selection
        navigate(`/modules/AA/B0/AAB0_0100/projects/${project.id}/models`);
      }
    } catch (error) {
      toast({ title: 'Lỗi', description: 'Không thể tạo dự án', variant: 'destructive' });
    } finally {
      if (mountedRef.current) {
        setCreating(false);
      }
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  return (
    <DashboardLayout>
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <Tag className="w-6 h-6 text-primary" />
            </div>
            <div>
              <span className="text-xs font-mono text-muted-foreground">AAB0_0100</span>
              <h1 className="text-2xl font-bold text-foreground">Label Studio</h1>
            </div>
          </div>
          
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="w-4 h-4 mr-2" />
                Tạo dự án mới
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Tạo dự án mới</DialogTitle>
                <DialogDescription>
                  Nhập thông tin để tạo dự án annotation mới
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label htmlFor="project-name">Tên dự án</Label>
                  <Input
                    id="project-name"
                    placeholder="Nhập tên dự án..."
                    value={newProject.name}
                    onChange={(e) => setNewProject(prev => ({ ...prev, name: e.target.value }))}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="project-type">Loại dự án</Label>
                  <Select
                    value={newProject.type}
                    onValueChange={(value: 'YOLO' | 'OCR') => setNewProject(prev => ({ ...prev, type: value }))}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="YOLO">YOLO - Object Detection</SelectItem>
                      <SelectItem value="OCR">OCR - Text Recognition</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)}>
                  Hủy
                </Button>
                <Button onClick={handleCreateProject} disabled={creating}>
                  {creating ? 'Đang tạo...' : 'Tạo dự án'}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        <p className="text-muted-foreground">
          Công cụ annotation cho YOLO object detection và OCR text recognition
        </p>

        {/* Project List */}
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2, 3].map((i) => (
              <Card key={i} className="animate-pulse">
                <CardHeader>
                  <div className="h-4 bg-muted rounded w-1/2" />
                  <div className="h-3 bg-muted rounded w-3/4 mt-2" />
                </CardHeader>
                <CardContent>
                  <div className="h-20 bg-muted rounded" />
                </CardContent>
              </Card>
            ))}
          </div>
        ) : projects.length === 0 ? (
          <Card className="py-12">
            <CardContent className="flex flex-col items-center justify-center text-center">
              <FolderOpen className="w-12 h-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-medium text-foreground mb-2">Chưa có dự án nào</h3>
              <p className="text-muted-foreground mb-4">
                Tạo dự án đầu tiên để bắt đầu annotation
              </p>
              <Button onClick={() => setDialogOpen(true)}>
                <Plus className="w-4 h-4 mr-2" />
                Tạo dự án mới
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {projects.map((project) => (
              <Card
                key={project.id}
                className="hover:shadow-lg transition-all hover:border-primary/50 cursor-pointer group"
                onClick={() => navigate(`/modules/AA/B0/AAB0_0100/projects/${project.id}/annotate`)}
              >
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between">
                    <Badge variant={project.type === 'YOLO' ? 'default' : 'secondary'}>
                      {project.type}
                    </Badge>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          <MoreVertical className="w-4 h-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/modules/AA/B0/AAB0_0100/projects/${project.id}/models`);
                          }}
                        >
                          Chọn Model
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/modules/AA/B0/AAB0_0100/projects/${project.id}/labels`);
                          }}
                        >
                          Cấu hình Labels
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/modules/AA/B0/AAB0_0100/projects/${project.id}/export`);
                          }}
                        >
                          Xuất dữ liệu
                        </DropdownMenuItem>
                        <DropdownMenuItem className="text-destructive" onClick={(e) => e.stopPropagation()}>
                          <Trash2 className="w-4 h-4 mr-2" />
                          Xóa dự án
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                  <CardTitle className="text-lg mt-2">{project.name}</CardTitle>
                  <CardDescription>
                    Cập nhật: {formatDate(project.updatedAt)}
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Image className="w-4 h-4" />
                      <span>{project.datasetCount} ảnh</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <FileText className="w-4 h-4" />
                      <span>{project.annotationCount} annotations</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground mt-3">
                    <Clock className="w-3 h-3" />
                    <span>Tạo: {formatDate(project.createdAt)}</span>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default ProjectListPage;
