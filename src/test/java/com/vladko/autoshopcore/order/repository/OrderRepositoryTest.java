package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import com.vladko.autoshopcore.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void findAllByCustomerIdOrderByIdDescShouldReturnCustomerOrders() {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .email("order-customer@example.com")
                .phoneNumber("+79990000041")
                .build());
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV1111111")
                .licensePlate("A111AA77")
                .build());

        orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Oil change")
                .status(OrderStatus.NEW)
                .build());
        orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Brake diagnostics")
                .status(OrderStatus.IN_PROGRESS)
                .build());

        List<Order> orders = orderRepository.findAllByCustomerIdOrderByIdDesc(customer.getId());

        assertThat(orders)
                .hasSize(2)
                .extracting(Order::getProblem)
                .containsExactly("Brake diagnostics", "Oil change");
    }

    @Test
    void findAllByStatusInOrderByIdDescShouldReturnActiveOrders() {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Olga")
                .lastName("Sidorova")
                .email("active-order@example.com")
                .phoneNumber("+79990000042")
                .build());
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .brand("Skoda")
                .model("Octavia")
                .vin("TMBDC41U962654321")
                .licensePlate("B222BB77")
                .build());

        orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build());
        orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Suspension repair")
                .status(OrderStatus.IN_PROGRESS)
                .build());
        orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Completed work")
                .status(OrderStatus.COMPLETED)
                .build());

        List<Order> orders = orderRepository.findAllByStatusInOrderByIdDesc(
                List.of(OrderStatus.NEW, OrderStatus.IN_PROGRESS)
        );

        assertThat(orders)
                .hasSize(2)
                .extracting(Order::getStatus)
                .containsExactly(OrderStatus.IN_PROGRESS, OrderStatus.NEW);
    }

    @Test
    void findAllByVehicleIdOrderByIdDescShouldReturnVehicleOrders() {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Sergey")
                .lastName("Morozov")
                .email("vehicle-order@example.com")
                .phoneNumber("+79990000043")
                .build());
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .brand("BMW")
                .model("X5")
                .vin("WBAFB71010LX54321")
                .licensePlate("C333CC77")
                .build());

        orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Transmission diagnostics")
                .status(OrderStatus.NEW)
                .build());
        orderRepository.save(Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .problem("Wheel alignment")
                .status(OrderStatus.IN_PROGRESS)
                .build());

        List<Order> orders = orderRepository.findAllByVehicleIdOrderByIdDesc(vehicle.getId());

        assertThat(orders)
                .hasSize(2)
                .extracting(Order::getProblem)
                .containsExactly("Wheel alignment", "Transmission diagnostics");
    }
}
