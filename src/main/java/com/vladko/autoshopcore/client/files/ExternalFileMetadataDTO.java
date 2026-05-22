package com.vladko.autoshopcore.client.files;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class ExternalFileMetadataDTO {
    @JsonProperty("fileId")
    @JsonAlias("id")
    private String fileId;

    @JsonProperty("filename")
    @JsonAlias("originalFilename")
    private String filename;
    private String category;
    private String ownerType;
    private String ownerId;
    private String contentType;
    private Long sizeBytes;
    private String status;
    private Instant createdAt;
}
