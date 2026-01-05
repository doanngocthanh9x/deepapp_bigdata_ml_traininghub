import { useState } from 'react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import {
  Users,
  Search,
  Plus,
  Edit2,
  Trash2,
  Shield,
  ShieldCheck,
  Eye,
  X,
  Loader2,
  MoreHorizontal,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

interface User {
  id: string;
  name: string;
  email: string;
  role: 'admin' | 'user' | 'viewer';
  status: 'active' | 'inactive';
  createdAt: Date;
}

const mockUsers: User[] = [
  { id: '1', name: 'Admin User', email: 'admin@swift-dashboard.dev', role: 'admin', status: 'active', createdAt: new Date('2024-01-15') },
  { id: '2', name: 'John Doe', email: 'john@example.com', role: 'user', status: 'active', createdAt: new Date('2024-03-20') },
  { id: '3', name: 'Jane Smith', email: 'jane@example.com', role: 'user', status: 'active', createdAt: new Date('2024-05-10') },
  { id: '4', name: 'Bob Wilson', email: 'bob@example.com', role: 'viewer', status: 'inactive', createdAt: new Date('2024-06-01') },
  { id: '5', name: 'Alice Brown', email: 'alice@example.com', role: 'viewer', status: 'active', createdAt: new Date('2024-07-15') },
];

