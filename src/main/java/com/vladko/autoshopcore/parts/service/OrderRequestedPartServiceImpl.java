package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderRequestedPartResponseDTO;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.exception.PartNotFoundException;
import com.vladko.autoshopcore.parts.repository.OrderRequestedPartRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderRequestedPartServiceImpl implements OrderRequestedPartService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final OrderRequestedPartRepository orderRequestedPartRepository;
    private final OrderRequestedPartMapper mapper;

    @Override
    @Transactional
    public OrderRequestedPartResponseDTO create(Integer orderId, OrderRequestedPartCreateDTO dto) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        Part matchedLocalPart = dto.getMatchedLocalPartId() == null ? null : partRepository.findById(dto.getMatchedLocalPartId())
                .orElseThrow(() -> new PartNotFoundException(dto.getMatchedLocalPartId()));

        OrderRequestedPart requestedPart = OrderRequestedPart.builder()
                .order(order)
                .articleNumber(normalizeRequiredText(dto.getArticleNumber(), true, 30))
                .brand(normalizeOptionalText(dto.getBrand(), 20))
                .name(normalizeRequiredText(dto.getName(), false, 100))
                .umapiArticleId(dto.getUmapiArticleId())
                .matchedLocalPart(matchedLocalPart)
                .requestedQuantity(dto.getQuantity())
                .status(OrderRequestedPartStatus.OUT_OF_STOCK)
                .build();
        return mapper.map(orderRequestedPartRepository.save(requestedPart));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderRequestedPartResponseDTO> getAllByOrderId(Integer orderId) {
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderRequestedPartRepository.findAllByOrderIdOrderByIdAsc(orderId).stream().map(mapper::map).toList();
    }

    private String normalizeRequiredText(String value, boolean uppercase, int maxLen) {
        String normalized = normalizeOptionalText(value, maxLen);
        if (normalized == null) throw new IllegalArgumentException("Value must not be blank");
        return uppercase ? normalized.toUpperCase(Locale.ROOT) : normalized;
    }

    private String normalizeOptionalText(String value, int maxLen) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        if (normalized.length() > maxLen) {
            normalized = normalized.substring(0, maxLen);
        }
        return normalized;
    }
}
