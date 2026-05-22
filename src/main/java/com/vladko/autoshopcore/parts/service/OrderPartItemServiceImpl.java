package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.OrderPartItemCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPartItemServiceImpl implements OrderPartItemService {

    private final OrderPartInventoryCoordinator coordinator;

    @Override
    @Transactional
    public OrderPartItemResponseDTO create(Integer orderId, OrderPartItemCreateDTO dto) {
        return coordinator.create(orderId, dto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderPartItemResponseDTO> getAllByOrderId(Integer orderId) {
        return coordinator.getAllByOrderId(orderId);
    }

    @Override
    @Transactional
    public OrderPartItemResponseDTO update(Integer orderId, Integer itemId, OrderPartItemUpdateDTO dto) {
        return coordinator.update(orderId, itemId, dto);
    }

    @Override
    @Transactional
    public void delete(Integer orderId, Integer itemId) {
        coordinator.delete(orderId, itemId);
    }
}
