import { 
  Document, 
  Packer, 
  Paragraph, 
  TextRun, 
  Table, 
  TableCell, 
  TableRow, 
  WidthType, 
  AlignmentType, 
  HeadingLevel,
  ShadingType,
} from 'docx';
import type { OrgChartData, OrgNode } from '@/schemas/orgChart';

const createIndentedHierarchy = (nodes: OrgNode[]): Paragraph[] => {
  const paragraphs: Paragraph[] = [];
  
  // Build parent-child map
  const childrenMap = new Map<string | null, OrgNode[]>();
  nodes.forEach(node => {
    const parentId = node.reports_to;
    if (!childrenMap.has(parentId)) {
      childrenMap.set(parentId, []);
    }
    childrenMap.get(parentId)!.push(node);
  });
  
  // Recursive function to build hierarchy
  const buildHierarchy = (parentId: string | null, level: number) => {
    const children = childrenMap.get(parentId) || [];
    
    children.forEach(child => {
      const indent = level * 400;
      const prefix = level > 0 ? '└── ' : '';
      
      paragraphs.push(
        new Paragraph({
          indent: { left: indent },
          spacing: { before: 100, after: 100 },
          children: [
            new TextRun({
              text: prefix + child.name,
              bold: true,
              size: 24 - (level * 2),
            }),
            new TextRun({
              text: ` — ${child.title}`,
              size: 22 - (level * 2),
              color: '#64748b',
            }),
          ],
        })
      );
      
      // Recursively add children
      buildHierarchy(child.id, level + 1);
    });
  };
  
  buildHierarchy(null, 0);
  
  return paragraphs;
};

const createRoleSummaryTable = (nodes: OrgNode[]): Table => {
  const rows: TableRow[] = [
    // Header row
    new TableRow({
      children: [
        new TableCell({
          children: [new Paragraph({
            children: [new TextRun({ text: 'Name', bold: true, size: 22 })],
          })],
          shading: { type: ShadingType.SOLID, fill: '#2563eb' },
        }),
        new TableCell({
          children: [new Paragraph({
            children: [new TextRun({ text: 'Title', bold: true, size: 22 })],
          })],
          shading: { type: ShadingType.SOLID, fill: '#2563eb' },
        }),
        new TableCell({
          children: [new Paragraph({
            children: [new TextRun({ text: 'Department', bold: true, size: 22 })],
          })],
          shading: { type: ShadingType.SOLID, fill: '#2563eb' },
        }),
        new TableCell({
          children: [new Paragraph({
            children: [new TextRun({ text: 'Level', bold: true, size: 22 })],
          })],
          shading: { type: ShadingType.SOLID, fill: '#2563eb' },
        }),
        new TableCell({
          children: [new Paragraph({
            children: [new TextRun({ text: 'Reports To', bold: true, size: 22 })],
          })],
          shading: { type: ShadingType.SOLID, fill: '#2563eb' },
        }),
      ],
    }),
  ];
  
  // Data rows
  const sortedNodes = [...nodes].sort((a, b) => a.level - b.level);
  
  sortedNodes.forEach(node => {
    const reportsToNode = nodes.find(n => n.id === node.reports_to);
    
    rows.push(
      new TableRow({
        children: [
          new TableCell({
            children: [new Paragraph(node.name)],
          }),
          new TableCell({
            children: [new Paragraph(node.title)],
          }),
          new TableCell({
            children: [new Paragraph(node.department || '-')],
          }),
          new TableCell({
            children: [new Paragraph(node.level.toString())],
          }),
          new TableCell({
            children: [new Paragraph(reportsToNode?.name || 'N/A')],
          }),
        ],
      })
    );
  });
  
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows,
  });
};

export const exportToWord = async (orgChart: OrgChartData): Promise<Blob> => {
  const doc = new Document({
    sections: [{
      properties: {},
      children: [
        // Title
        new Paragraph({
          text: orgChart.organization,
          heading: HeadingLevel.TITLE,
          alignment: AlignmentType.CENTER,
          spacing: { after: 400 },
        }),
        
        // Date
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { after: 600 },
          children: [
            new TextRun({
              text: `Generated on: ${new Date().toLocaleDateString()}`,
              size: 20,
              color: '#64748b',
              italics: true,
            }),
          ],
        }),
        
        // Section 1: Visual Hierarchy (Table format)
        new Paragraph({
          text: 'Organizational Hierarchy',
          heading: HeadingLevel.HEADING_1,
          spacing: { before: 400, after: 200 },
        }),
        
        ...createIndentedHierarchy(orgChart.nodes),
        
        new Paragraph({ text: '', spacing: { after: 400 } }),
        
        // Section 2: Role Summary Table
        new Paragraph({
          text: 'Role Summary',
          heading: HeadingLevel.HEADING_1,
          spacing: { before: 400, after: 200 },
        }),
        
        createRoleSummaryTable(orgChart.nodes),
        
        new Paragraph({ text: '', spacing: { after: 400 } }),
        
        // Section 3: Statistics
        new Paragraph({
          text: 'Organization Statistics',
          heading: HeadingLevel.HEADING_1,
          spacing: { before: 400, after: 200 },
        }),
        
        new Paragraph({
          children: [
            new TextRun({ text: 'Total Employees: ', bold: true }),
            new TextRun({ text: orgChart.nodes.length.toString() }),
          ],
          spacing: { after: 100 },
        }),
        
        new Paragraph({
          children: [
            new TextRun({ text: 'Organization Levels: ', bold: true }),
            new TextRun({ 
              text: Math.max(...orgChart.nodes.map(n => n.level)).toString() 
            }),
          ],
          spacing: { after: 100 },
        }),
        
        new Paragraph({
          children: [
            new TextRun({ text: 'Departments: ', bold: true }),
            new TextRun({ 
              text: new Set(orgChart.nodes.map(n => n.department).filter(Boolean)).size.toString() 
            }),
          ],
        }),
      ],
    }],
  });

  return await Packer.toBlob(doc);
};

export const downloadWordDoc = async (orgChart: OrgChartData, filename?: string) => {
  const blob = await exportToWord(orgChart);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || `${orgChart.organization.replace(/\s+/g, '_')}_OrgChart.docx`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};
