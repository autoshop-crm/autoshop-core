package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.parts.entity.OrderPartItem;
import com.vladko.autoshopcore.parts.repository.OrderPartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderFinancialsService {

    private final OrderPartItemRepository orderPartItemRepository;

    public void initialize(Order order) {
        order.setLaborTotal(defaultIfNull(order.getLaborTotal()));
        order.setPartsTotal(defaultIfNull(order.getPartsTotal()));
        order.setDiscountAmount(defaultIfNull(order.getDiscountAmount()));
        recalculate(order);
    }

    public void updateEstimate(Order order, BigDecimal laborTotal, BigDecimal discountAmount) {
        Objects.requireNonNull(laborTotal, "Labor total must not be null");
        Objects.requireNonNull(discountAmount, "Discount amount must not be null");

        if (laborTotal.compareTo(BigDecimal.ZERO) < 0 || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderConflictException("Estimate amounts cannot be negative");
        }

        order.setLaborTotal(laborTotal);
        order.setDiscountAmount(discountAmount);
        recalculate(order);
    }

    public void recalculate(Order order) {
        BigDecimal partsTotal = calculatePartsTotal(order.getId());
        BigDecimal laborTotal = defaultIfNull(order.getLaborTotal());
        BigDecimal discountAmount = defaultIfNull(order.getDiscountAmount());
        BigDecimal costsTotal = laborTotal.add(partsTotal);

        if (discountAmount.compareTo(costsTotal) > 0) {
            throw new OrderConflictException("Discount amount cannot exceed total costs");
        }

        order.setPartsTotal(partsTotal);
        order.setLaborTotal(laborTotal);
        order.setDiscountAmount(discountAmount);
        order.setCostsTotal(costsTotal);
        order.setFinalAmount(costsTotal.subtract(discountAmount));
    }

    private BigDecimal calculatePartsTotal(Integer orderId) {
        if (orderId == null) {
            return BigDecimal.ZERO;
        }

        List<OrderPartItem> items = orderPartItemRepository.findAllByOrderIdOrderByIdAsc(orderId);
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
