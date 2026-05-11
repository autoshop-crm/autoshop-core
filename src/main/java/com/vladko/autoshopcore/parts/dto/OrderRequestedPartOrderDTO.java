package com.vladko.autoshopcore.parts.dto;

import com.vladko.autoshopcore.procurement.dto.CarretaQuoteOrderDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestedPartOrderDTO {

    @NotNull
    @Valid
    private CarretaQuoteOrderDTO quote;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal salePrice;

    @Builder.Default
    private Boolean createExternalOrder = Boolean.TRUE;

    private String clientComment;
}
