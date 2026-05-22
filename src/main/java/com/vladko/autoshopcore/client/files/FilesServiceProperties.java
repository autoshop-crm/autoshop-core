package com.vladko.autoshopcore.client.files;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.files")
public record FilesServiceProperties(
        String baseUrl,
        String listPath,
        String getPathTemplate,
        String presignedDownloadPathTemplate
) {
    public FilesServiceProperties {
        baseUrl = normalize(baseUrl, "http://localhost:8084");
        listPath = normalize(listPath, "/api/files");
        getPathTemplate = normalize(getPathTemplate, "/api/files/{fileId}");
        presignedDownloadPathTemplate = normalize(presignedDownloadPathTemplate, "/api/files/{fileId}/presigned-download-url");
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
