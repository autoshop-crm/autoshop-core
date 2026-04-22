package com.vladko.autoshopcore.procurement.service;

import com.vladko.autoshopcore.procurement.dto.PurchaseOrderCreateDTO;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderResponseDTO;

public interface PurchaseOrderService {

    PurchaseOrderResponseDTO create(PurchaseOrderCreateDTO dto);
}
