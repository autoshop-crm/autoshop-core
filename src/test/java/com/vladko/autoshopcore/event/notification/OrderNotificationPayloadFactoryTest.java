package com.vladko.autoshopcore.event.notification;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderNotificationPayloadFactoryTest {

    private final OrderNotificationPayloadFactory factory =
            new OrderNotificationPayloadFactory(new OrderNumberFormatter());

    @Test
    void orderCreatedShouldBuildNotificationPayload() {
        OrderCreatedNotificationPayload payload = factory.orderCreated(order());

        assertThat(payload.orderId()).isEqualTo(42L);
        assertThat(payload.orderNumber()).isEqualTo("AS-2026-00042");
        assertThat(payload.customerId()).isEqualTo(7L);
        assertThat(payload.customerFirstName()).isEqualTo("Ivan");
        assertThat(payload.customerLastName()).isEqualTo("Petrov");
        assertThat(payload.customerEmail()).isEqualTo("ivan@example.com");
        assertThat(payload.vehicleId()).isEqualTo(12L);
        assertThat(payload.vehicleBrand()).isEqualTo("Toyota");
        assertThat(payload.vehicleModel()).isEqualTo("Camry");
        assertThat(payload.vehiclePlateNumber()).isEqualTo("A123BC77");
        assertThat(payload.createdAt()).isEqualTo(Instant.parse("2026-04-22T10:15:30Z"));
    }

    @Test
    void orderStatusChangedShouldBuildNotificationPayload() {
        OrderStatusChangedNotificationPayload payload = factory.orderStatusChanged(
                order(),
                OrderStatus.NEW,
                OrderStatus.IN_PROGRESS,
                null
        );

        assertThat(payload.orderId()).isEqualTo(42L);
        assertThat(payload.orderNumber()).isEqualTo("AS-2026-00042");
        assertThat(payload.previousStatus()).isEqualTo("NEW");
        assertThat(payload.newStatus()).isEqualTo("IN_PROGRESS");
        assertThat(payload.managerComment()).isEmpty();
        assertThat(payload.changedAt()).isEqualTo(Instant.parse("2026-04-22T12:00:00Z"));
    }

    @Test
    void orderStatusChangedShouldRejectSameStatus() {
        assertThatThrownBy(() -> factory.orderStatusChanged(order(), OrderStatus.NEW, OrderStatus.NEW, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order status must change before notification payload is created");
    }

    @Test
    void orderCompletedShouldBuildNotificationPayload() {
        OrderCompletedNotificationPayload payload = factory.orderCompleted(order(), 625);

        assertThat(payload.orderId()).isEqualTo(42L);
        assertThat(payload.orderNumber()).isEqualTo("AS-2026-00042");
        assertThat(payload.completedAt()).isEqualTo(Instant.parse("2026-04-22T17:45:00Z"));
        assertThat(payload.finalAmount()).isEqualByComparingTo("12500.00");
        assertThat(payload.currency()).isEqualTo("RUB");
        assertThat(payload.loyaltyPointsEarned()).isEqualTo(625);
    }

    @Test
    void orderCreatedShouldRejectBlankCustomerEmail() {
        Order order = order();
        order.getCustomer().setEmail(" ");

        assertThatThrownBy(() -> factory.orderCreated(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customer.email must not be blank");
    }

    private Order order() {
        Customer customer = Customer.builder()
                .id(7)
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .build();
        Vehicle vehicle = Vehicle.builder()
                .id(12)
                .customer(customer)
                .brand("Toyota")
                .model("Camry")
                .licensePlate("A123BC77")
                .build();
        return Order.builder()
                .id(42)
                .customer(customer)
                .vehicle(vehicle)
                .status(OrderStatus.COMPLETED)
                .createdAt(Instant.parse("2026-04-22T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-22T12:00:00Z"))
                .completedAt(Instant.parse("2026-04-22T17:45:00Z"))
                .finalAmount(new BigDecimal("12500.00"))
                .build();
    }
}
