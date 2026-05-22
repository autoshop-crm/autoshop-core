package com.vladko.autoshopcore.client.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class CustomerFileMetadataDTO {
    String fileId;
    String filename;
    String category;
    String ownerType;
    String ownerId;
    String contentType;
    Long sizeBytes;
    String status;
    Instant createdAt;
}
