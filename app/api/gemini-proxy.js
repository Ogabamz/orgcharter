const express = require('express');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const GEMINI_API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent';

// Health check
app.get('/api/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    apiKeyConfigured: !!GEMINI_API_KEY 
  });
});

// Proxy to Gemini API
app.post('/api/gemini/generate', async (req, res) => {
  if (!GEMINI_API_KEY) {
    return res.status(500).json({ 
      error: 'Gemini API key not configured. Please set GEMINI_API_KEY environment variable.' 
    });
  }

  try {
    const response = await fetch(`${GEMINI_API_URL}?key=${GEMINI_API_KEY}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req.body)
    });

    const data = await response.json();
    
    if (!response.ok) {
      return res.status(response.status).json(data);
    }

    res.json(data);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
  console.log(`Gemini proxy server running on port ${PORT}`);
  console.log(`API Key configured: ${GEMINI_API_KEY ? 'Yes' : 'No'}`);
});
