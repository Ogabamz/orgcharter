import { useState, useCallback } from 'react';
import { Wand2, Loader2, MessageSquare, History, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useChartStore } from '@/store/chartStore';
import { modifyOrgChart } from '@/services/gemini';

interface ModificationHistory {
  id: string;
  request: string;
  timestamp: Date;
}

const AIModificationPanel = () => {
  const [modificationRequest, setModificationRequest] = useState('');
  const [isModifying, setIsModifying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [history, setHistory] = useState<ModificationHistory[]>([]);
  
  const orgData = useChartStore((state) => state.orgData);
  const setOrgData = useChartStore((state) => state.setOrgData);
  const selectedNodeId = useChartStore((state) => state.selectedNodeId);
  const nodes = useChartStore((state) => state.nodes);

  const handleModify = useCallback(async () => {
    if (!modificationRequest.trim() || !orgData) {
      setError('Please enter a modification request');
      return;
    }

    setIsModifying(true);
    setError(null);

    try {
      // Enhance request with selected node context if available
      let enhancedRequest = modificationRequest;
      if (selectedNodeId) {
        const selectedNode = nodes.find(n => n.id === selectedNodeId);
        if (selectedNode) {
          enhancedRequest = `Regarding "${selectedNode.data.name}" (${selectedNode.data.title}): ${modificationRequest}`;
        }
      }

      const modifiedChart = await modifyOrgChart(orgData, enhancedRequest);
      setOrgData(modifiedChart);
      
      // Add to history
      setHistory(prev => [{
        id: Date.now().toString(),
        request: modificationRequest,
        timestamp: new Date(),
      }, ...prev]);
      
      setModificationRequest('');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to modify org chart';
      setError(message);
    } finally {
      setIsModifying(false);
    }
  }, [modificationRequest, orgData, selectedNodeId, nodes, setOrgData]);

  const exampleModifications = [
    'Add 3 senior engineers under the CTO',
    'Create a new Marketing department with 5 people',
    'Remove all contractors and temporary staff',
    'Merge the Sales and Marketing teams under one VP',
    'Add a new COO role between CEO and VPs',
    'Create regional teams under each department head',
  ];

  const selectedNode = selectedNodeId ? nodes.find(n => n.id === selectedNodeId) : null;

  if (!orgData) {
    return (
      <div className="h-full flex flex-col items-center justify-center p-6 text-center">
        <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mb-3">
          <Wand2 className="w-6 h-6 text-slate-400" />
        </div>
        <p className="text-sm text-slate-500">
          Generate an org chart first to use AI modification features
        </p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-white">
      <div className="p-4 border-b border-slate-200">
        <h2 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
          <Wand2 className="w-5 h-5 text-purple-500" />
          AI Modification
        </h2>
        <p className="text-sm text-slate-500 mt-1">
          Ask AI to modify your org chart
        </p>
      </div>

      <div className="flex-1 flex flex-col p-4 gap-4 overflow-hidden">
        {/* Selected node indicator */}
        {selectedNode && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-xs text-blue-600 font-medium mb-1">Selected Node:</p>
            <div className="flex items-center gap-2">
              <span className="font-medium text-slate-900">{selectedNode.data.name}</span>
              <Badge variant="secondary" className="text-xs">
                {selectedNode.data.title}
              </Badge>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Your request will reference this node
            </p>
          </div>
        )}

        {/* Modification input */}
        <div className="flex gap-2">
          <Input
            placeholder="e.g., Add 3 engineers under CTO..."
            value={modificationRequest}
            onChange={(e) => setModificationRequest(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleModify()}
            disabled={isModifying}
            className="flex-1"
          />
          <Button
            onClick={handleModify}
            disabled={isModifying || !modificationRequest.trim()}
            className="gap-2"
          >
            {isModifying ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Wand2 className="w-4 h-4" />
            )}
            Go
          </Button>
        </div>

        {/* Example modifications */}
        <div className="space-y-2">
          <p className="text-xs text-slate-500 font-medium">Try asking:</p>
          <div className="flex flex-wrap gap-2">
            {exampleModifications.map((example, idx) => (
              <button
                key={idx}
                onClick={() => setModificationRequest(example)}
                className="text-xs bg-slate-100 hover:bg-blue-100 hover:text-blue-700
                         text-slate-600 px-2 py-1 rounded-full transition-colors"
                disabled={isModifying}
              >
                {example.length > 35 ? example.slice(0, 35) + '...' : example}
              </button>
            ))}
          </div>
        </div>

        {/* History */}
        {history.length > 0 && (
          <div className="flex-1 flex flex-col min-h-0">
            <div className="flex items-center justify-between mb-2">
              <p className="text-xs text-slate-500 font-medium flex items-center gap-1">
                <History className="w-3 h-3" />
                Recent Changes
              </p>
              <button
                onClick={() => setHistory([])}
                className="text-xs text-slate-400 hover:text-red-500 flex items-center gap-1"
              >
                <RotateCcw className="w-3 h-3" />
                Clear
              </button>
            </div>
            <ScrollArea className="flex-1">
              <div className="space-y-2 pr-3">
                {history.map((item) => (
                  <div
                    key={item.id}
                    className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-sm"
                  >
                    <div className="flex items-start gap-2">
                      <MessageSquare className="w-3 h-3 text-slate-400 mt-0.5 flex-shrink-0" />
                      <div className="flex-1 min-w-0">
                        <p className="text-slate-700 truncate">{item.request}</p>
                        <p className="text-xs text-slate-400 mt-1">
                          {item.timestamp.toLocaleTimeString()}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </ScrollArea>
          </div>
        )}

        {/* Stats */}
        <div className="bg-slate-50 rounded-lg p-3 space-y-2">
          <p className="text-xs text-slate-500 font-medium">Current Organization</p>
          <div className="grid grid-cols-2 gap-2 text-sm">
            <div>
              <span className="text-slate-500">Total:</span>{' '}
              <span className="font-medium">{orgData.nodes.length}</span>
            </div>
            <div>
              <span className="text-slate-500">Levels:</span>{' '}
              <span className="font-medium">
                {Math.max(...orgData.nodes.map(n => n.level))}
              </span>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="p-4">
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </div>
      )}
    </div>
  );
};

export default AIModificationPanel;
