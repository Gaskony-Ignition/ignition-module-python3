package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Metadata for a saved Python script (without the full code).
 */
public class ScriptMetadata {
    private String id;
    private String name;
    private String description;
    private String author;
    private String createdDate;
    private String lastModified;
    private String folderPath;
    private String version;

    public ScriptMetadata() {
    }

    public ScriptMetadata(String id, String name, String description,
                         String author, String createdDate, String lastModified,
                         String folderPath, String version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.author = author;
        this.createdDate = createdDate;
        this.lastModified = lastModified;
        this.folderPath = folderPath;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
