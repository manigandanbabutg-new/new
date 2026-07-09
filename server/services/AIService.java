package com.lumina.workspace.services;

import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

@Service
public class AIService {
    private final String geminiModel = "gemini-3.5-flash";
    
    /**
     * Helper to get Gemini API key from environment
     */
    private String getApiKey() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("MY_GEMINI_API_KEY")) {
            throw new IllegalStateException("GEMINI_API_KEY environment variable is not configured.");
        }
        return apiKey;
    }

    /**
     * Executes HTTP Post Request to Google Gemini REST API
     */
    private String callGemini(String prompt, String systemInstruction, boolean isJson) {
        try {
            String apiKey = getApiKey();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + apiKey;

            // Build request payload matching Gemini API JSON schema
            JSONObject requestBody = new JSONObject();
            
            // Contents payload
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            
            JSONArray partsArray = new JSONArray();
            partsArray.put(part);
            
            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", partsArray);
            contents.put(contentObj);
            requestBody.put("contents", contents);

            // Configuration payload
            JSONObject config = new JSONObject();
            if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                JSONObject siPart = new JSONObject();
                siPart.put("text", systemInstruction);
                JSONArray siParts = new JSONArray();
                siParts.put(siPart);
                JSONObject siContent = new JSONObject();
                siContent.put("parts", siParts);
                config.put("systemInstruction", siContent);
            }

            if (isJson) {
                config.put("responseMimeType", "application/json");
            }
            requestBody.put("generationConfig", config);

            // Create HTTP client and send request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "aistudio-build-java")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API call failed with HTTP code: " + response.statusCode() + " response: " + response.body());
            }

            // Parse response
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject contentResult = firstCandidate.getJSONObject("content");
                JSONArray partsResult = contentResult.getJSONArray("parts");
                if (partsResult.length() > 0) {
                    return partsResult.getJSONObject(0).getString("text");
                }
            }
            return "Could not generate content.";
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return "[AI Assistant Offline] Error: " + e.getMessage();
        }
    }

    /**
     * Summarizes the document title and content
     */
    public String summarizeDocument(String title, String content) {
        String prompt = "Please summarize the following document.\n" +
                "Title: " + title + "\n" +
                "Content:\n" + (content != null && !content.isEmpty() ? content : "(No content yet)") + "\n\n" +
                "Provide a concise, professional, bulleted summary of key points, followed by a one-sentence TL;DR. Keep it clean and elegant.";
        
        String systemInstruction = "You are an elite research analyst and concise summarizer. Use clean formatting.";
        return callGemini(prompt, systemInstruction, false);
    }

    /**
     * Suggests 3-5 relevant document tags in JSON list
     */
    public List<String> suggestTags(String title, String content) {
        String prompt = "Analyze the title and content of this document and suggest 3 to 5 single-word tags representing its categories/topics.\n" +
                "Title: " + title + "\n" +
                "Content:\n" + (content != null ? content : "") + "\n";
        
        String systemInstruction = "You are an auto-tagging utility. Return a simple JSON array of strings containing 3-5 tags representing the document.";
        
        try {
            String jsonResult = callGemini(prompt, systemInstruction, true);
            JSONArray array = new JSONArray(jsonResult.trim());
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                tags.add(array.getString(i));
            }
            return tags;
        } catch (Exception e) {
            System.err.println("Failed to parse tags JSON, returning defaults: " + e.getMessage());
            return List.of("Ideas", "Workspace", "Note");
        }
    }

    /**
     * Suggests 3 smart recommendations / next steps
     */
    public String smartDocumentSuggestions(String title, String content) {
        String prompt = "Based on this document, suggest 3 smart next steps, expansion ideas, or structural templates the user can use to build this document.\n" +
                "Title: " + title + "\n" +
                "Content:\n" + (content != null && !content.isEmpty() ? content : "(Empty Document)") + "\n";
        
        String systemInstruction = "You are Notion Smart Autocomplete. Provide helpful, highly actionable, concise bullet points (3 items) that would fit perfectly as next action steps for this workspace.";
        return callGemini(prompt, systemInstruction, false);
    }

    /**
     * In-line text generator/rewriter assistant
     */
    public String executeWritingAssistant(String title, String content, String actionPrompt) {
        String prompt = "Context Document:\n" +
                "Title: " + title + "\n" +
                "Current content:\n" + (content != null ? content : "") + "\n\n" +
                "User command for editor assistance:\n" +
                "\"" + actionPrompt + "\"\n\n" +
                "Generate the requested text modification or response. Keep it contextually fitting to the document, matching the existing tone of writing. Directly return the written markdown text without meta-commentary.";
        
        String systemInstruction = "You are an expert inline writer. You write prose, markdown, list items, or outlines directly matching user commands. Never start with conversational fluff. Return ONLY the generated text.";
        return callGemini(prompt, systemInstruction, false);
    }
}
