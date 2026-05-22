package com.vladko.autoshopcore.parts.repository;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.parts.entity.OrderPartItem;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import com.vladko.autoshopcore.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderPartItemRepositoryTest {

    @Autowired
    private OrderPartItemRepository orderPartItemRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void findAllByOrderIdOrderByIdAscShouldReturnOrderItems() {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .email("parts-order@example.com")
                .phoneNumber("+79990000101")
                .build());
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0010101")
                .licensePlate("A101AA77")
                .build());
        Order order = orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build());
        Part firstPart = partRepository.save(Part.builder()
                .brand("Bosch")
                .name("Oil filter")
                .articleNumber("OF-123")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(10)
                .reservedQuantity(0)
                .build());
        Part secondPart = partRepository.save(Part.builder()
                .brand("Mann")
                .name("Air filter")
                .articleNumber("AF-456")
                .cost(new BigDecimal("20.00"))
                .stockQuantity(10)
                .reservedQuantity(0)
                .build());

        orderPartItemRepository.save(OrderPartItem.builder()
                .order(order)
                .part(firstPart)
                .quantity(1)
                .unitPrice(new BigDecimal("15.50"))
                .build());
        orderPartItemRepository.save(OrderPartItem.builder()
                .order(order)
                .part(secondPart)
                .quantity(2)
                .unitPrice(new BigDecimal("20.00"))
                .build());

        List<OrderPartItem> items = orderPartItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());

        assertThat(items)
                .hasSize(2)
                .extracting(item -> item.getPart().getArticleNumber())
                .containsExactly("OF-123", "AF-456");
    }

    @Test
    void orderAndPartCombinationShouldBeUnique() {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Olga")
                .lastName("Sidorova")
                .email("parts-unique@example.com")
                .phoneNumber("+79990000102")
                .build());
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .brand("Skoda")
                .model("Octavia")
                .vin("TMBDC41U962001020")
                .licensePlate("B202BB77")
                .build());
        Order order = orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build());
        Part part = partRepository.save(Part.builder()
                .brand("Bosch")
                .name("Oil filter")
                .articleNumber("OF-789")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(10)
                .reservedQuantity(0)
                .build());

        orderPartItemRepository.saveAndFlush(OrderPartItem.builder()
                .order(order)
                .part(part)
                .quantity(1)
                .unitPrice(new BigDecimal("15.50"))
                .build());

        assertThatThrownBy(() -> orderPartItemRepository.saveAndFlush(OrderPartItem.builder()
                .order(order)
                .part(part)
                .quantity(2)
                .unitPrice(new BigDecimal("15.50"))
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