const UserManagement = () => {
  const { toast } = useToast();
  const [users, setUsers] = useState<User[]>(mockUsers);
  const [search, setSearch] = useState('');
  const [showDialog, setShowDialog] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    role: 'user' as User['role'],
  });

  const filteredUsers = users.filter(
    (user) =>
      user.name.toLowerCase().includes(search.toLowerCase()) ||
      user.email.toLowerCase().includes(search.toLowerCase())
  );

  const openAddDialog = () => {
    setEditingUser(null);
    setFormData({ name: '', email: '', role: 'user' });
    setShowDialog(true);
  };

  const openEditDialog = (user: User) => {
    setEditingUser(user);
    setFormData({ name: user.name, email: user.email, role: user.role });
    setShowDialog(true);
  };

  const handleSubmit = async () => {
    if (!formData.name || !formData.email) {
      toast({ title: 'Error', description: 'Please fill all fields.', variant: 'destructive' });
      return;
    }

    setLoading(true);
    await new Promise((r) => setTimeout(r, 500));

    if (editingUser) {
      setUsers((prev) =>
        prev.map((u) => (u.id === editingUser.id ? { ...u, ...formData } : u))
      );
      toast({ title: 'User updated', description: 'User information has been updated.' });
    } else {
      const newUser: User = {
        id: String(Date.now()),
        ...formData,
        status: 'active',
        createdAt: new Date(),
      };
      setUsers((prev) => [...prev, newUser]);
      toast({ title: 'User created', description: 'New user has been added.' });
    }

    setLoading(false);
    setShowDialog(false);
  };

  const deleteUser = (id: string) => {
    setUsers((prev) => prev.filter((u) => u.id !== id));
    toast({ title: 'User deleted', description: 'User has been removed.' });
  };

  const toggleStatus = (id: string) => {
    setUsers((prev) =>
      prev.map((u) =>
        u.id === id ? { ...u, status: u.status === 'active' ? 'inactive' : 'active' } : u
      )
    );
  };

  const getRoleIcon = (role: User['role']) => {
    switch (role) {
      case 'admin':
        return ShieldCheck;
      case 'user':
        return Shield;
      default:
        return Eye;
    }
  };

  const getRoleStyles = (role: User['role']) => {
    switch (role) {
      case 'admin':
        return 'bg-destructive/20 text-destructive';
      case 'user':
        return 'bg-primary/20 text-primary';
      default:
        return 'bg-muted text-muted-foreground';
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 animate-fade-in">
          <div>
            <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
              <Users className="w-6 h-6" />
              User Management
            </h1>
            <p className="text-muted-foreground mt-1">Manage users and their permissions</p>
          </div>
          <Button onClick={openAddDialog}>
            <Plus className="w-4 h-4 mr-2" />
            Add User
          </Button>
        </div>

        {/* Search & Stats */}
        <div className="flex flex-col sm:flex-row gap-4 animate-fade-in" style={{ animationDelay: '50ms' }}>
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              placeholder="Search users..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-10"
            />
          </div>
          <div className="flex gap-2">
            <div className="px-4 py-2 bg-card border border-border rounded-lg text-sm">
              <span className="text-muted-foreground">Total:</span>{' '}
              <span className="font-semibold text-foreground">{users.length}</span>
            </div>
            <div className="px-4 py-2 bg-card border border-border rounded-lg text-sm">
              <span className="text-muted-foreground">Active:</span>{' '}
              <span className="font-semibold text-success">{users.filter((u) => u.status === 'active').length}</span>
            </div>
          </div>
        </div>

        {/* Users Table */}
        <div className="bg-card border border-border rounded-2xl overflow-hidden animate-fade-in" style={{ animationDelay: '100ms' }}>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">User</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">Role</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">Created</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-muted-foreground uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filteredUsers.map((user) => {
                  const RoleIcon = getRoleIcon(user.role);
                  return (
                    <tr key={user.id} className="hover:bg-muted/30 transition-colors">
                      <td className="px-4 py-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary/20 to-accent/20 flex items-center justify-center">
                            <span className="font-semibold text-foreground">
                              {user.name.charAt(0).toUpperCase()}
                            </span>
                          </div>
                          <div>
                            <p className="font-medium text-foreground">{user.name}</p>
                            <p className="text-sm text-muted-foreground">{user.email}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-4">
                        <span className={cn('inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium capitalize', getRoleStyles(user.role))}>
                          <RoleIcon className="w-3 h-3" />
                          {user.role}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        <button
                          onClick={() => toggleStatus(user.id)}
                          className={cn(
                            'px-2.5 py-1 rounded-full text-xs font-medium transition-colors',
                            user.status === 'active'
                              ? 'bg-success/20 text-success hover:bg-success/30'
                              : 'bg-muted text-muted-foreground hover:bg-muted/80'
                          )}
                        >
                          {user.status}
                        </button>
                      </td>
                      <td className="px-4 py-4 text-sm text-muted-foreground">
                        {user.createdAt.toLocaleDateString()}
                      </td>
                      <td className="px-4 py-4 text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="sm">
                              <MoreHorizontal className="w-4 h-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => openEditDialog(user)}>
                              <Edit2 className="w-4 h-4 mr-2" />
                              Edit
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() => deleteUser(user.id)}
                              className="text-destructive focus:text-destructive"
                            >
                              <Trash2 className="w-4 h-4 mr-2" />
                              Delete
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {filteredUsers.length === 0 && (
            <div className="text-center py-12">
              <Users className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No users found</p>
            </div>
          )}
        </div>

        {/* Add/Edit Dialog */}
        <Dialog open={showDialog} onOpenChange={setShowDialog}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{editingUser ? 'Edit User' : 'Add New User'}</DialogTitle>
              <DialogDescription>
                {editingUser ? 'Update user information below.' : 'Fill in the details to create a new user.'}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="name">Full Name</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData((prev) => ({ ...prev, name: e.target.value }))}
                  placeholder="John Doe"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData((prev) => ({ ...prev, email: e.target.value }))}
                  placeholder="john@example.com"
                />
              </div>
              <div className="space-y-2">
                <Label>Role</Label>
                <div className="flex gap-2">
                  {(['admin', 'user', 'viewer'] as const).map((role) => (
                    <Button
                      key={role}
                      type="button"
                      variant={formData.role === role ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => setFormData((prev) => ({ ...prev, role }))}
                      className="capitalize"
                    >
                      {role}
                    </Button>
                  ))}
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setShowDialog(false)}>
                Cancel
              </Button>
              <Button onClick={handleSubmit} disabled={loading}>
                {loading && <Loader2 className="w-4 h-4 animate-spin mr-2" />}
                {editingUser ? 'Save Changes' : 'Create User'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </DashboardLayout>
  );
};

export default UserManagement;
