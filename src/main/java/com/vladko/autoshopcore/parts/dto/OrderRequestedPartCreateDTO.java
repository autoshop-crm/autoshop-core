package com.vladko.autoshopcore.parts.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestedPartCreateDTO {

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
