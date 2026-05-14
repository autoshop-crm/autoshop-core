package com.vladko.autoshopcore.servicecatalog.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceCategoryResponseDTO {
    Integer id;
    String name;
    Integer displayOrder;
    Boolean active;
}
