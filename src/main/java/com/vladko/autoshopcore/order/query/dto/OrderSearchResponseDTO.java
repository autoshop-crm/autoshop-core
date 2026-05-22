package com.vladko.autoshopcore.order.query.dto;

import com.vladko.autoshopcore.loyalty.dto.LoyaltySettingsResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OrderSearchResponseDTO {
    List<OrderResponseDTO> items;
    int page;
    int size;
    boolean hasMore;
    LoyaltySettingsResponseDTO loyaltySettings;
}
