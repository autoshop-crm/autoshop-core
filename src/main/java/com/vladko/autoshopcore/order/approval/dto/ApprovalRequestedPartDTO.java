package com.vladko.autoshopcore.order.approval.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovalRequestedPartDTO {
    @NotBlank
    @Size(max = 30)
    private String articleNumber;
    @Size(max = 20)
    private String brand;
    @NotBlank
    @Size(max = 100)
    private String name;
    private Integer umapiArticleId;
    private Integer matchedLocalPartId;
    @NotNull
    @Min(1)
    private Integer quantity;
}
