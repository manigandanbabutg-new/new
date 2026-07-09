package com.lumina.workspace.controllers;

import com.lumina.workspace.services.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    private final AIService aiService;

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarize(@RequestBody Map<String, String> payload) {
        try {
            String title = payload.getOrDefault("title", "");
            String content = payload.getOrDefault("content", "");
            if (title.isEmpty() && content.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Title or content must be provided"));
            }
            String summary = aiService.summarizeDocument(title, content);
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Summarization failed", "details", e.getMessage()));
        }
    }

    @PostMapping("/suggest-tags")
    public ResponseEntity<Map<String, Object>> suggestTags(@RequestBody Map<String, String> payload) {
        try {
            String title = payload.getOrDefault("title", "");
            String content = payload.getOrDefault("content", "");
            List<String> tags = aiService.suggestTags(title, content);
            return ResponseEntity.ok(Map.of("tags", tags));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Tag suggestions failed", "details", e.getMessage()));
        }
    }

    @PostMapping("/smart-suggestions")
    public ResponseEntity<Map<String, String>> getSmartSuggestions(@RequestBody Map<String, String> payload) {
        try {
            String title = payload.getOrDefault("title", "");
            String content = payload.getOrDefault("content", "");
            String suggestions = aiService.smartDocumentSuggestions(title, content);
            return ResponseEntity.ok(Map.of("suggestions", suggestions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Smart suggestions failed", "details", e.getMessage()));
        }
    }

    @PostMapping("/writing-assistant")
    public ResponseEntity<Map<String, String>> getWritingAssistance(@RequestBody Map<String, String> payload) {
        try {
            String title = payload.getOrDefault("title", "");
            String content = payload.getOrDefault("content", "");
            String prompt = payload.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "A writing prompt is required"));
            }
            String resultText = aiService.executeWritingAssistant(title, content, prompt);
            return ResponseEntity.ok(Map.of("text", resultText));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Writing assistance failed", "details", e.getMessage()));
        }
    }
}
