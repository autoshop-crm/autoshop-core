package com.vladko.autoshopcore.event.notification;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OrderNotificationPayloadFactory {

    private static final String CURRENCY = "RUB";

    private final OrderNumberFormatter orderNumberFormatter;

    public OrderCreatedNotificationPayload orderCreated(Order order) {
        Customer customer = requireCustomer(order);
        Vehicle vehicle = requireVehicle(order);
        requireNotBlank(customer.getEmail(), "customer.email");

        return new OrderCreatedNotificationPayload(
                toLong(requireId(order)),
                orderNumberFormatter.format(requireId(order), order.getCreatedAt()),
                toLong(requireId(customer)),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                toLong(requireId(vehicle)),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getLicensePlate(),
                requireInstant(order.getCreatedAt(), "order.createdAt")
        );
    }

    public OrderStatusChangedNotificationPayload orderStatusChanged(Order order,
                                                                    OrderStatus previousStatus,
                                                                    OrderStatus newStatus,
                                                                    String managerComment) {
        Customer customer = requireCustomer(order);
        requireNotBlank(customer.getEmail(), "customer.email");
        if (previousStatus == null || newStatus == null) {
            throw new IllegalArgumentException("Order status values must not be null");
        }
        if (previousStatus == newStatus) {
            throw new IllegalArgumentException("Order status must change before notification payload is created");
        }

        Instant changedAt = order.getUpdatedAt() == null ? Instant.now() : order.getUpdatedAt();
        return new OrderStatusChangedNotificationPayload(
                toLong(requireId(order)),
                orderNumberFormatter.format(requireId(order), order.getCreatedAt()),
                toLong(requireId(customer)),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                previousStatus.name(),
                newStatus.name(),
                changedAt,
                managerComment == null ? "" : managerComment
        );
    }

    public OrderCompletedNotificationPayload orderCompleted(Order order, Integer loyaltyPointsEarned) {
        Customer customer = requireCustomer(order);
        requireNotBlank(customer.getEmail(), "customer.email");

        return new OrderCompletedNotificationPayload(
                toLong(requireId(order)),
                orderNumberFormatter.format(requireId(order), order.getCreatedAt()),
                toLong(requireId(customer)),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                requireInstant(order.getCompletedAt(), "order.completedAt"),
                defaultAmount(order.getFinalAmount()),
                CURRENCY,
                loyaltyPointsEarned == null ? 0 : loyaltyPointsEarned
        );
    }

    private Customer requireCustomer(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        if (order.getCustomer() == null) {
            throw new IllegalArgumentException("order.customer must not be null");
        }
        return order.getCustomer();
    }

    private Vehicle requireVehicle(Order order) {
        if (order.getVehicle() == null) {
            throw new IllegalArgumentException("order.vehicle must not be null");
        }
        return order.getVehicle();
    }

    private Integer requireId(Order order) {
        if (order.getId() == null) {
            throw new IllegalArgumentException("order.id must not be null");
        }
        return order.getId();
    }

    private Integer requireId(Customer customer) {
        if (customer.getId() == null) {
            throw new IllegalArgumentException("customer.id must not be null");
        }
        return customer.getId();
    }

    private Integer requireId(Vehicle vehicle) {
        if (vehicle.getId() == null) {
            throw new IllegalArgumentException("vehicle.id must not be null");
        }
        return vehicle.getId();
    }

    private Long toLong(Integer id) {
        return Long.valueOf(id);
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private Instant requireInstant(Instant value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
