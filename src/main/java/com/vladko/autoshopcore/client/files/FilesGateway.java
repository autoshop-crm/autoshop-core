package com.vladko.autoshopcore.client.files;

import java.util.List;

public interface FilesGateway {
    List<ExternalFileMetadataDTO> listByOwner(String ownerType, String ownerId);
    ExternalFileMetadataDTO getById(String fileId);
    ExternalPresignedDownloadUrlDTO createPresignedDownloadUrl(String fileId);
}
