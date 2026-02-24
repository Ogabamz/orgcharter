import { useCallback, useEffect } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  type Connection,
  type Node,
  type Edge,
  Panel,
  useReactFlow,
  ReactFlowProvider,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { Plus, ZoomIn, ZoomOut, Maximize, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useChartStore } from '@/store/chartStore';
import OrgNode from './OrgNode';
import type { OrgNodeData } from '@/types/flow';

const nodeTypes = {
  orgNode: OrgNode,
};

interface FlowCanvasInnerProps {
  onExport: () => void;
}

const FlowCanvasInner = ({ onExport }: FlowCanvasInnerProps) => {
  const { fitView, zoomIn, zoomOut } = useReactFlow();
  const storeNodes = useChartStore((state) => state.nodes);
  const storeEdges = useChartStore((state) => state.edges);
  const selectedNodeId = useChartStore((state) => state.selectedNodeId);
  const updateNode = useChartStore((state) => state.updateNode);
  const addNode = useChartStore((state) => state.addNode);
  const deleteNode = useChartStore((state) => state.deleteNode);
  const selectNode = useChartStore((state) => state.selectNode);

  const [nodes, setLocalNodes, onNodesChange] = useNodesState<Node<OrgNodeData>>([]);
  const [edges, setLocalEdges, onEdgesChange] = useEdgesState<Edge>([]);

  // Sync store state with React Flow state
  useEffect(() => {
    // Update node callbacks
    const nodesWithCallbacks = storeNodes.map((node) => ({
      ...node,
      data: {
        ...node.data,
        onEdit: (id: string, data: Partial<OrgNodeData>) => {
          updateNode(id, data);
        },
        onAIAssist: (id: string, context: string) => {
          console.log('AI Assist:', id, context);
        },
        onDelete: (id: string) => {
          deleteNode(id);
        },
      },
    }));
    setLocalNodes(nodesWithCallbacks);
  }, [storeNodes, updateNode, deleteNode, setLocalNodes]);

  useEffect(() => {
    setLocalEdges(storeEdges);
  }, [storeEdges, setLocalEdges]);

  const onConnect = useCallback(
    (connection: Connection) => {
      const newEdge: Edge = {
        id: `e-${connection.source}-${connection.target}`,
        source: connection.source!,
        target: connection.target!,
        type: 'smoothstep',
      };
      setLocalEdges((eds) => addEdge(newEdge, eds));
    },
    [setLocalEdges]
  );

  const onNodeClick = useCallback(
    (_: React.MouseEvent, node: Node<OrgNodeData>) => {
      selectNode(node.id);
    },
    [selectNode]
  );

  const onPaneClick = useCallback(() => {
    selectNode(null);
  }, [selectNode]);

  const handleAddNode = useCallback(() => {
    const parentId = selectedNodeId;
    addNode(parentId, {
      name: 'New Role',
      title: 'Title',
    });
  }, [selectedNodeId, addNode]);

  const handleFitView = useCallback(() => {
    fitView({ padding: 0.2, duration: 800 });
  }, [fitView]);

  return (
    <div className="w-full h-full relative">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeClick={onNodeClick}
        onPaneClick={onPaneClick}
        nodeTypes={nodeTypes}
        fitView
        attributionPosition="bottom-left"
        minZoom={0.1}
        maxZoom={2}
        defaultEdgeOptions={{
          type: 'smoothstep',
          animated: false,
          style: { stroke: '#94a3b8', strokeWidth: 2 },
        }}
      >
        <Background color="#cbd5e1" gap={20} size={1} />
        <Controls className="!bg-white !border-slate-200 !shadow-md" />
        <MiniMap
          className="!bg-white !border-slate-200 !shadow-md"
          nodeColor={(node) => {
            return node.data?.department ? '#3b82f6' : '#64748b';
          }}
          maskColor="rgba(203, 213, 225, 0.5)"
        />

        {/* Custom toolbar panel */}
        <Panel position="top-right" className="!m-4">
          <div className="flex flex-col gap-2 bg-white p-2 rounded-lg shadow-md border border-slate-200">
            <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0"
              onClick={() => zoomIn()}
              title="Zoom In"
            >
              <ZoomIn className="w-4 h-4" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0"
              onClick={() => zoomOut()}
              title="Zoom Out"
            >
              <ZoomOut className="w-4 h-4" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0"
              onClick={handleFitView}
              title="Fit View"
            >
              <Maximize className="w-4 h-4" />
            </Button>
          </div>
        </Panel>

        {/* Action panel */}
        <Panel position="bottom-center" className="!m-4">
          <div className="flex gap-2 bg-white p-2 rounded-lg shadow-md border border-slate-200">
            <Button
              variant="default"
              size="sm"
              onClick={handleAddNode}
              className="gap-2"
            >
              <Plus className="w-4 h-4" />
              Add Node
              {selectedNodeId && (
                <span className="text-xs opacity-75">(under selected)</span>
              )}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={onExport}
              className="gap-2"
            >
              <Download className="w-4 h-4" />
              Export Word
            </Button>
          </div>
        </Panel>
      </ReactFlow>
    </div>
  );
};

interface FlowCanvasProps {
  onExport: () => void;
}

const FlowCanvas = ({ onExport }: FlowCanvasProps) => {
  return (
    <ReactFlowProvider>
      <FlowCanvasInner onExport={onExport} />
    </ReactFlowProvider>
  );
};

export default FlowCanvas;
