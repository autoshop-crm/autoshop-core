package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.entity.LegacyOrderStatus;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LegacyOrderCompatibilityService {

    private final OrderService orderService;
    private final LegacyOrderStatusProjector legacyOrderStatusProjector;

    @Transactional(readOnly = true)
    public OrderResponseDTO getById(Integer id) {
        return adapt(orderService.getById(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByCustomerId(Integer customerId) {
        return orderService.getAllByCustomerId(customerId).stream().map(this::adapt).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByVehicleId(Integer vehicleId) {
        return orderService.getAllByVehicleId(vehicleId).stream().map(this::adapt).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByStatus(LegacyOrderStatus status) {
        return orderService.getAll().stream()
                .map(this::adapt)
                .filter(order -> order.getLegacyStatus() == status)
                .toList();
    }

    private OrderResponseDTO adapt(OrderResponseDTO source) {
        LegacyOrderStatus legacyStatus = legacyOrderStatusProjector.project(source.getStatus());
        source.setCrmStatus(source.getStatus());
        source.setLegacyStatus(legacyStatus);
        if (legacyStatus != null) {
            source.setStatus(OrderStatus.valueOf(legacyStatus.name()));
        }
        return source;
    }
}
