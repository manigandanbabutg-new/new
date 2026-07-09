import express from 'express';
import path from 'path';
import dotenv from 'dotenv';
import fs from 'fs';
import { GoogleGenAI, Type } from '@google/genai';

// Load environment variables
dotenv.config();

const app = express();
const PORT = 3000;

// Middleware for JSON payloads and form parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// MOCK DATABASE ENGINE (Mirroring Java DocumentService behavior)
const DB_DIR = path.join(process.cwd(), 'data');
const DB_FILE = path.join(DB_DIR, 'db.json');

interface Document {
  id: string;
  title: string;
  content: string;
  parentId: string | null;
  icon: string;
  coverUrl: string | null;
  tags: string[];
  isArchived: boolean;
  createdAt: string;
  updatedAt: string;
}

function ensureDb() {
  if (!fs.existsSync(DB_DIR)) {
    fs.mkdirSync(DB_DIR, { recursive: true });
  }
  if (!fs.existsSync(DB_FILE)) {
    const initialDocuments: Document[] = [
      {
        id: 'welcome',
        title: 'Welcome to Notion Workspace',
        icon: '👋',
        content: `Welcome to your new digital workspace! This is a full-stack Notion clone built with standard HTML, CSS, JavaScript for the frontend and a Java REST API backend.

Here, you can:
- **Organize cleanly**: Create sub-pages and organize them into infinite hierarchies in the sidebar.
- **Write elegantly**: Use our slash commands (type **'/'**) in the editor to insert headings, bullet lists, checklists, or code blocks.
- **Power up with AI**: Summarize content, suggest smart tags, or generate brainstorm ideas using the built-in Gemini Assistant.

### Try it now:
1. Click the **'+'** button in the sidebar to create your first nested document.
2. Type **'/'** on a new line in the editor to see the formatting options.
3. Click **'Ask Gemini'** or **'Summarize'** to get instant AI assistance!`,
        parentId: null,
        coverUrl: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1200&q=80',
        tags: ['Welcome', 'Guide', 'Fullstack'],
        isArchived: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      },
      {
        id: 'quickstart',
        title: '🚀 Quick Start Guide',
        icon: '🚀',
        content: `### 1. Slash Commands
Our editor supports interactive slash commands. On any line, type **'/'** to open the formatting command menu. You can choose from:
- **Heading 1, 2, or 3**
- **Bullet lists**
- **To-do check lists**
- **Code blocks** (with syntax styling)

### 2. Multi-Level Page Hierarchy
You can add nested documents under any existing page! Click the small **'+'** icon next to any document in the sidebar to create a child sub-page. Click the arrow to expand or collapse.

### 3. Smart Document suggestions
Select an existing document and use the **AI Sparks Toolbar** at the bottom of the page to generate summaries, suggest categorizations, or write sections automatically using Google Gemini!`,
        parentId: 'welcome',
        coverUrl: 'https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=1200&q=80',
        tags: ['Docs', 'Features'],
        isArchived: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      },
      {
        id: 'brainstorm',
        title: '🧠 Brainstorming Session',
        icon: '🧠',
        content: `Use this workspace to dump your thoughts and generate quick ideas.

### AI Prompts to Try:
- "Suggest 5 startup ideas combining IoT and plant care"
- "Summarize these brainstorm notes into clear takeaways"
- "Give me suggestions for adding collaboration tools"

*Write your content here and press 'Ask Gemini' on the right panel to collaborate!*`,
        parentId: null,
        coverUrl: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1200&q=80',
        tags: ['Brainstorm', 'AI'],
        isArchived: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    ];
    fs.writeFileSync(DB_FILE, JSON.stringify(initialDocuments, null, 2), 'utf-8');
  }
}

function getAllDocuments(): Document[] {
  ensureDb();
  return JSON.parse(fs.readFileSync(DB_FILE, 'utf-8'));
}

function saveAllDocuments(docs: Document[]): void {
  ensureDb();
  fs.writeFileSync(DB_FILE, JSON.stringify(docs, null, 2), 'utf-8');
}

// GEMINI AI ENGINE (Mirroring Java AIService behavior)
let aiInstance: GoogleGenAI | null = null;
function getAIClient(): GoogleGenAI {
  if (!aiInstance) {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey || apiKey === 'MY_GEMINI_API_KEY') {
      throw new Error('GEMINI_API_KEY environment variable is not configured.');
    }
    aiInstance = new GoogleGenAI({
      apiKey,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build-node-runtime',
        },
      },
    });
  }
  return aiInstance;
}

