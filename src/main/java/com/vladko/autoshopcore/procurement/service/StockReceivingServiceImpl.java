package com.vladko.autoshopcore.procurement.service;

import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.exception.PartNotFoundException;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import com.vladko.autoshopcore.procurement.dto.StockReceiptDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockReceivingServiceImpl implements StockReceivingService {

    private final PartRepository partRepository;

    @Override
    @Transactional
    public PartResponseDTO receive(StockReceiptDTO dto) {
        Part part = partRepository.findById(dto.getTargetPartId())
                .orElseThrow(() -> new PartNotFoundException(dto.getTargetPartId()));

        if (dto.getReceivedQuantity() == null || dto.getReceivedQuantity() <= 0) {
            throw new IllegalArgumentException("Received quantity must be greater than zero");
        }

        part.setStockQuantity(part.getStockQuantity() + dto.getReceivedQuantity());
        if (dto.getSalePrice() != null) {
            part.setCost(dto.getSalePrice());
        }

        return mapToResponse(partRepository.save(part));
    }

    private PartResponseDTO mapToResponse(Part part) {
        return PartResponseDTO.builder()
                .id(part.getId())
                .brand(part.getBrand())
                .name(part.getName())
                .articleNumber(part.getArticleNumber())
                .cost(part.getCost())
                .stockQuantity(part.getStockQuantity())
                .reservedQuantity(part.getReservedQuantity())
                .availableQuantity(part.getStockQuantity() - part.getReservedQuantity())
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }
}
