import type { Node, Edge } from '@xyflow/react';

export interface OrgNodeData extends Record<string, unknown> {
  name: string;
  title: string;
  department?: string;
  level: number;
  metadata?: {
    email?: string;
    location?: string;
    headcount?: number;
  };
  onEdit: (id: string, newData: Partial<OrgNodeData>) => void;
  onAIAssist: (id: string, context: string) => void;
  onDelete: (id: string) => void;
}

export type OrgFlowNode = Node<OrgNodeData>;
export type OrgFlowEdge = Edge;

export interface FlowState {
  nodes: OrgFlowNode[];
  edges: OrgFlowEdge[];
  selectedNode: string | null;
}
