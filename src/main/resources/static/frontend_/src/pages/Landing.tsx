import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { 
  Zap, 
  Users, 
  ScanText, 
  Brain, 
  Shield, 
  BarChart3,
  ArrowRight,
  Star,
  CheckCircle2,
  Github,
  Twitter,
  Linkedin,
  Mail,
  Menu,
  X
} from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/lib/utils';
import { useThemeStore } from '@/stores/themeStore';

const Landing = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { theme, toggleTheme } = useThemeStore();

  const features = [
    {
      icon: ScanText,
      title: 'OCR Module',
      description: 'Nhận dạng và trích xuất văn bản từ hình ảnh với độ chính xác cao sử dụng AI tiên tiến.',
    },
    {
      icon: Brain,
      title: 'AI Modules',
      description: 'Tích hợp các mô-đun AI mạnh mẽ cho phân tích dữ liệu, xử lý ngôn ngữ tự nhiên và hơn thế nữa.',
    },
    {
      icon: Users,
      title: 'Quản lý người dùng',
      description: 'Hệ thống quản lý người dùng toàn diện với phân quyền và kiểm soát truy cập chi tiết.',
    },
    {
      icon: Shield,
      title: 'Bảo mật cao',
      description: 'Xác thực đa lớp, mã hóa dữ liệu và tuân thủ các tiêu chuẩn bảo mật quốc tế.',
    },
    {
      icon: BarChart3,
      title: 'Phân tích & Báo cáo',
      description: 'Dashboard trực quan với biểu đồ và báo cáo chi tiết theo thời gian thực.',
    },
    {
      icon: Zap,
      title: 'Hiệu suất tối ưu',
      description: 'Kiến trúc microservices đảm bảo tốc độ xử lý nhanh và khả năng mở rộng linh hoạt.',
    },
  ];

  const testimonials = [
    {
      name: 'Nguyễn Văn An',
      role: 'CTO, TechCorp Vietnam',
      avatar: 'A',
      content: 'Swift Dashboard đã giúp chúng tôi giảm 60% thời gian xử lý tài liệu. OCR module hoạt động cực kỳ chính xác!',
      rating: 5,
    },
    {
      name: 'Trần Thị Mai',
      role: 'Product Manager, StartupXYZ',
      avatar: 'M',
      content: 'Giao diện trực quan, dễ sử dụng. Team của tôi đã onboard chỉ trong 1 ngày. Highly recommended!',
      rating: 5,
    },
    {
      name: 'Lê Hoàng Minh',
      role: 'Developer Lead, FinTech Inc',
      avatar: 'L',
      content: 'API documentation rõ ràng, integration nhanh chóng. Đội ngũ support rất nhiệt tình và chuyên nghiệp.',
      rating: 5,
    },
  ];

  const stats = [
    { value: '10K+', label: 'Người dùng' },
    { value: '500+', label: 'Doanh nghiệp' },
    { value: '99.9%', label: 'Uptime' },
    { value: '24/7', label: 'Hỗ trợ' },
  ];

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur border-b border-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <Link to="/" className="flex items-center gap-2">
              <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-primary to-accent flex items-center justify-center">
                <span className="text-primary-foreground font-bold text-lg">S</span>
              </div>
              <span className="font-bold text-lg text-foreground hidden sm:block">Swift Dashboard</span>
            </Link>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center gap-8">
              <Link to="/dashboard" className="text-muted-foreground hover:text-foreground transition-colors">
                Dashboard
              </Link>
              <Link to="/modules/AA/A0/AAA0_0101" className="text-muted-foreground hover:text-foreground transition-colors">
                Modules
              </Link>
              <a href="#features" className="text-muted-foreground hover:text-foreground transition-colors">
                Tính năng
              </a>
              <a href="#testimonials" className="text-muted-foreground hover:text-foreground transition-colors">
                Đánh giá
              </a>
            </nav>

            {/* Desktop Actions */}
            <div className="hidden md:flex items-center gap-3">
              <Button
                variant="ghost"
                size="icon"
                onClick={toggleTheme}
                className="text-muted-foreground"
              >
                {theme === 'dark' ? '☀️' : '🌙'}
              </Button>
              <Link to="/auth">
                <Button variant="ghost">Đăng nhập</Button>
              </Link>
              <Link to="/auth">
                <Button className="bg-gradient-to-r from-primary to-accent hover:opacity-90 transition-opacity">
                  Đăng ký miễn phí
                </Button>
              </Link>
            </div>

            {/* Mobile Menu Button */}
            <button
              className="md:hidden p-2 text-muted-foreground"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden border-t border-border bg-background animate-fade-in">
            <div className="px-4 py-4 space-y-3">
              <Link
                to="/dashboard"
                className="block py-2 text-foreground"
                onClick={() => setMobileMenuOpen(false)}
              >
                Dashboard
              </Link>
              <Link
                to="/modules/AA/A0/AAA0_0101"
                className="block py-2 text-foreground"
                onClick={() => setMobileMenuOpen(false)}
              >
                Modules
              </Link>
              <a href="#features" className="block py-2 text-foreground" onClick={() => setMobileMenuOpen(false)}>
                Tính năng
              </a>
              <a href="#testimonials" className="block py-2 text-foreground" onClick={() => setMobileMenuOpen(false)}>
                Đánh giá
              </a>
              <div className="pt-3 border-t border-border space-y-2">
                <Link to="/auth" onClick={() => setMobileMenuOpen(false)}>
                  <Button variant="outline" className="w-full">Đăng nhập</Button>
                </Link>
                <Link to="/auth" onClick={() => setMobileMenuOpen(false)}>
                  <Button className="w-full bg-gradient-to-r from-primary to-accent">Đăng ký miễn phí</Button>
                </Link>
              </div>
            </div>
          </div>
        )}
      </header>

      {/* Hero Section */}
      <section className="relative overflow-hidden">
        {/* Background Gradient */}
        <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-accent/5" />
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-accent/10 rounded-full blur-3xl" />

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-32">
          <div className="text-center animate-fade-in">
            {/* Badge */}
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 mb-6">
              <Zap className="w-4 h-4 text-primary" />
              <span className="text-sm font-medium text-primary">Phiên bản mới 2.0</span>
            </div>

            {/* Title */}
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-foreground mb-6 leading-tight">
              Nền tảng quản lý
              <span className="block bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                thông minh cho doanh nghiệp
              </span>
            </h1>

            {/* Description */}
            <p className="text-lg sm:text-xl text-muted-foreground max-w-2xl mx-auto mb-8">
              Tích hợp OCR, AI và quản lý người dùng trong một nền tảng duy nhất. 
              Tối ưu hóa quy trình làm việc của bạn ngay hôm nay.
            </p>

            {/* CTA Buttons */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link to="/auth">
                <Button size="lg" className="bg-gradient-to-r from-primary to-accent hover:opacity-90 transition-opacity text-lg px-8 h-12">
                  Bắt đầu miễn phí
                  <ArrowRight className="w-5 h-5 ml-2" />
                </Button>
              </Link>
              <Link to="/dashboard">
                <Button size="lg" variant="outline" className="text-lg px-8 h-12">
                  Xem Demo
                </Button>
              </Link>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-8 mt-16 pt-16 border-t border-border">
              {stats.map((stat, index) => (
                <div key={index} className="text-center animate-fade-in" style={{ animationDelay: `${index * 100}ms` }}>
                  <div className="text-3xl sm:text-4xl font-bold text-foreground">{stat.value}</div>
                  <div className="text-sm text-muted-foreground mt-1">{stat.label}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 bg-muted/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16 animate-fade-in">
            <h2 className="text-3xl sm:text-4xl font-bold text-foreground mb-4">
              Tính năng nổi bật
            </h2>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Khám phá những công cụ mạnh mẽ giúp bạn quản lý công việc hiệu quả hơn
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature, index) => (
              <div
                key={index}
                className="group p-6 bg-card border border-border rounded-2xl hover:border-primary/50 hover:shadow-lg transition-all duration-300 animate-fade-in"
                style={{ animationDelay: `${index * 100}ms` }}
              >
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary/10 to-accent/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                  <feature.icon className="w-6 h-6 text-primary" />
                </div>
                <h3 className="text-xl font-semibold text-foreground mb-2">{feature.title}</h3>
                <p className="text-muted-foreground">{feature.description}</p>
              </div>
            ))}
          </div>

          <div className="text-center mt-12">
            <Link to="/modules/AA/A0/AAA0_0101">
              <Button variant="outline" size="lg">
                Khám phá tất cả tính năng
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section id="testimonials" className="py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16 animate-fade-in">
            <h2 className="text-3xl sm:text-4xl font-bold text-foreground mb-4">
              Khách hàng nói gì về chúng tôi
            </h2>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Hàng nghìn doanh nghiệp đã tin tưởng sử dụng Swift Dashboard
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {testimonials.map((testimonial, index) => (
              <div
                key={index}
                className="p-6 bg-card border border-border rounded-2xl animate-fade-in"
                style={{ animationDelay: `${index * 100}ms` }}
              >
                {/* Rating */}
                <div className="flex gap-1 mb-4">
                  {[...Array(testimonial.rating)].map((_, i) => (
                    <Star key={i} className="w-5 h-5 fill-warning text-warning" />
                  ))}
                </div>

                {/* Content */}
                <p className="text-foreground mb-6">"{testimonial.content}"</p>

                {/* Author */}
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center">
                    <span className="text-primary-foreground font-medium">{testimonial.avatar}</span>
                  </div>
                  <div>
                    <div className="font-medium text-foreground">{testimonial.name}</div>
                    <div className="text-sm text-muted-foreground">{testimonial.role}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-gradient-to-br from-primary/10 via-background to-accent/10">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center animate-fade-in">
          <h2 className="text-3xl sm:text-4xl font-bold text-foreground mb-4">
            Sẵn sàng bắt đầu?
          </h2>
          <p className="text-lg text-muted-foreground mb-8">
            Đăng ký miễn phí và trải nghiệm ngay hôm nay. Không cần thẻ tín dụng.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link to="/auth">
              <Button size="lg" className="bg-gradient-to-r from-primary to-accent hover:opacity-90 transition-opacity text-lg px-8 h-12">
                Đăng ký miễn phí
                <ArrowRight className="w-5 h-5 ml-2" />
              </Button>
            </Link>
            <Button size="lg" variant="outline" className="text-lg px-8 h-12">
              <Mail className="w-5 h-5 mr-2" />
              Liên hệ sales
            </Button>
          </div>

          {/* Trust badges */}
          <div className="flex flex-wrap items-center justify-center gap-4 mt-8 text-sm text-muted-foreground">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-success" />
              <span>Miễn phí 14 ngày</span>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-success" />
              <span>Không cần thẻ tín dụng</span>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-success" />
              <span>Hỗ trợ 24/7</span>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-card border-t border-border py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-8">
            {/* Company */}
            <div className="col-span-2 md:col-span-1">
              <Link to="/" className="flex items-center gap-2 mb-4">
                <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-primary to-accent flex items-center justify-center">
                  <span className="text-primary-foreground font-bold text-lg">S</span>
                </div>
                <span className="font-bold text-lg text-foreground">Swift Dashboard</span>
              </Link>
              <p className="text-sm text-muted-foreground mb-4">
                Nền tảng quản lý thông minh cho doanh nghiệp hiện đại.
              </p>
              <div className="flex gap-3">
                <a href="#" className="p-2 rounded-lg bg-muted hover:bg-muted/80 transition-colors">
                  <Github className="w-5 h-5 text-muted-foreground" />
                </a>
                <a href="#" className="p-2 rounded-lg bg-muted hover:bg-muted/80 transition-colors">
                  <Twitter className="w-5 h-5 text-muted-foreground" />
                </a>
                <a href="#" className="p-2 rounded-lg bg-muted hover:bg-muted/80 transition-colors">
                  <Linkedin className="w-5 h-5 text-muted-foreground" />
                </a>
              </div>
            </div>

            {/* Product */}
            <div>
              <h4 className="font-semibold text-foreground mb-4">Sản phẩm</h4>
              <ul className="space-y-2 text-sm">
                <li><Link to="/dashboard" className="text-muted-foreground hover:text-foreground transition-colors">Dashboard</Link></li>
                <li><Link to="/modules/AA/A0/AAA0_0101" className="text-muted-foreground hover:text-foreground transition-colors">OCR Module</Link></li>
                <li><Link to="/users" className="text-muted-foreground hover:text-foreground transition-colors">Quản lý người dùng</Link></li>
                <li><Link to="/kanban" className="text-muted-foreground hover:text-foreground transition-colors">Kanban</Link></li>
              </ul>
            </div>

            {/* Resources */}
            <div>
              <h4 className="font-semibold text-foreground mb-4">Tài nguyên</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="text-muted-foreground hover:text-foreground transition-colors">Tài liệu</a></li>
                <li><a href="#" className="text-muted-foreground hover:text-foreground transition-colors">API Reference</a></li>
                <li><a href="#" className="text-muted-foreground hover:text-foreground transition-colors">Blog</a></li>
                <li><a href="#" className="text-muted-foreground hover:text-foreground transition-colors">Community</a></li>
              </ul>
            </div>

            {/* Contact */}
            <div>
              <h4 className="font-semibold text-foreground mb-4">Liên hệ</h4>
              <ul className="space-y-2 text-sm">
                <li className="text-muted-foreground">Email: support@swift.dev</li>
                <li className="text-muted-foreground">Hotline: 1800-1234</li>
                <li className="text-muted-foreground">Địa chỉ: Hà Nội, Việt Nam</li>
              </ul>
            </div>
          </div>

          {/* Bottom */}
          <div className="pt-8 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-4">
            <p className="text-sm text-muted-foreground">
              © 2024 Swift Dashboard. All rights reserved.
            </p>
            <div className="flex gap-6 text-sm">
              <a href="#" className="text-muted-foreground hover:text-foreground transition-colors">Điều khoản sử dụng</a>
              <a href="#" className="text-muted-foreground hover:text-foreground transition-colors">Chính sách bảo mật</a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Landing;
