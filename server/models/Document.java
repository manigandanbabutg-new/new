package com.lumina.workspace.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Document {
    private String id;
    private String title;
    private String content;
    private String parentId;
    private String icon;
    private String coverUrl;
    private List<String> tags;
    private boolean isArchived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Document() {
        this.tags = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Document(String id, String title, String content, String parentId, String icon, String coverUrl) {
        this.id = id;
        this.title = title != null ? title : "Untitled Page";
        this.content = content != null ? content : "";
        this.parentId = parentId;
        this.icon = icon != null ? icon : "📄";
        this.coverUrl = coverUrl;
        this.tags = new ArrayList<>();
        this.isArchived = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
