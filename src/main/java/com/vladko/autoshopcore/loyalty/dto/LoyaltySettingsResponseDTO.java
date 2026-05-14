package com.vladko.autoshopcore.loyalty.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoyaltySettingsResponseDTO {
    boolean enabled;
    boolean earnEnabled;
    boolean spendEnabled;
    boolean visible;
}
