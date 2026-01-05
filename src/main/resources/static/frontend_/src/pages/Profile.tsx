import { useState } from 'react';
import { DashboardLayout } from '@/components/DashboardLayout';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { auth, User } from '@/utils/auth';
import { User as UserIcon, Mail, Shield, Camera, Save, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

const Profile = () => {
  const { toast } = useToast();
  const [user, setUser] = useState<User>(() => auth.autoLogin());
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: user.name,
    email: user.email,
  });

  const handleSave = async () => {
    setLoading(true);
    try {
      // Mock save - update localStorage
      const updatedUser = { ...user, ...formData };
      localStorage.setItem('swift_dashboard_auth', JSON.stringify(updatedUser));
      setUser(updatedUser);
      toast({
        title: 'Profile updated',
        description: 'Your profile has been saved successfully.',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to update profile.',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const roleColors: Record<string, string> = {
    admin: 'bg-destructive/20 text-destructive',
    user: 'bg-primary/20 text-primary',
    viewer: 'bg-muted text-muted-foreground',
  };

  return (
    <DashboardLayout>
      <div className="max-w-3xl mx-auto space-y-8">
        {/* Header */}
        <div className="animate-fade-in">
          <h1 className="text-2xl font-bold text-foreground">Profile</h1>
          <p className="text-muted-foreground mt-1">Manage your account information</p>
        </div>

        {/* Profile Card */}
        <div className="bg-card border border-border rounded-2xl overflow-hidden animate-fade-in" style={{ animationDelay: '50ms' }}>
          {/* Cover Image */}
          <div className="h-32 bg-gradient-to-r from-primary/20 to-accent/20" />

          {/* Avatar & Info */}
          <div className="px-6 pb-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-end gap-4 -mt-12">
              <div className="relative">
                <div className="w-24 h-24 rounded-2xl bg-gradient-to-br from-primary to-accent flex items-center justify-center border-4 border-card shadow-lg">
                  {user.avatar ? (
                    <img src={user.avatar} alt={user.name} className="w-full h-full rounded-xl object-cover" />
                  ) : (
                    <span className="text-3xl font-bold text-primary-foreground">
                      {user.name.charAt(0).toUpperCase()}
                    </span>
                  )}
                </div>
                <button className="absolute -bottom-1 -right-1 p-2 rounded-full bg-background border border-border shadow-sm hover:bg-muted transition-colors">
                  <Camera className="w-4 h-4 text-muted-foreground" />
                </button>
              </div>
              <div className="flex-1">
                <h2 className="text-xl font-semibold text-foreground">{user.name}</h2>
                <p className="text-muted-foreground">{user.email}</p>
              </div>
              <span className={cn('px-3 py-1 rounded-full text-xs font-medium capitalize', roleColors[user.role])}>
                <Shield className="w-3 h-3 inline mr-1" />
                {user.role}
              </span>
            </div>
          </div>
        </div>

        {/* Edit Form */}
        <div className="bg-card border border-border rounded-2xl p-6 animate-fade-in" style={{ animationDelay: '100ms' }}>
          <h3 className="text-lg font-semibold text-foreground mb-4">Edit Information</h3>
          
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="name">Full Name</Label>
              <div className="relative">
                <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData((prev) => ({ ...prev, name: e.target.value }))}
                  className="pl-10"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">Email Address</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData((prev) => ({ ...prev, email: e.target.value }))}
                  className="pl-10"
                />
              </div>
            </div>
          </div>

          <div className="mt-6 flex justify-end">
            <Button onClick={handleSave} disabled={loading}>
              {loading ? (
                <Loader2 className="w-4 h-4 animate-spin mr-2" />
              ) : (
                <Save className="w-4 h-4 mr-2" />
              )}
              Save Changes
            </Button>
          </div>
        </div>

        {/* Account Stats */}
        <div className="grid gap-4 sm:grid-cols-3 animate-fade-in" style={{ animationDelay: '150ms' }}>
          <div className="bg-card border border-border rounded-xl p-4">
            <p className="text-sm text-muted-foreground">User ID</p>
            <p className="font-mono text-sm text-foreground mt-1">{user.id}</p>
          </div>
          <div className="bg-card border border-border rounded-xl p-4">
            <p className="text-sm text-muted-foreground">Account Type</p>
            <p className="font-medium text-foreground mt-1 capitalize">{user.role}</p>
          </div>
          <div className="bg-card border border-border rounded-xl p-4">
            <p className="text-sm text-muted-foreground">Member Since</p>
            <p className="font-medium text-foreground mt-1">Dec 2024</p>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default Profile;
