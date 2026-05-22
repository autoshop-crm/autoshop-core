package com.vladko.autoshopcore.client.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerFileDownloadUrlResponseDTO {
    String url;
}
