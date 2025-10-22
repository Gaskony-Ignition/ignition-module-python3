package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Represents a single code completion result from the Python completion engine.
 */
public class CompletionResult {
    private String text;
    private String type;
    private String complete;
    private String description;
    private String docstring;
    private String signature;

    public CompletionResult() {
    }

    public CompletionResult(String text, String type, String complete, String description, String docstring, String signature) {
        this.text = text;
        this.type = type;
        this.complete = complete;
        this.description = description;
        this.docstring = docstring;
        this.signature = signature;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComplete() {
        return complete;
    }

    public void setComplete(String complete) {
        this.complete = complete;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocstring() {
        return docstring;
    }

    public void setDocstring(String docstring) {
        this.docstring = docstring;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "CompletionResult{" +
                "text='" + text + '\'' +
                ", type='" + type + '\'' +
                ", complete='" + complete + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
