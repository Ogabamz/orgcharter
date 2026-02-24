# AI Org Chart Generator

A powerful web application that uses Google's Gemini AI to generate organizational charts from text descriptions or uploaded images. Features an interactive visual editor with drag-and-drop, inline editing, and Word export.

![App Screenshot](https://via.placeholder.com/800x400?text=AI+Org+Chart+Generator)

## 🚀 Quick Start

### Step 1: Get Your Gemini API Key (FREE)

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy your new API key

### Step 2: Add Your API Key

**Option A: Using the Proxy Server (Recommended - Secure)**

Edit the `.env` file in the project root:

```bash
GEMINI_API_KEY=your_actual_api_key_here
```

Then start both the backend and frontend:
```bash
npm run server  # Terminal 1 - starts proxy server
npm run dev     # Terminal 2 - starts frontend
```

**Option B: Direct Frontend (Less Secure - Key Visible in Browser)**

Edit the `.env` file:

```bash
VITE_GEMINI_API_KEY=your_actual_api_key_here
```

Then start only the frontend:
```bash
npm run dev
```

## ✨ Features

### AI-Powered Generation
- **Text to Org Chart**: Describe your organization in natural language
- **Image to Org Chart**: Upload existing org chart images for AI parsing

### Interactive Editor
- Drag-and-drop node positioning
- Double-click to edit node details
- Add/delete nodes with toolbar
- Zoom, pan, and minimap navigation

### AI Modification
- Natural language editing (e.g., "Add 3 engineers under CTO")
- Context-aware suggestions
- Modification history

### Export
- One-click Word document export
- Includes hierarchy, role summary table, and statistics

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| AI Engine | Google Gemini 1.5 Flash |
| Frontend | React 18 + TypeScript + Vite |
| Diagram | React Flow (@xyflow/react) |
| State | Zustand |
| Validation | Zod |
| Export | docx library |
| Styling | Tailwind CSS + shadcn/ui |

## 📁 Project Structure

```
├── api/                    # Backend proxy server
│   └── gemini-proxy.js     # Secure API key handling
├── src/
│   ├── components/         # React components
│   │   ├── flow/          # React Flow components
│   │   ├── AIInputPanel.tsx
│   │   └── AIModificationPanel.tsx
│   ├── services/          # API services
│   │   ├── gemini.ts      # Gemini API integration
│   │   └── export.ts      # Word export
│   ├── store/             # Zustand state management
│   ├── schemas/           # Zod validation schemas
│   └── types/             # TypeScript types
├── .env                   # Environment variables
└── package.json
```

## 🔒 Security Notes

- **Recommended**: Use the proxy server (`npm run server`) to keep your API key secure on the backend
- **Alternative**: Use direct frontend API calls (key visible in browser network tab)
- Never commit your `.env` file with real API keys to version control

## 📝 Example Prompts

Try these prompts in the text input:

```
"Our company has a CEO named John Smith. Under him are 3 VPs: 
Sarah (Engineering), Mike (Sales), and Lisa (Marketing). 
Sarah manages 5 engineers."
```

```
"Small startup: Founder/CEO Alex, CTO Jordan with 3 developers, 
CMO Taylor with 2 marketers."
```

## 🖼️ Image Upload Tips

- Use clear, high-resolution org chart images
- Ensure text is readable
- Supported formats: PNG, JPG, GIF
- Max file size: 5MB

## 📄 Word Export

The exported Word document includes:
1. **Organizational Hierarchy** - Visual tree structure
2. **Role Summary Table** - Complete listing with departments
3. **Statistics** - Total employees, levels, departments

## 🐛 Troubleshooting

### "API key not configured" error
- Make sure you've added your key to `.env`
- If using proxy, restart the server after editing `.env`

### "Failed to parse org chart" error
- Try being more specific in your description
- Include clear reporting relationships
- Use names and titles

### Images not parsing correctly
- Ensure the image is clear and text is readable
- Try cropping to just the org chart area
- Use higher resolution images

## 📚 Learn More

- [Gemini API Documentation](https://ai.google.dev/docs)
- [React Flow Documentation](https://reactflow.dev)
- [shadcn/ui Components](https://ui.shadcn.com)

## 📄 License

MIT License - feel free to use and modify!
