package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.UnifiedPartSearchResponseDTO;

public interface UnifiedPartSearchService {

    UnifiedPartSearchResponseDTO search(String articleNumber, String brand, Boolean availableOnly, Integer limit, Integer offset);
}
