package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.ExternalPartSearchResponseDTO;

public interface ExternalPartSearchService {

    ExternalPartSearchResponseDTO search(String articleNumber, String brand, Integer limit, Integer offset);
}
