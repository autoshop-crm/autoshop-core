package com.vladko.autoshopcore.loyalty.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLoyaltySpendDTO {

    @NotNull
    @Min(1)
    private Integer points;
}
