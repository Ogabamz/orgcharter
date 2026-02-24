import type { OrgChartData } from '@/schemas/orgChart';
import { OrgChartSchema } from '@/schemas/orgChart';

// Use proxy URL if configured, otherwise fallback to direct API calls
const API_URL = import.meta.env.VITE_API_URL || '/api/gemini/generate';
const USE_PROXY = !import.meta.env.VITE_API_URL;

// Direct Gemini API (only used if no proxy)
const GEMINI_API_KEY = import.meta.env.VITE_GEMINI_API_KEY || '';
const GEMINI_API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent';

const textToOrgPrompt = `You are an expert organizational designer. Convert the following description into a structured organizational chart.

RULES:
- Extract ALL names, titles, and reporting relationships
- Assign hierarchy levels (1 = highest authority)
- If names are missing, use descriptive role titles (e.g., "VP of Engineering")
- Include departments where mentioned
- Handle matrix reporting by creating secondary relationships
- Ensure no orphaned nodes (everyone except root must report to someone)
- Generate unique IDs for each node (e.g., "node_1", "node_2")
- The root node should have reports_to: null

OUTPUT: Valid JSON only, no markdown, no explanations. Must match the provided schema exactly.`;

const imageToOrgPrompt = `Analyze this organizational chart image and extract the complete hierarchy.

INSTRUCTIONS:
- Identify all boxes/nodes and their text content
- Determine reporting lines (who reports to whom)
- Preserve the exact text as shown in the image
- Note colors/shapes if they indicate departments
- Handle overlapping or unclear connections by making reasonable inferences
- Generate unique IDs for each node
- The top-most node should have reports_to: null

Return structured JSON matching the provided schema.`;

const modificationPrompt = `Modify this existing org chart based on the user's request.

MODIFICATION RULES:
- Preserve all unchanged nodes exactly as they are
- Only modify/add/delete what the user specifically requests
- Maintain valid hierarchy (no circular references)
- Reassign IDs if inserting new nodes between existing ones
- Return complete updated chart, not just changes
- Ensure all nodes have valid reports_to references

OUTPUT: Complete valid JSON of the modified org chart matching the schema exactly.`;

const suggestionPrompt = `Based on this role, suggest typical team structure and responsibilities.

Return JSON format:
{
  "directReports": [{"title": "...", "department": "..."}],
  "peers": [{"title": "..."}],
  "responsibilities": ["..."]
}`;

// Helper to make API calls (uses proxy or direct)
const callGeminiAPI = async (body: object): Promise<any> => {
  const url = USE_PROXY ? API_URL : `${GEMINI_API_URL}?key=${GEMINI_API_KEY}`;
  
  if (!USE_PROXY && !GEMINI_API_KEY) {
    throw new Error('Gemini API key not configured. Please add GEMINI_API_KEY to your .env file or set up the proxy server.');
  }

  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`API error: ${error}`);
  }

  return response.json();
};

export const generateOrgChartFromText = async (
  description: string
): Promise<OrgChartData> => {
  const result = await callGeminiAPI({
    contents: [{
      parts: [{ text: `${textToOrgPrompt}\n\nDESCRIPTION:\n${description}` }]
    }],
    generationConfig: {
      temperature: 0.2,
      responseMimeType: 'application/json',
    }
  });

  const text = result.candidates?.[0]?.content?.parts?.[0]?.text;
  
  if (!text) {
    throw new Error('No response from AI');
  }

  try {
    const parsed = JSON.parse(text);
    return OrgChartSchema.parse(parsed);
  } catch (e) {
    throw new Error(`Failed to parse org chart: ${e}`);
  }
};

export const generateOrgChartFromImage = async (
  imageBase64: string,
  mimeType: string
): Promise<OrgChartData> => {
  const result = await callGeminiAPI({
    contents: [{
      parts: [
        { text: imageToOrgPrompt },
        {
          inline_data: {
            mime_type: mimeType,
            data: imageBase64
          }
        }
      ]
    }],
    generationConfig: {
      temperature: 0.2,
      responseMimeType: 'application/json',
    }
  });

  const text = result.candidates?.[0]?.content?.parts?.[0]?.text;
  
  if (!text) {
    throw new Error('No response from AI');
  }

  try {
    const parsed = JSON.parse(text);
    return OrgChartSchema.parse(parsed);
  } catch (e) {
    throw new Error(`Failed to parse org chart: ${e}`);
  }
};

export const modifyOrgChart = async (
  currentChart: OrgChartData,
  modificationRequest: string
): Promise<OrgChartData> => {
  const result = await callGeminiAPI({
    contents: [{
      parts: [{
        text: `${modificationPrompt}\n\nCURRENT CHART:\n${JSON.stringify(currentChart, null, 2)}\n\nUSER REQUEST:\n${modificationRequest}`
      }]
    }],
    generationConfig: {
      temperature: 0.3,
      responseMimeType: 'application/json',
    }
  });

  const text = result.candidates?.[0]?.content?.parts?.[0]?.text;
  
  if (!text) {
    throw new Error('No response from AI');
  }

  try {
    const parsed = JSON.parse(text);
    return OrgChartSchema.parse(parsed);
  } catch (e) {
    throw new Error(`Failed to parse modified org chart: ${e}`);
  }
};

export const getNodeSuggestions = async (
  roleContext: string
): Promise<{
  directReports: { title: string; department?: string }[];
  peers: { title: string }[];
  responsibilities: string[];
}> => {
  const result = await callGeminiAPI({
    contents: [{
      parts: [{
        text: `${suggestionPrompt}\n\nROLE CONTEXT: ${roleContext}`
      }]
    }],
    generationConfig: {
      temperature: 0.5,
      responseMimeType: 'application/json',
    }
  });

  const text = result.candidates?.[0]?.content?.parts?.[0]?.text;
  
  if (!text) {
    throw new Error('No response from AI');
  }

  try {
    return JSON.parse(text);
  } catch (e) {
    throw new Error(`Failed to parse suggestions: ${e}`);
  }
};
