package com.lumina.workspace.services;

import com.lumina.workspace.models.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {
    // In-memory thread-safe mock database to hold documents
    private final Map<String, Document> database = new ConcurrentHashMap<>();

    public DocumentService() {
        // Seed initial documents
        seedInitialData();
    }

    /**
     * Seed database with starter workspace pages
     */
    private void seedInitialData() {
        Document welcome = new Document(
            "welcome",
            "Welcome to Notion Workspace",
            "Welcome to your new digital workspace! This is a full-stack Notion clone built with a structured Java REST API backend.\n\n" +
            "Here, you can:\n" +
            "- **Organize cleanly**: Create sub-pages and organize them into infinite hierarchies in the sidebar.\n" +
            "- **Write elegantly**: Use our slash commands (type **'/'**) in the editor to insert headings, bullet points, checklists, or code blocks.\n" +
            "- **Power up with AI**: Summarize content, suggest smart tags, or generate brainstorm ideas using the built-in Gemini Assistant.\n\n" +
            "### Try it now:\n" +
            "1. Click the **'+'** button in the sidebar to create your first nested document.\n" +
            "2. Type **'/'** on a new line in the editor to see the formatting options.\n" +
            "3. Click **'Ask Gemini'** or **'Summarize'** to get instant AI assistance!",
            null,
            "👋",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1200&q=80"
        );
        welcome.setTags(Arrays.asList("Welcome", "Guide", "Fullstack"));
        database.put(welcome.getId(), welcome);

        Document quickstart = new Document(
            "quickstart",
            "🚀 Quick Start Guide",
            "### 1. Slash Commands\n" +
            "Our editor supports interactive slash commands. On any line, type **'/'** to open the formatting command menu. You can choose from:\n" +
            "- **Heading 1, 2, or 3**\n" +
            "- **Bullet lists**\n" +
            "- **To-do check lists**\n" +
            "- **Code blocks** (with syntax styling)\n\n" +
            "### 2. Multi-Level Page Hierarchy\n" +
            "You can add nested documents under any existing page! Click the small **'+'** icon next to any document in the sidebar to create a child sub-page. Click the arrow to expand or collapse.\n\n" +
            "### 3. Smart Document suggestions\n" +
            "Select an existing document and use the **AI Sparks Toolbar** at the bottom of the page to generate summaries, suggest categorizations, or write sections automatically using Google Gemini!",
            "welcome",
            "🚀",
            "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=1200&q=80"
        );
        quickstart.setTags(Arrays.asList("Docs", "Features"));
        database.put(quickstart.getId(), quickstart);

        Document brainstorm = new Document(
            "brainstorm",
            "🧠 Brainstorming Session",
            "Use this workspace to dump your thoughts and generate quick ideas.\n\n" +
            "### AI Prompts to Try:\n" +
            "- \"Suggest 5 startup ideas combining IoT and plant care\"\n" +
            "- \"Summarize these brainstorm notes into clear takeaways\"\n" +
            "- \"Give me suggestions for adding collaboration tools\"\n\n" +
            "*Write your content here and press 'Ask Gemini' on the right panel to collaborate!*",
            null,
            "🧠",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1200&q=80"
        );
        brainstorm.setTags(Arrays.asList("Brainstorm", "AI"));
        database.put(brainstorm.getId(), brainstorm);
    }

    /**
     * Fetch all active non-archived documents
     */
    public List<Document> getActiveDocuments() {
        return database.values().stream()
                .filter(doc -> !doc.isArchived())
                .sorted(Comparator.comparing(Document::getCreatedAt))
                .collect(Collectors.toList());
    }

    /**
     * Fetch all archived documents (trash bin)
     */
    public List<Document> getArchivedDocuments() {
        return database.values().stream()
                .filter(Document::isArchived)
                .collect(Collectors.toList());
    }

    /**
     * Find document by ID
     */
    public Optional<Document> getDocumentById(String id) {
        return Optional.ofNullable(database.get(id));
    }

    /**
     * Create a new document
     */
    public Document createDocument(String title, String parentId, String icon) {
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        String coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1200&q=80";
        
        Document newDoc = new Document(randomId, title, "", parentId, icon, coverUrl);
        database.put(newDoc.getId(), newDoc);
        return newDoc;
    }

    /**
     * Update an existing document
     */
    public Optional<Document> updateDocument(String id, Document updates) {
        Document existing = database.get(id);
        if (existing == null) {
            return Optional.empty();
        }

        if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
        if (updates.getContent() != null) existing.setContent(updates.getContent());
        if (updates.getIcon() != null) existing.setIcon(updates.getIcon());
        if (updates.getCoverUrl() != null) existing.setCoverUrl(updates.getCoverUrl());
        if (updates.getTags() != null) existing.setTags(updates.getTags());
        existing.setUpdatedAt(LocalDateTime.now());

        return Optional.of(existing);
    }

    /**
     * Archive document and all descendants recursively
     */
    public boolean archiveDocument(String id) {
        if (!database.containsKey(id)) {
            return false;
        }

        Set<String> toArchive = new HashSet<>();
        toArchive.add(id);

        Queue<String> queue = new LinkedList<>();
        queue.add(id);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            for (Document doc : database.values()) {
                if (currentId.equals(doc.getParentId()) && !toArchive.contains(doc.getId())) {
                    toArchive.add(doc.getId());
                    queue.add(doc.getId());
                }
            }
        }

        for (String archiveId : toArchive) {
            Document doc = database.get(archiveId);
            if (doc != null) {
                doc.setArchived(true);
            }
        }

        return true;
    }

    /**
     * Restore document and its ancestors if archived
     */
    public boolean restoreDocument(String id) {
        Document doc = database.get(id);
        if (doc == null) {
            return false;
        }

        doc.setArchived(false);

        String parentId = doc.getParentId();
        while (parentId != null) {
            Document parent = database.get(parentId);
            if (parent != null && parent.isArchived()) {
                parent.setArchived(false);
                parentId = parent.getParentId();
            } else {
                break;
            }
        }

        return true;
    }

    /**
     * Permanently delete a document
     */
    public boolean deletePermanently(String id) {
        Document doc = database.get(id);
        if (doc == null) {
            return false;
        }

        // Re-link children to the deleted document's parent
        for (Document d : database.values()) {
            if (id.equals(d.getParentId())) {
                d.setParentId(doc.getParentId());
            }
        }

        database.remove(id);
        return true;
    }

    /**
     * Get breadcrumb ancestry list
     */
    public List<Map<String, String>> getBreadcrumbs(String id) {
        List<Map<String, String>> breadcrumbs = new ArrayList<>();
        Document current = database.get(id);

        while (current != null) {
            Map<String, String> crumb = new HashMap<>();
            crumb.put("id", current.getId());
            crumb.put("title", current.getTitle());
            crumb.put("icon", current.getIcon());
            breadcrumbs.add(0, crumb);

            if (current.getParentId() != null) {
                current = database.get(current.getParentId());
            } else {
                break;
            }
        }

        return breadcrumbs;
    }
}
