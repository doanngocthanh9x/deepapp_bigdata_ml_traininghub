import { DashboardLayout } from '@/components/DashboardLayout';
import { Kanban as KanbanIcon, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';

const Kanban = () => {
  const columns = [
    { id: 'backlog', title: 'Backlog', count: 3 },
    { id: 'todo', title: 'To Do', count: 2 },
    { id: 'in-progress', title: 'In Progress', count: 4 },
    { id: 'done', title: 'Done', count: 2 },
  ];

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-6">
        <div className="flex items-center justify-between animate-fade-in">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Kanban Board</h1>
            <p className="text-muted-foreground mt-1">
              Manage tasks and workflows
            </p>
          </div>
          <Button className="gap-2">
            <Plus className="w-4 h-4" />
            Add Task
          </Button>
        </div>

        <div className="grid gap-4 lg:grid-cols-4">
          {columns.map((column, index) => (
            <div
              key={column.id}
              className="bg-muted/30 rounded-xl p-4 min-h-[500px] animate-fade-in"
              style={{ animationDelay: `${index * 50}ms` }}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-foreground">{column.title}</h3>
                <span className="text-xs bg-muted px-2 py-1 rounded-full text-muted-foreground">
                  {column.count}
                </span>
              </div>
              <div className="space-y-3">
                {Array.from({ length: column.count }).map((_, i) => (
                  <div
                    key={i}
                    className="bg-card border border-border rounded-lg p-4 cursor-pointer hover:shadow-md hover:border-primary/20 transition-all"
                  >
                    <p className="text-sm font-medium text-card-foreground">
                      Sample Task {i + 1}
                    </p>
                    <p className="text-xs text-muted-foreground mt-1">
                      Task description goes here
                    </p>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  );
};

export default Kanban;
