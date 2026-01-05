import { DashboardLayout } from '@/components/DashboardLayout';
import { Brain, Send, Loader2, Download, Settings, Zap, MessageSquare, BarChart3, Clock, Cpu, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Textarea } from '@/components/ui/textarea';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Slider } from '@/components/ui/slider';
import { cn } from '@/lib/utils';
import { useState, useEffect, useRef } from 'react';
import { useToast } from '@/hooks/use-toast';
import { StatsCard } from '@/components/StatsCard';

interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
  tokens?: number;
  inferenceTime?: number;
}

interface InferenceStats {
  totalInferences: number;
  avgInferenceTime: number;
  totalTokens: number;
  avgTokensPerRequest: number;
}

const LLMInferencePage = () => {
  const [prompt, setPrompt] = useState('');
  const [chatHistory, setChatHistory] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState<InferenceStats>({
    totalInferences: 0,
    avgInferenceTime: 0,
    totalTokens: 0,
    avgTokensPerRequest: 0
  });
  
  // Configuration
  const [workerType, setWorkerType] = useState<'python' | 'cpp'>('python');
  const [temperature, setTemperature] = useState(0.1);
  const [maxTokens, setMaxTokens] = useState(200);
  const [modelName, setModelName] = useState('vinallama-7b-chat');
  
  const { toast } = useToast();
  const chatEndRef = useRef<HTMLDivElement>(null);

  // Auto scroll to bottom
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatHistory]);

  // Load stats
  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const response = await fetch('/AA/A0/AAA0_0300/stats');
      if (response.ok) {
        const data = await response.json();
        setStats(data);
      }
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  const handleSendMessage = async () => {
    if (!prompt.trim()) return;

    const userMessage: ChatMessage = {
      role: 'user',
      content: prompt.trim(),
      timestamp: new Date()
    };

    setChatHistory(prev => [...prev, userMessage]);
    setLoading(true);
    setPrompt('');

    const startTime = Date.now();

    try {
      const response = await fetch('/AA/A0/AAA0_0300/inference', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prompt: userMessage.content,
          workerType,
          temperature,
          maxTokens,
          modelName,
          chatHistory: chatHistory.map(msg => ({
            role: msg.role,
            content: msg.content
          }))
        })
      });

      const result = await response.json();
      const inferenceTime = Date.now() - startTime;

      if (response.ok && result.status === 'success') {
        const assistantMessage: ChatMessage = {
          role: 'assistant',
          content: result.response,
          timestamp: new Date(),
          tokens: result.tokens,
          inferenceTime: inferenceTime
        };

        setChatHistory(prev => [...prev, assistantMessage]);
        
        // Update stats
        await loadStats();

        toast({
          title: "Inference Complete",
          description: `Generated in ${(inferenceTime / 1000).toFixed(2)}s using ${workerType.toUpperCase()} worker`,
        });
      } else {
        throw new Error(result.message || 'Inference failed');
      }
    } catch (error: any) {
      toast({
        title: "Inference Failed",
        description: error.message,
        variant: "destructive",
      });

      // Add error message to chat
      setChatHistory(prev => [...prev, {
        role: 'assistant',
        content: `Error: ${error.message}`,
        timestamp: new Date()
      }]);
    } finally {
      setLoading(false);
    }
  };

  const clearChat = () => {
    setChatHistory([]);
    toast({
      title: "Chat Cleared",
      description: "Chat history has been cleared",
    });
  };

  const exportChat = () => {
    const chatData = JSON.stringify(chatHistory, null, 2);
    const blob = new Blob([chatData], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `llm-chat-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
            <Brain className="h-8 w-8 text-purple-500" />
            AAA0_0300 - LLM Inference
          </h1>
          <p className="text-muted-foreground mt-2">
            Vietnamese Language Model Inference with llama.cpp
          </p>
        </div>

        {/* Stats */}
        <div className="grid gap-4 md:grid-cols-4">
          <StatsCard
            title="Total Inferences"
            value={stats.totalInferences}
            icon={MessageSquare}
            trend={0}
          />
          <StatsCard
            title="Avg Time"
            value={`${stats.avgInferenceTime.toFixed(2)}s`}
            icon={Clock}
            trend={0}
          />
          <StatsCard
            title="Total Tokens"
            value={stats.totalTokens}
            icon={Zap}
            trend={0}
          />
          <StatsCard
            title="Avg Tokens/Request"
            value={stats.avgTokensPerRequest.toFixed(0)}
            icon={BarChart3}
            trend={0}
          />
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Configuration Panel */}
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Settings className="h-5 w-5" />
                Configuration
              </CardTitle>
              <CardDescription>
                Configure LLM inference parameters
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label>Worker Type</Label>
                <Select value={workerType} onValueChange={(v: any) => setWorkerType(v)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="python">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">Python</Badge>
                        <span className="text-sm">llama-cpp-python</span>
                      </div>
                    </SelectItem>
                    <SelectItem value="cpp">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">C++</Badge>
                        <span className="text-sm">llama.cpp native</span>
                      </div>
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Model</Label>
                <Select value={modelName} onValueChange={setModelName}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="vinallama-7b-chat">VinAllama 7B Chat</SelectItem>
                    <SelectItem value="vietcuna-7b">VietCuna 7B</SelectItem>
                    <SelectItem value="phobert-base">PhoBERT Base</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Temperature: {temperature}</Label>
                <Slider
                  value={[temperature]}
                  onValueChange={(v) => setTemperature(v[0])}
                  min={0}
                  max={1}
                  step={0.1}
                />
                <p className="text-xs text-muted-foreground">
                  Lower = more focused, Higher = more creative
                </p>
              </div>

              <div className="space-y-2">
                <Label>Max Tokens</Label>
                <Input
                  type="number"
                  value={maxTokens}
                  onChange={(e) => setMaxTokens(Number(e.target.value))}
                  min={50}
                  max={2048}
                />
              </div>

              <Separator />

              <div className="space-y-2">
                <Badge variant="secondary" className="w-full justify-center">
                  <Cpu className="h-3 w-3 mr-1" />
                  {workerType.toUpperCase()} Worker
                </Badge>
              </div>
            </CardContent>
          </Card>

          {/* Chat Panel */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <MessageSquare className="h-5 w-5" />
                    Conversation
                  </CardTitle>
                  <CardDescription>
                    Chat with Vietnamese LLM
                  </CardDescription>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" onClick={exportChat} disabled={chatHistory.length === 0}>
                    <Download className="h-4 w-4 mr-1" />
                    Export
                  </Button>
                  <Button variant="outline" size="sm" onClick={clearChat} disabled={chatHistory.length === 0}>
                    <Trash2 className="h-4 w-4 mr-1" />
                    Clear
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Chat History */}
              <div className="h-[500px] overflow-y-auto space-y-4 p-4 bg-muted/50 rounded-lg">
                {chatHistory.length === 0 ? (
                  <div className="h-full flex items-center justify-center text-muted-foreground">
                    <div className="text-center">
                      <Brain className="h-12 w-12 mx-auto mb-2 opacity-50" />
                      <p>No messages yet. Start a conversation!</p>
                    </div>
                  </div>
                ) : (
                  <>
                    {chatHistory.map((message, idx) => (
                      <div
                        key={idx}
                        className={cn(
                          "flex gap-3",
                          message.role === 'user' ? "justify-end" : "justify-start"
                        )}
                      >
                        <div
                          className={cn(
                            "max-w-[80%] rounded-lg p-3",
                            message.role === 'user'
                              ? "bg-primary text-primary-foreground"
                              : "bg-card border"
                          )}
                        >
                          <div className="text-sm font-medium mb-1">
                            {message.role === 'user' ? 'You' : 'Assistant'}
                          </div>
                          <div className="text-sm whitespace-pre-wrap">{message.content}</div>
                          {message.inferenceTime && (
                            <div className="text-xs mt-2 opacity-70 flex items-center gap-2">
                              <Clock className="h-3 w-3" />
                              {(message.inferenceTime / 1000).toFixed(2)}s
                              {message.tokens && (
                                <>
                                  <Zap className="h-3 w-3 ml-2" />
                                  {message.tokens} tokens
                                </>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                    <div ref={chatEndRef} />
                  </>
                )}
              </div>

              {/* Input Area */}
              <div className="space-y-2">
                <Textarea
                  placeholder="Tên người bệnh? chỉ cần trả về họ tên đầy đủ - Họ tên người bệnh: nguyễn quốc tỉnh&#10;Ngày sinh: 20/11/1978"
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSendMessage();
                    }
                  }}
                  rows={4}
                  disabled={loading}
                  className="resize-none"
                />
                <div className="flex justify-between items-center">
                  <p className="text-xs text-muted-foreground">
                    Press Enter to send, Shift+Enter for new line
                  </p>
                  <Button
                    onClick={handleSendMessage}
                    disabled={loading || !prompt.trim()}
                  >
                    {loading ? (
                      <>
                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        Processing...
                      </>
                    ) : (
                      <>
                        <Send className="h-4 w-4 mr-2" />
                        Send
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Quick Templates */}
        <Card>
          <CardHeader>
            <CardTitle>Quick Templates</CardTitle>
            <CardDescription>
              Pre-configured prompts for common medical tasks
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-4">
              <Button
                variant="outline"
                className="justify-start h-auto p-4"
                onClick={() => setPrompt('Tên người bệnh? chỉ cần trả về họ tên đầy đủ - Họ tên người bệnh: nguyễn quốc tỉnh\nNgày sinh: 20/11/1978')}
              >
                <div className="text-left">
                  <div className="font-medium">Extract Patient Name</div>
                  <div className="text-xs text-muted-foreground">Trích xuất tên bệnh nhân</div>
                </div>
              </Button>
              <Button
                variant="outline"
                className="justify-start h-auto p-4"
                onClick={() => setPrompt('Chẩn đoán của bệnh nhân là gì? - Chẩn đoán: Viêm phổi cấp\nĐiều trị: Kháng sinh, nghỉ ngơi')}
              >
                <div className="text-left">
                  <div className="font-medium">Extract Diagnosis</div>
                  <div className="text-xs text-muted-foreground">Trích xuất chẩn đoán</div>
                </div>
              </Button>
              <Button
                variant="outline"
                className="justify-start h-auto p-4"
                onClick={() => setPrompt('Tóm tắt thông tin bệnh nhân:\nHọ tên: Nguyễn Văn A\nNgày sinh: 01/01/1980\nChẩn đoán: Tiểu đường type 2\nĐiều trị: Metformin 500mg')}
              >
                <div className="text-left">
                  <div className="font-medium">Summarize</div>
                  <div className="text-xs text-muted-foreground">Tóm tắt thông tin</div>
                </div>
              </Button>
              <Button
                variant="outline"
                className="justify-start h-auto p-4"
                onClick={() => setPrompt('Dịch sang tiếng Anh: Bệnh nhân được chỉ định nhập viện điều trị')}
              >
                <div className="text-left">
                  <div className="font-medium">Translate</div>
                  <div className="text-xs text-muted-foreground">Dịch thuật</div>
                </div>
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default LLMInferencePage;
