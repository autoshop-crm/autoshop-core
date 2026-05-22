package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.client.dto.CustomerCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.service.CustomerService;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.order.dto.OrderAssignmentDTO;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderEstimateUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import com.vladko.autoshopcore.order.service.OrderService;
import com.vladko.autoshopcore.parts.dto.OrderPartItemCreateDTO;
import com.vladko.autoshopcore.parts.dto.PartCreateDTO;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleCreateDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false"
})
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderPartsIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PartService partService;

    @Autowired
    private OrderPartItemService orderPartItemService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @Transactional
    void completedOrderShouldConvertReservationIntoStockWriteOffAndKeepFrozenPrice() {
        CustomerResponseDTO customer = customerService.create(CustomerCreateDTO.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .email("parts-int-complete@example.com")
                .phoneNumber("+79990000201")
                .build());
        VehicleResponseDTO vehicle = vehicleService.create(VehicleCreateDTO.builder()
                .customerId(customer.getId())
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0020202")
                .licensePlate("C303CC77")
                .build());
        OrderResponseDTO order = orderService.create(OrderCreateDTO.builder()
                .customerId(customer.getId())
                .vehicleId(vehicle.getId())
                .problem("Oil change")
                .build());
        Employee mechanic = employeeRepository.save(Employee.builder()
                .firstName("Petr")
                .lastName("Mechanic")
                .function(EmployeeType.MECHANIC)
                .build());

        orderService.assignEmployee(order.getId(), new OrderAssignmentDTO(mechanic.getId()));
        orderService.updateEstimate(order.getId(), new OrderEstimateUpdateDTO(new BigDecimal("100.00"), BigDecimal.ZERO));

        PartResponseDTO part = partService.create(PartCreateDTO.builder()
                .brand("Bosch")
                .name("Oil filter")
                .articleNumber("OF-COMPLETE")
                .cost(new BigDecimal("20.00"))
                .build());
        partService.updateStock(part.getId(), new com.vladko.autoshopcore.parts.dto.PartStockUpdateDTO(10));
        orderPartItemService.create(order.getId(), new OrderPartItemCreateDTO(part.getId(), 2));

        OrderResponseDTO beforeCompletion = orderService.getById(order.getId());
        PartResponseDTO partBeforeCompletion = partService.getById(part.getId());

        assertThat(beforeCompletion.getLaborTotal()).isEqualByComparingTo("100.00");
        assertThat(beforeCompletion.getPartsTotal()).isEqualByComparingTo("40.00");
        assertThat(beforeCompletion.getCostsTotal()).isEqualByComparingTo("140.00");
        assertThat(beforeCompletion.getFinalAmount()).isEqualByComparingTo("140.00");
        assertThat(partBeforeCompletion.getReservedQuantity()).isEqualTo(2);
        assertThat(partBeforeCompletion.getStockQuantity()).isEqualTo(10);

        partService.update(part.getId(), com.vladko.autoshopcore.parts.dto.PartUpdateDTO.builder()
                .cost(new BigDecimal("25.00"))
                .build());
        orderService.updateStatus(order.getId(), new OrderStatusUpdateDTO(OrderStatus.IN_PROGRESS));
        orderService.updateStatus(order.getId(), new OrderStatusUpdateDTO(OrderStatus.COMPLETED));

        OrderResponseDTO completedOrder = orderService.getById(order.getId());
        PartResponseDTO completedPart = partService.getById(part.getId());

        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completedOrder.getPartsTotal()).isEqualByComparingTo("40.00");
        assertThat(completedPart.getReservedQuantity()).isEqualTo(0);
        assertThat(completedPart.getStockQuantity()).isEqualTo(8);
    }

    @Test
    @Transactional
    void cancelledOrderShouldReleaseReservationWithoutWritingOffPhysicalStock() {
        CustomerResponseDTO customer = customerService.create(CustomerCreateDTO.builder()
                .firstName("Olga")
                .lastName("Sidorova")
                .email("parts-int-cancel@example.com")
                .phoneNumber("+79990000202")
                .build());
        VehicleResponseDTO vehicle = vehicleService.create(VehicleCreateDTO.builder()
                .customerId(customer.getId())
                .brand("Skoda")
                .model("Octavia")
                .vin("TMBDC41U962002022")
                .licensePlate("D404DD77")
                .build());
        OrderResponseDTO order = orderService.create(OrderCreateDTO.builder()
                .customerId(customer.getId())
                .vehicleId(vehicle.getId())
                .problem("Brake diagnostics")
                .build());

        PartResponseDTO part = partService.create(PartCreateDTO.builder()
                .brand("Mann")
                .name("Brake pad")
                .articleNumber("BP-CANCEL")
                .cost(new BigDecimal("35.00"))
                .build());
        partService.updateStock(part.getId(), new com.vladko.autoshopcore.parts.dto.PartStockUpdateDTO(6));
        orderPartItemService.create(order.getId(), new OrderPartItemCreateDTO(part.getId(), 3));

        orderService.updateStatus(order.getId(), new OrderStatusUpdateDTO(OrderStatus.CANCELLED));

        OrderResponseDTO cancelledOrder = orderService.getById(order.getId());
        PartResponseDTO cancelledPart = partService.getById(part.getId());

        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.getPartsTotal()).isEqualByComparingTo("105.00");
        assertThat(cancelledPart.getReservedQuantity()).isEqualTo(0);
        assertThat(cancelledPart.getStockQuantity()).isEqualTo(6);
    }
}
