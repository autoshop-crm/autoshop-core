package com.vladko.autoshopcore.procurement.service;

import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.procurement.dto.StockReceiptDTO;

public interface StockReceivingService {

    PartResponseDTO receive(StockReceiptDTO dto);
}
