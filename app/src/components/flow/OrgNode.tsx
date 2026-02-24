import { useState, useCallback } from 'react';
import { Handle, Position } from '@xyflow/react';
import { Pencil, Sparkles, Trash2, Check, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import type { OrgNodeData } from '@/types/flow';

interface OrgNodeProps {
  id: string;
  data: OrgNodeData;
  selected?: boolean;
}

const OrgNode = ({ id, data, selected }: OrgNodeProps) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    name: data.name,
    title: data.title,
    department: data.department || '',
  });
  const [showSuggestions, setShowSuggestions] = useState(false);

  const handleSave = useCallback(() => {
    data.onEdit(id, {
      name: editForm.name,
      title: editForm.title,
      department: editForm.department || undefined,
    });
    setIsEditing(false);
  }, [id, data, editForm]);

  const handleCancel = useCallback(() => {
    setEditForm({
      name: data.name,
      title: data.title,
      department: data.department || '',
    });
    setIsEditing(false);
  }, [data]);

  const handleAIAssist = useCallback(() => {
    const context = `${data.name} - ${data.title}`;
    data.onAIAssist(id, context);
    setShowSuggestions(true);
  }, [id, data]);

  const handleDelete = useCallback(() => {
    if (confirm('Are you sure you want to delete this node and all its children?')) {
      data.onDelete(id);
    }
  }, [id, data]);

  return (
    <div
      className={`
        relative min-w-[180px] max-w-[240px] bg-white rounded-lg shadow-md
        border-2 transition-all duration-200
        ${selected ? 'border-blue-500 shadow-lg ring-2 ring-blue-200' : 'border-slate-200'}
        hover:shadow-lg hover:border-blue-300
      `}
    >
      {/* Target handle (top) */}
      <Handle
        type="target"
        position={Position.Top}
        className="!w-3 !h-3 !bg-blue-500 !border-2 !border-white"
      />

      {/* Node content */}
      <div className="p-3">
        {isEditing ? (
          <div className="space-y-2">
            <Input
              value={editForm.name}
              onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
              placeholder="Name"
              className="h-7 text-sm font-semibold"
              autoFocus
            />
            <Input
              value={editForm.title}
              onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
              placeholder="Title"
              className="h-7 text-sm"
            />
            <Input
              value={editForm.department}
              onChange={(e) => setEditForm({ ...editForm, department: e.target.value })}
              placeholder="Department (optional)"
              className="h-7 text-sm"
            />
            <div className="flex gap-1 pt-1">
              <Button
                size="sm"
                variant="default"
                className="h-7 px-2"
                onClick={handleSave}
              >
                <Check className="w-3 h-3 mr-1" />
                Save
              </Button>
              <Button
                size="sm"
                variant="ghost"
                className="h-7 px-2"
                onClick={handleCancel}
              >
                <X className="w-3 h-3 mr-1" />
                Cancel
              </Button>
            </div>
          </div>
        ) : (
          <div className="space-y-1">
            <div className="flex items-start justify-between gap-2">
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-slate-900 text-sm truncate">
                  {data.name}
                </h3>
                <p className="text-slate-600 text-xs truncate">{data.title}</p>
                {data.department && (
                  <Badge 
                    variant="secondary" 
                    className="mt-1 text-xs bg-blue-50 text-blue-700 border-blue-200"
                  >
                    {data.department}
                  </Badge>
                )}
              </div>
            </div>

            {/* Action buttons */}
            <div className="flex gap-1 pt-2 mt-2 border-t border-slate-100 opacity-0 group-hover:opacity-100 transition-opacity">
              <Dialog open={showSuggestions} onOpenChange={setShowSuggestions}>
                <DialogTrigger asChild>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="h-6 w-6 p-0 text-amber-500 hover:text-amber-600 hover:bg-amber-50"
                    onClick={handleAIAssist}
                    title="AI Suggestions"
                  >
                    <Sparkles className="w-3 h-3" />
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>AI Suggestions for {data.name}</DialogTitle>
                  </DialogHeader>
                  <div className="space-y-4">
                    <p className="text-sm text-slate-600">
                      Get AI-powered suggestions for team structure and responsibilities
                      for this role.
                    </p>
                    <Button 
                      onClick={handleAIAssist}
                      className="w-full"
                    >
                      <Sparkles className="w-4 h-4 mr-2" />
                      Generate Suggestions
                    </Button>
                  </div>
                </DialogContent>
              </Dialog>

              <Button
                size="sm"
                variant="ghost"
                className="h-6 w-6 p-0 text-slate-500 hover:text-blue-600 hover:bg-blue-50"
                onClick={() => setIsEditing(true)}
                title="Edit"
              >
                <Pencil className="w-3 h-3" />
              </Button>

              <Button
                size="sm"
                variant="ghost"
                className="h-6 w-6 p-0 text-slate-500 hover:text-red-600 hover:bg-red-50"
                onClick={handleDelete}
                title="Delete"
              >
                <Trash2 className="w-3 h-3" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Source handle (bottom) */}
      <Handle
        type="source"
        position={Position.Bottom}
        className="!w-3 !h-3 !bg-blue-500 !border-2 !border-white"
      />
    </div>
  );
};

export default OrgNode;
