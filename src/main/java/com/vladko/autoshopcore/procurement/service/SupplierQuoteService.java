package com.vladko.autoshopcore.procurement.service;

import com.vladko.autoshopcore.procurement.dto.SupplierQuoteSearchResponseDTO;

public interface SupplierQuoteService {

    SupplierQuoteSearchResponseDTO searchCarretaQuotes(String query);
}
