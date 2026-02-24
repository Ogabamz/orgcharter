import { useCallback, useState } from 'react';
import { Toaster, toast } from 'sonner';
import { 
  Layout, 
  Download, 
  Trash2, 
  Sparkles, 
  FileText,
  Settings,
  HelpCircle,
  Github
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { Separator } from '@/components/ui/separator';
import { useChartStore } from '@/store/chartStore';
import { downloadWordDoc } from '@/services/export';
import FlowCanvas from '@/components/flow/FlowCanvas';
import AIInputPanel from '@/components/AIInputPanel';
import AIModificationPanel from '@/components/AIModificationPanel';
import './App.css';

function App() {
  const [showLeftPanel, setShowLeftPanel] = useState(true);
  const [showRightPanel, setShowRightPanel] = useState(true);
  
  const orgData = useChartStore((state) => state.orgData);
  const clearChart = useChartStore((state) => state.clearChart);
  const isGenerating = useChartStore((state) => state.isGenerating);

  const handleExport = useCallback(async () => {
    if (!orgData) {
      toast.error('No org chart to export');
      return;
    }

    try {
      await downloadWordDoc(orgData);
      toast.success('Org chart exported to Word');
    } catch (error) {
      toast.error('Failed to export org chart');
      console.error(error);
    }
  }, [orgData]);

  const handleClear = useCallback(() => {
    if (confirm('Are you sure you want to clear the current org chart?')) {
      clearChart();
      toast.info('Org chart cleared');
    }
  }, [clearChart]);

  return (
    <TooltipProvider>
      <div className="h-screen flex flex-col bg-slate-50">
        <Toaster position="top-right" richColors />
        
        {/* Header */}
        <header className="h-14 bg-white border-b border-slate-200 flex items-center justify-between px-4 flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 
                          flex items-center justify-center">
              <Layout className="w-4 h-4 text-white" />
            </div>
            <div>
              <h1 className="font-semibold text-slate-900">AI Org Chart Generator</h1>
              <p className="text-xs text-slate-500">Powered by Gemini</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setShowLeftPanel(!showLeftPanel)}
                  className={showLeftPanel ? 'bg-slate-100' : ''}
                >
                  <FileText className="w-4 h-4 mr-2" />
                  Input
                </Button>
              </TooltipTrigger>
              <TooltipContent>Toggle Input Panel</TooltipContent>
            </Tooltip>

            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setShowRightPanel(!showRightPanel)}
                  className={showRightPanel ? 'bg-slate-100' : ''}
                >
                  <Sparkles className="w-4 h-4 mr-2" />
                  AI Modify
                </Button>
              </TooltipTrigger>
              <TooltipContent>Toggle AI Modification Panel</TooltipContent>
            </Tooltip>

            <Separator orientation="vertical" className="h-6 mx-2" />

            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleExport}
                  disabled={!orgData}
                >
                  <Download className="w-4 h-4 mr-2" />
                  Export Word
                </Button>
              </TooltipTrigger>
              <TooltipContent>Export to Word Document</TooltipContent>
            </Tooltip>

            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleClear}
                  disabled={!orgData}
                  className="text-red-600 hover:text-red-700 hover:bg-red-50"
                >
                  <Trash2 className="w-4 h-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Clear Chart</TooltipContent>
            </Tooltip>

            <Dialog>
              <DialogTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <HelpCircle className="w-4 h-4" />
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-lg">
                <DialogHeader>
                  <DialogTitle>About AI Org Chart Generator</DialogTitle>
                </DialogHeader>
                <div className="space-y-4 text-sm text-slate-600">
                  <p>
                    This tool uses Google's Gemini AI to generate and modify organizational 
                    charts from text descriptions or uploaded images.
                  </p>
                  
                  <div>
                    <h4 className="font-medium text-slate-900 mb-2">Features:</h4>
                    <ul className="list-disc list-inside space-y-1">
                      <li>Generate org charts from natural language descriptions</li>
                      <li>Extract org charts from uploaded images</li>
                      <li>Edit nodes by double-clicking or using the edit button</li>
                      <li>Drag and drop to reposition nodes</li>
                      <li>AI-powered modifications with natural language</li>
                      <li>Export to Word document format</li>
                    </ul>
                  </div>

                  <div>
                    <h4 className="font-medium text-slate-900 mb-2">Tips:</h4>
                    <ul className="list-disc list-inside space-y-1">
                      <li>Be specific about reporting relationships</li>
                      <li>Include names and titles for best results</li>
                      <li>Use the AI modification panel for quick changes</li>
                      <li>Select a node before using AI modify to reference it</li>
                    </ul>
                  </div>

                  <div className="pt-4 border-t border-slate-200">
                    <a 
                      href="https://github.com" 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="flex items-center gap-2 text-slate-500 hover:text-slate-900"
                    >
                      <Github className="w-4 h-4" />
                      View on GitHub
                    </a>
                  </div>
                </div>
              </DialogContent>
            </Dialog>
          </div>
        </header>

        {/* Main content */}
        <main className="flex-1 flex overflow-hidden">
          {/* Left panel - AI Input */}
          {showLeftPanel && (
            <div className="w-80 flex-shrink-0 border-r border-slate-200 overflow-hidden">
              <AIInputPanel />
            </div>
          )}

          {/* Center - Flow Canvas */}
          <div className="flex-1 relative">
            <FlowCanvas onExport={handleExport} />
            
            {/* Empty state */}
            {!orgData && !isGenerating && (
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div className="text-center">
                  <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center mx-auto mb-4">
                    <Layout className="w-8 h-8 text-slate-400" />
                  </div>
                  <h3 className="text-lg font-medium text-slate-700 mb-2">
                    No Org Chart Yet
                  </h3>
                  <p className="text-sm text-slate-500 max-w-sm">
                    Use the input panel on the left to generate an org chart from text 
                    or upload an existing image
                  </p>
                </div>
              </div>
            )}

            {/* Generating state */}
            {isGenerating && (
              <div className="absolute inset-0 flex items-center justify-center bg-white/80 backdrop-blur-sm">
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center mx-auto mb-3 animate-pulse">
                    <Sparkles className="w-6 h-6 text-blue-600" />
                  </div>
                  <p className="text-sm font-medium text-slate-700">
                    AI is generating your org chart...
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* Right panel - AI Modification */}
          {showRightPanel && (
            <div className="w-72 flex-shrink-0 border-l border-slate-200 overflow-hidden">
              <AIModificationPanel />
            </div>
          )}
        </main>

        {/* Footer */}
        <footer className="h-8 bg-white border-t border-slate-200 flex items-center justify-between px-4 text-xs text-slate-500">
          <div className="flex items-center gap-4">
            {orgData && (
              <>
                <span>
                  <strong>{orgData.nodes.length}</strong> people
                </span>
                <span>
                  <strong>{Math.max(...orgData.nodes.map(n => n.level))}</strong> levels
                </span>
                <span>
                  <strong>{new Set(orgData.nodes.map(n => n.department).filter(Boolean)).size}</strong> departments
                </span>
              </>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Settings className="w-3 h-3" />
            <span>Powered by Gemini 1.5 Flash</span>
          </div>
        </footer>
      </div>
    </TooltipProvider>
  );
}

export default App;
