package com.lumina.workspace.controllers;

import com.lumina.workspace.models.Document;
import com.lumina.workspace.services.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<Document>> getActiveDocuments() {
        try {
            return ResponseEntity.ok(documentService.getActiveDocuments());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/archived")
    public ResponseEntity<List<Document>> getArchivedDocuments() {
        try {
            return ResponseEntity.ok(documentService.getArchivedDocuments());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable String id) {
        return documentService.getDocumentById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<Document> createDocument(@RequestBody Map<String, String> payload) {
        try {
            String title = payload.getOrDefault("title", "Untitled Page");
            String parentId = payload.get("parentId");
            String icon = payload.getOrDefault("icon", "📄");
            Document created = documentService.createDocument(title, parentId, icon);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(@PathVariable String id, @RequestBody Document updates) {
        return documentService.updateDocument(id, updates)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Map<String, Object>> archiveDocument(@PathVariable String id) {
        boolean success = documentService.archiveDocument(id);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Document archived successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", "Document not found"));
        }
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, Object>> restoreDocument(@PathVariable String id) {
        boolean success = documentService.restoreDocument(id);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Document restored successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", "Document not found"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePermanently(@PathVariable String id) {
        boolean success = documentService.deletePermanently(id);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Document deleted permanently"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", "Document not found"));
        }
    }

    @GetMapping("/{id}/breadcrumbs")
    public ResponseEntity<List<Map<String, String>>> getBreadcrumbs(@PathVariable String id) {
        try {
            return ResponseEntity.ok(documentService.getBreadcrumbs(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
