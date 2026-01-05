import React, { useState, useEffect, useRef } from 'react';
import { Activity, Plus, Upload, Pencil, Download, Zap, RefreshCw } from 'lucide-react';
import { getActivityLogs, type ActivityLog as ActivityLogType } from '@/services/aab0Api';
import { cn } from '@/lib/utils';
import { formatDistanceToNow } from 'date-fns';

interface ActivityLogProps {
  projectId?: string;
}

const ACTION_ICONS: Record<ActivityLogType['action'], React.ReactNode> = {
  create: <Plus className="w-3 h-3" />,
  upload: <Upload className="w-3 h-3" />,
  annotate: <Pencil className="w-3 h-3" />,
  export: <Download className="w-3 h-3" />,
  inference: <Zap className="w-3 h-3" />,
};

const ACTION_COLORS: Record<ActivityLogType['action'], string> = {
  create: 'bg-blue-500/20 text-blue-500',
  upload: 'bg-green-500/20 text-green-500',
  annotate: 'bg-purple-500/20 text-purple-500',
  export: 'bg-orange-500/20 text-orange-500',
  inference: 'bg-yellow-500/20 text-yellow-500',
};

const ActivityLog: React.FC<ActivityLogProps> = ({ projectId }) => {
  const [logs, setLogs] = useState<ActivityLogType[]>([]);
  const [loading, setLoading] = useState(false);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    const fetchLogs = async () => {
      setLoading(true);
      try {
        const data = await getActivityLogs(projectId);
        if (mountedRef.current) {
          setLogs(data);
        }
      } catch (error) {
        console.error('Failed to fetch activity logs:', error);
      } finally {
        if (mountedRef.current) {
          setLoading(false);
        }
      }
    };
    fetchLogs();
  }, [projectId]);

  const handleRefresh = async () => {
    setLoading(true);
    try {
      const data = await getActivityLogs(projectId);
      if (mountedRef.current) {
        setLogs(data);
      }
    } catch (error) {
      console.error('Failed to refresh logs:', error);
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  };

  return (
    <div className="bg-card border border-border rounded-xl p-4 h-full flex flex-col">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Activity className="w-5 h-5 text-primary" />
          <h3 className="font-semibold text-foreground">Activity Log</h3>
        </div>
        <button 
          onClick={handleRefresh}
          className="p-1.5 rounded-lg hover:bg-muted transition-colors"
          disabled={loading}
        >
          <RefreshCw className={cn("w-4 h-4 text-muted-foreground", loading && "animate-spin")} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto space-y-3">
        {loading && logs.length === 0 ? (
          <div className="flex items-center justify-center h-32">
            <RefreshCw className="w-6 h-6 animate-spin text-muted-foreground" />
          </div>
        ) : logs.length === 0 ? (
          <div className="flex items-center justify-center h-32 text-muted-foreground text-sm">
            No activity yet
          </div>
        ) : (
          logs.map((log) => (
            <div 
              key={log.id} 
              className="flex items-start gap-3 p-2 rounded-lg hover:bg-muted/50 transition-colors"
            >
              <div className={cn(
                'p-1.5 rounded-full shrink-0 mt-0.5',
                ACTION_COLORS[log.action]
              )}>
                {ACTION_ICONS[log.action]}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-foreground truncate">
                  {log.description}
                </p>
                <div className="flex items-center gap-2 text-xs text-muted-foreground mt-0.5">
                  <span>{log.userName}</span>
                  <span>•</span>
                  <span>{formatDistanceToNow(new Date(log.timestamp), { addSuffix: true })}</span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default ActivityLog;
