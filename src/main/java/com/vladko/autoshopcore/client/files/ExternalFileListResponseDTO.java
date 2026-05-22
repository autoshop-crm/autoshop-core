package com.vladko.autoshopcore.client.files;

import lombok.Data;

import java.util.List;

@Data
public class ExternalFileListResponseDTO {
    private List<ExternalFileMetadataDTO> items;
    private Integer page;
    private Integer size;
    private Long totalElements;
}
