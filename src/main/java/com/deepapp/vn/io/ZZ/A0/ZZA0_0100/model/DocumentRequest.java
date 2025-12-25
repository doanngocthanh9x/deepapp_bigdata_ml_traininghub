package com.deepapp.vn.io.ZZ.A0.ZZA0_0100.model;

/**
 * Request model for document processing
 */
public class DocumentRequest {
    private String data;        // Base64 encoded document data
    private String filename;    // Original filename
    private String options;     // Optional processing options (JSON string)

    public DocumentRequest() {
    }

    public DocumentRequest(String data, String filename, String options) {
        this.data = data;
        this.filename = filename;
        this.options = options;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }
}
