import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { useEffect } from "react";
import { useThemeStore } from "@/stores/themeStore";
import "@/i18n"; // Initialize i18n
import Landing from "./pages/Landing";
import Dashboard from "./pages/Dashboard";
import Kanban from "./pages/Kanban";
import Roadmap from "./pages/Roadmap";
import Settings from "./pages/Settings";
import NotFound from "./pages/NotFound";
import Auth from "./pages/Auth";
import Profile from "./pages/Profile";
import Notifications from "./pages/Notifications";
import UserManagement from "./pages/UserManagement";
import I18nDemo from "./pages/I18nDemo";
import AuthWorkerPage from "./modules/AA/A0/AAA0_0100/AuthWorkerPage";
import OcrPage from "./modules/AA/A0/AAA0_0101/OcrPage";
import OcrPipelinePage from "./modules/AA/A0/AAA0_0102/OcrPipelinePage";
import PaddleOcrPage from "./modules/AA/A0/AAA0_0201/PaddleOcrPage";
import RAGOcrPage from "./modules/AA/A0/AAA0_0202/RAGOcrPage";
import DischargePaperOcrPage from "./modules/AA/A0/AAA0_0103/DischargePaperOcrPage";
import DocumentManagementPage from "./modules/AA/A0/AAA0_0104/DocumentManagementPage";
import YoloDetectionPage from "./modules/AA/A0/AAA0_0105/YoloDetectionPage";
import NERTrainingPage from "./modules/AA/A0/AAA0_0203/NERTrainingPage";
import LLMInferencePage from "./modules/AA/A0/AAA0_0300/LLMInferencePage";
import ModulesLanding from "./pages/ModulesLanding";
import ProjectListPage from "./modules/AA/B0/AAB0_0100/pages/ProjectListPage";
import ModelSelectPage from "./modules/AA/B0/AAB0_0100/pages/ModelSelectPage";
import LabelConfigPage from "./modules/AA/B0/AAB0_0100/pages/LabelConfigPage";
import AnnotatePage from "./modules/AA/B0/AAB0_0100/pages/AnnotatePage";
import ExportPage from "./modules/AA/B0/AAB0_0100/pages/ExportPage";
const queryClient = new QueryClient();

const ThemeInitializer = ({ children }: { children: React.ReactNode }) => {
  const { theme } = useThemeStore();

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
  }, [theme]);

  return <>{children}</>;
};

const App = () => (
  <QueryClientProvider client={queryClient}>
    <ThemeInitializer>
      <TooltipProvider>
        <Toaster />
        <Sonner />
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/auth" element={<Auth />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/notifications" element={<Notifications />} />
            <Route path="/users" element={<UserManagement />} />
            <Route path="/kanban" element={<Kanban />} />
            <Route path="/roadmap" element={<Roadmap />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/i18n-demo" element={<I18nDemo />} />
            <Route path="/modules" element={<ModulesLanding />} />
            <Route path="/modules/AA/A0/AAA0_0100" element={<AuthWorkerPage />} />
            <Route path="/modules/AA/A0/AAA0_0101" element={<OcrPage />} />
            <Route path="/modules/AA/A0/AAA0_0102" element={<OcrPipelinePage />} />
            <Route path="/modules/AA/A0/AAA0_0201" element={<PaddleOcrPage />} />
            <Route path="/modules/AA/A0/AAA0_0202" element={<RAGOcrPage />} />
            <Route path="/modules/AA/A0/AAA0_0103" element={<DischargePaperOcrPage />} />
            <Route path="/modules/AA/A0/AAA0_0104" element={<DocumentManagementPage />} />
            <Route path="/modules/AA/A0/AAA0_0105" element={<YoloDetectionPage />} />
            <Route path="/modules/AA/A0/AAA0_0203" element={<NERTrainingPage />} />
            <Route path="/modules/AA/A0/AAA0_0300" element={<LLMInferencePage />} />
            {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
             {/* Label Studio workflow routes */}
            <Route path="/modules/AA/B0/AAB0_0100" element={<ProjectListPage />} />
            <Route path="/modules/AA/B0/AAB0_0100/projects" element={<ProjectListPage />} />
            <Route path="/modules/AA/B0/AAB0_0100/projects/:id/models" element={<ModelSelectPage />} />
            <Route path="/modules/AA/B0/AAB0_0100/projects/:id/labels" element={<LabelConfigPage />} />
            <Route path="/modules/AA/B0/AAB0_0100/projects/:id/annotate" element={<AnnotatePage />} />
            <Route path="/modules/AA/B0/AAB0_0100/projects/:id/export" element={<ExportPage />} />
            {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </TooltipProvider>
    </ThemeInitializer>
  </QueryClientProvider>
);

export default App;
