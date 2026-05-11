package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.exception.OrderRequestedPartNotFoundException;
import com.vladko.autoshopcore.parts.repository.OrderRequestedPartRepository;
import com.vladko.autoshopcore.procurement.dto.SupplierQuoteSearchResponseDTO;
import com.vladko.autoshopcore.procurement.service.SupplierQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderRequestedPartQuoteServiceImpl implements OrderRequestedPartQuoteService {

    private final OrderRequestedPartRepository orderRequestedPartRepository;
    private final SupplierQuoteService supplierQuoteService;

    @Override
    @Transactional(readOnly = true)
    public SupplierQuoteSearchResponseDTO getQuotes(Integer orderId, Integer requestedPartId) {
        OrderRequestedPart requestedPart = orderRequestedPartRepository.findByIdAndOrderId(requestedPartId, orderId)
                .orElseThrow(() -> new OrderRequestedPartNotFoundException(orderId, requestedPartId));
        String query = requestedPart.getArticleNumber();
        return supplierQuoteService.searchCarretaQuotes(query);
    }
}
