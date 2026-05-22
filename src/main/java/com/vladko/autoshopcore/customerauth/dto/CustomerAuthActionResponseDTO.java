package com.vladko.autoshopcore.customerauth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerAuthActionResponseDTO {
    boolean success;
    String message;
}
