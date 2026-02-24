import { create } from 'zustand';
import type { OrgChartData, OrgNode } from '@/schemas/orgChart';
import type { OrgFlowNode, OrgFlowEdge } from '@/types/flow';

interface ChartState {
  // Raw data
  orgData: OrgChartData | null;
  
  // Flow data
  nodes: OrgFlowNode[];
  edges: OrgFlowEdge[];
  
  // UI State
  selectedNodeId: string | null;
  isGenerating: boolean;
  error: string | null;
  
  // Actions
  setOrgData: (data: OrgChartData) => void;
  setNodes: (nodes: OrgFlowNode[]) => void;
  setEdges: (edges: OrgFlowEdge[]) => void;
  updateNode: (id: string, data: Partial<OrgFlowNode['data']>) => void;
  addNode: (parentId: string | null, nodeData: Partial<OrgNode>) => void;
  deleteNode: (id: string) => void;
  selectNode: (id: string | null) => void;
  setGenerating: (isGenerating: boolean) => void;
  setError: (error: string | null) => void;
  clearChart: () => void;
}

const generateId = () => `node_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

export const useChartStore = create<ChartState>((set, get) => ({
  orgData: null,
  nodes: [],
  edges: [],
  selectedNodeId: null,
  isGenerating: false,
  error: null,

  setOrgData: (data) => {
    // Convert OrgChartData to Flow nodes and edges
    const nodes: OrgFlowNode[] = [];
    const edges: OrgFlowEdge[] = [];
    
    // Group nodes by level and parent for positioning
    const nodesByParent = new Map<string | null, OrgNode[]>();
    
    data.nodes.forEach(node => {
      const parentId = node.reports_to;
      if (!nodesByParent.has(parentId)) {
        nodesByParent.set(parentId, []);
      }
      nodesByParent.get(parentId)!.push(node);
    });
    
    // Position nodes recursively
    const positionNode = (node: OrgNode, parentX?: number, siblingIndex: number = 0, totalSiblings: number = 1) => {
      const children = nodesByParent.get(node.id) || [];
      
      let x: number;
      let y: number;
      
      if (parentX === undefined) {
        // Root node
        x = 0;
        y = 50;
      } else {
        const spacing = 220;
        const startX = parentX - ((totalSiblings - 1) * spacing) / 2;
        x = startX + siblingIndex * spacing;
        y = node.level * 120 + 50;
      }
      
      nodes.push({
        id: node.id,
        type: 'orgNode',
        position: { x, y },
        data: {
          name: node.name,
          title: node.title,
          department: node.department,
          level: node.level,
          metadata: node.metadata,
          onEdit: () => {},
          onAIAssist: () => {},
          onDelete: () => {},
        },
      });
      
      if (node.reports_to) {
        edges.push({
          id: `e-${node.reports_to}-${node.id}`,
          source: node.reports_to,
          target: node.id,
          type: 'smoothstep',
          animated: false,
        });
      }
      
      // Position children
      children.forEach((child, idx) => {
        positionNode(child, x, idx, children.length);
      });
    };
    
    // Start with root nodes
    const rootNodes = data.nodes.filter(n => n.reports_to === null);
    rootNodes.forEach((root, idx) => {
      positionNode(root, undefined, idx, rootNodes.length);
    });
    
    set({ orgData: data, nodes, edges });
  },

  setNodes: (nodes) => set({ nodes }),
  setEdges: (edges) => set({ edges }),

  updateNode: (id, data) => {
    set((state) => ({
      nodes: state.nodes.map((node) =>
        node.id === id
          ? { ...node, data: { ...node.data, ...data } }
          : node
      ),
    }));
  },

  addNode: (parentId, nodeData) => {
    const newId = generateId();
    const parentNode = get().nodes.find(n => n.id === parentId);
    const level = parentNode ? parentNode.data.level + 1 : 1;
    
    // Calculate position based on siblings
    const siblings = get().nodes.filter(n => {
      const edge = get().edges.find(e => e.target === n.id);
      return edge?.source === parentId;
    });
    
    const siblingIndex = siblings.length;
    const position = parentNode 
      ? { 
          x: parentNode.position.x + (siblingIndex - siblings.length / 2) * 220,
          y: parentNode.position.y + 120
        }
      : { x: 0, y: 50 };
    
    const newNode: OrgFlowNode = {
      id: newId,
      type: 'orgNode',
      position,
      data: {
        name: nodeData.name || 'New Role',
        title: nodeData.title || 'Title',
        department: nodeData.department,
        level,
        metadata: nodeData.metadata,
        onEdit: () => {},
        onAIAssist: () => {},
        onDelete: () => {},
      },
    };
    
    const newEdge: OrgFlowEdge | null = parentId ? {
      id: `e-${parentId}-${newId}`,
      source: parentId,
      target: newId,
      type: 'smoothstep',
    } : null;
    
    set((state) => ({
      nodes: [...state.nodes, newNode],
      edges: newEdge ? [...state.edges, newEdge] : state.edges,
    }));
  },

  deleteNode: (id) => {
    // Recursively delete all children
    const deleteRecursive = (nodeId: string) => {
      const children = get().edges
        .filter(e => e.source === nodeId)
        .map(e => e.target);
      
      children.forEach(childId => deleteRecursive(childId));
      
      set((state) => ({
        nodes: state.nodes.filter(n => n.id !== nodeId),
        edges: state.edges.filter(e => e.source !== nodeId && e.target !== nodeId),
      }));
    };
    
    deleteRecursive(id);
  },

  selectNode: (id) => set({ selectedNodeId: id }),
  setGenerating: (isGenerating) => set({ isGenerating }),
  setError: (error) => set({ error }),
  
  clearChart: () => set({ 
    orgData: null, 
    nodes: [], 
    edges: [], 
    selectedNodeId: null,
    error: null 
  }),
}));
