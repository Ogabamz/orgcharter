import { z } from 'zod';

export const OrgNodeSchema = z.object({
  id: z.string().describe("Unique identifier (e.g., 'node_1')"),
  name: z.string().describe("Person's full name or role title"),
  title: z.string().describe("Job title"),
  department: z.string().optional().describe("Department name"),
  level: z.number().describe("Hierarchy level (1 = CEO/top)"),
  reports_to: z.string().nullable().describe("ID of parent node, null for root"),
  metadata: z.object({
    email: z.string().optional(),
    location: z.string().optional(),
    headcount: z.number().optional().describe("For department nodes")
  }).optional()
});

export const OrgChartSchema = z.object({
  organization: z.string().describe("Company or unit name"),
  nodes: z.array(OrgNodeSchema),
  created_date: z.string().optional(),
  version: z.string().default("1.0")
});

export type OrgNode = z.infer<typeof OrgNodeSchema>;
export type OrgChartData = z.infer<typeof OrgChartSchema>;
