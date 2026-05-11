package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.procurement.dto.SupplierQuoteSearchResponseDTO;

public interface OrderRequestedPartQuoteService {
    SupplierQuoteSearchResponseDTO getQuotes(Integer orderId, Integer requestedPartId);
}