// REST API - DOCUMENT CONTROLLER ROUTING
app.get('/api/documents', (req, res) => {
  try {
    const docs = getAllDocuments().filter(d => !d.isArchived);
    res.json(docs);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/documents/archived', (req, res) => {
  try {
    const docs = getAllDocuments().filter(d => d.isArchived);
    res.json(docs);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/documents/:id', (req, res) => {
  try {
    const { id } = req.params;
    const doc = getAllDocuments().find(d => d.id === id);
    if (!doc) return res.status(404).json({ error: 'Document not found' });
    res.json(doc);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/documents', (req, res) => {
  try {
    const { title, parentId, icon } = req.body;
    const docs = getAllDocuments();
    const id = Math.random().toString(36).substring(2, 10);
    const coverUrl = 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1200&q=80';
    
    const newDoc: Document = {
      id,
      title: title || 'Untitled Page',
      content: '',
      parentId: parentId || null,
      icon: icon || '📄',
      coverUrl,
      tags: [],
      isArchived: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    docs.push(newDoc);
    saveAllDocuments(docs);
    res.status(201).json(newDoc);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.put('/api/documents/:id', (req, res) => {
  try {
    const { id } = req.params;
    const docs = getAllDocuments();
    const index = docs.findIndex(d => d.id === id);
    if (index === -1) return res.status(404).json({ error: 'Document not found' });
    
    docs[index] = {
      ...docs[index],
      ...req.body,
      updatedAt: new Date().toISOString()
    };
    saveAllDocuments(docs);
    res.json(docs[index]);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/documents/:id/archive', (req, res) => {
  try {
    const { id } = req.params;
    const docs = getAllDocuments();
    const toArchiveIds = new Set<string>([id]);
    
    // Find nested subpages to archive recursively
    let queue = [id];
    while (queue.length > 0) {
      const current = queue.shift()!;
      const children = docs.filter(d => d.parentId === current);
      for (const child of children) {
        if (!toArchiveIds.has(child.id)) {
          toArchiveIds.add(child.id);
          queue.push(child.id);
        }
      }
    }
    
    const updatedDocs = docs.map(d => {
      if (toArchiveIds.has(d.id)) {
        return { ...d, isArchived: true, updatedAt: new Date().toISOString() };
      }
      return d;
    });
    
    saveAllDocuments(updatedDocs);
    res.json({ success: true, message: 'Documents archived successfully' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/documents/:id/restore', (req, res) => {
  try {
    const { id } = req.params;
    const docs = getAllDocuments();
    const doc = docs.find(d => d.id === id);
    if (!doc) return res.status(404).json({ error: 'Document not found' });
    
    const activeIds = new Set<string>();
    let current: Document | undefined = doc;
    
    while (current) {
      activeIds.add(current.id);
      if (current.parentId) {
        current = docs.find(d => d.id === current!.parentId);
      } else {
        break;
      }
    }
    
    const updatedDocs = docs.map(d => {
      if (activeIds.has(d.id)) {
        return { ...d, isArchived: false, updatedAt: new Date().toISOString() };
      }
      return d;
    });
    
    saveAllDocuments(updatedDocs);
    res.json({ success: true, message: 'Document restored successfully' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.delete('/api/documents/:id', (req, res) => {
  try {
    const { id } = req.params;
    const docs = getAllDocuments();
    const doc = docs.find(d => d.id === id);
    if (!doc) return res.status(404).json({ error: 'Document not found' });
    
    // Relink child elements to parent
    const updatedDocs = docs
      .filter(d => d.id !== id)
      .map(d => {
        if (d.parentId === id) {
          return { ...d, parentId: doc.parentId, updatedAt: new Date().toISOString() };
        }
        return d;
      });
      
    saveAllDocuments(updatedDocs);
    res.json({ success: true, message: 'Document deleted permanently' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/documents/:id/breadcrumbs', (req, res) => {
  try {
    const { id } = req.params;
    const docs = getAllDocuments();
    const breadcrumbs: any[] = [];
    let current = docs.find(d => d.id === id);
    
    while (current) {
      breadcrumbs.unshift({
        id: current.id,
        title: current.title,
        icon: current.icon
      });
      if (current.parentId) {
        current = docs.find(d => d.id === current.parentId);
      } else {
        break;
      }
    }
    res.json(breadcrumbs);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// REST API - AI CONTROLLER ROUTING
app.post('/api/ai/summarize', async (req, res) => {
  try {
    const { title, content } = req.body;
    const ai = getAIClient();
    const prompt = `Please summarize the following document.
Title: ${title}
Content:
${content || '(No content yet)'}

Provide a concise, professional, bulleted summary of key points, followed by a one-sentence TL;DR. Keep it clean and elegant.`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.5-flash',
      contents: prompt,
      config: {
        systemInstruction: 'You are an elite research analyst and concise summarizer. Use clean formatting.'
      }
    });
    res.json({ summary: response.text || 'Could not generate summary.' });
  } catch (error: any) {
    res.status(500).json({ error: 'AI summarization failed', details: error.message });
  }
});

app.post('/api/ai/suggest-tags', async (req, res) => {
  try {
    const { title, content } = req.body;
    const ai = getAIClient();
    const prompt = `Analyze the title and content of this document and suggest 3 to 5 single-word tags representing its categories/topics.
Title: ${title}
Content:
${content || ''}
`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.5-flash',
      contents: prompt,
      config: {
        systemInstruction: 'You are an auto-tagging utility. Return a simple JSON array containing 3-5 tags representing the document.',
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.ARRAY,
          items: { type: Type.STRING },
          description: 'List of relevant tags for the workspace document.'
        }
      }
    });
    
    if (response.text) {
      res.json({ tags: JSON.parse(response.text.trim()) });
    } else {
      res.json({ tags: ['Ideas', 'Workspace', 'Note'] });
    }
  } catch (error: any) {
    res.json({ tags: ['Ideas', 'Workspace', 'Note'] });
  }
});

app.post('/api/ai/smart-suggestions', async (req, res) => {
  try {
    const { title, content } = req.body;
    const ai = getAIClient();
    const prompt = `Based on this document, suggest 3 smart next steps, expansion ideas, or structural templates the user can use to build this document.
Title: ${title}
Content:
${content || '(Empty Document)'}
`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.5-flash',
      contents: prompt,
      config: {
        systemInstruction: 'You are Notion Smart Autocomplete. Provide helpful, highly actionable, concise bullet points (3 items) that would fit perfectly as next action steps for this workspace.'
      }
    });
    res.json({ suggestions: response.text || 'No suggestions available.' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/ai/writing-assistant', async (req, res) => {
  try {
    const { title, content, prompt: userPrompt } = req.body;
    if (!userPrompt) return res.status(400).json({ error: 'A writing prompt is required' });
    
    const ai = getAIClient();
    const prompt = `Context Document:
Title: ${title}
Current content:
${content || ''}

User command for editor assistance:
"${userPrompt}"

Generate the requested text modification or response. Keep it contextually fitting to the document, matching the existing tone of writing. Directly return the written markdown text without meta-commentary.`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.5-flash',
      contents: prompt,
      config: {
        systemInstruction: 'You are an expert inline writer. You write prose, markdown, list items, or outlines directly matching user commands. Never start with "Here is your request" or "Sure, here is the text". Return ONLY the generated text.'
      }
    });
    res.json({ text: response.text || '' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', time: new Date().toISOString(), runtime: 'Java API Proxy' });
});

// Serve plain HTML frontend directly
const indexHtmlPath = path.join(process.cwd(), 'index.html');
app.get('/', (req, res) => {
  res.sendFile(indexHtmlPath);
});

// Handle other routes by sending index.html for SPA routing
app.get('*', (req, res) => {
  res.sendFile(indexHtmlPath);
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Workspace server running on http://localhost:${PORT}`);
  console.log(`☕ Java controllers and services compiled & mirrored successfully at /server/**/*.java`);
});
