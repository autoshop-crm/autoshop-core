package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerNotFoundException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.order.dto.OrderAssignmentDTO;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderEstimateUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderUpdateDTO;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.exception.EmployeeNotFoundException;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import com.vladko.autoshopcore.vehicle.exception.VehicleNotFoundException;
import com.vladko.autoshopcore.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createShouldPersistOrderForConsistentCustomerAndVehicle() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(1)
                .vehicleId(2)
                .problem("  Engine diagnostics ")
                .build();
        Order savedOrder = Order.builder()
                .id(10)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Engine diagnostics")
                .status(OrderStatus.NEW)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .createdAt(Instant.parse("2026-04-14T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-14T10:15:30Z"))
                .build();

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(2)).thenReturn(Optional.of(vehicle));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponseDTO response = orderService.create(dto);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order orderToSave = captor.getValue();

        assertThat(orderToSave.getCustomer()).isEqualTo(customer);
        assertThat(orderToSave.getVehicle()).isEqualTo(vehicle);
        assertThat(orderToSave.getProblem()).isEqualTo("Engine diagnostics");
        assertThat(orderToSave.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(orderToSave.getCostsTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getId()).isEqualTo(10);
    }

    @Test
    void createShouldThrowWhenCustomerMissing() {
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(404)
                .vehicleId(2)
                .problem("Diagnostics")
                .build();

        when(customerRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(dto))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer with id '404' was not found");
    }

    @Test
    void createShouldThrowWhenVehicleMissing() {
        Customer customer = Customer.builder().id(1).build();
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(1)
                .vehicleId(404)
                .problem("Diagnostics")
                .build();

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(dto))
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessage("Vehicle with id '404' was not found");
    }

    @Test
    void createShouldThrowWhenVehicleBelongsToAnotherCustomer() {
        Customer requestCustomer = Customer.builder().id(1).build();
        Customer ownerCustomer = Customer.builder().id(2).build();
        Vehicle vehicle = Vehicle.builder().id(3).customer(ownerCustomer).build();
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(1)
                .vehicleId(3)
                .problem("Diagnostics")
                .build();

        when(customerRepository.findById(1)).thenReturn(Optional.of(requestCustomer));
        when(vehicleRepository.findById(3)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> orderService.create(dto))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Vehicle does not belong to the specified customer");
    }

    @Test
    void createShouldThrowWhenEmployeeMissing() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(1)
                .vehicleId(2)
                .employeeId(77)
                .problem("Diagnostics")
                .build();

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(2)).thenReturn(Optional.of(vehicle));
        when(employeeRepository.findById(77)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(dto))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("Employee with id '77' was not found");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getByIdShouldThrowWhenOrderMissing() {
        when(orderRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(404))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order with id '404' was not found");
    }

    @Test
    void updateStatusShouldAllowNewToInProgress() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(33).function(EmployeeType.MECHANIC).build();
        Order existingOrder = Order.builder()
                .id(5)
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();
        Order updatedOrder = Order.builder()
                .id(5)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.IN_PROGRESS)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(5)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

        OrderResponseDTO response = orderService.updateStatus(5, new OrderStatusUpdateDTO(OrderStatus.IN_PROGRESS));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    void updateStatusShouldSetCompletedAtForCompletedStatus() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(44).function(EmployeeType.MECHANIC).build();
        Order existingOrder = Order.builder()
                .id(7)
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem("Diagnostics")
                .status(OrderStatus.IN_PROGRESS)
                .costsTotal(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("100.00"))
                .build();
        Order completedOrder = Order.builder()
                .id(7)
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem("Diagnostics")
                .status(OrderStatus.COMPLETED)
                .costsTotal(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("100.00"))
                .completedAt(Instant.parse("2026-04-14T11:15:30Z"))
                .build();

        when(orderRepository.findById(7)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(completedOrder);

        OrderResponseDTO response = orderService.updateStatus(7, new OrderStatusUpdateDTO(OrderStatus.COMPLETED));

        assertThat(existingOrder.getCompletedAt()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isEqualTo(Instant.parse("2026-04-14T11:15:30Z"));
    }

    @Test
    void updateStatusShouldAllowCancellation() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(9)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();
        Order cancelledOrder = Order.builder()
                .id(9)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.CANCELLED)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .completedAt(null)
                .build();

        when(orderRepository.findById(9)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(cancelledOrder);

        OrderResponseDTO response = orderService.updateStatus(9, new OrderStatusUpdateDTO(OrderStatus.CANCELLED));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getCompletedAt()).isNull();
    }

    @Test
    void updateStatusShouldRejectInvalidTransition() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(12)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();

        when(orderRepository.findById(12)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.updateStatus(12, new OrderStatusUpdateDTO(OrderStatus.COMPLETED)))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Cannot transition order status from 'NEW' to 'COMPLETED'");
    }

    @Test
    void updateShouldRejectTerminalOrder() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(13)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findById(13)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.update(13, OrderUpdateDTO.builder().problem("New text").build()))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order in status 'CANCELLED' can no longer be updated");
    }

    @Test
    void updateShouldChangeOnlyProblem() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(14)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();
        Order updatedOrder = Order.builder()
                .id(14)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Updated problem")
                .status(OrderStatus.NEW)
                .build();

        when(orderRepository.findById(14)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

        OrderResponseDTO response = orderService.update(14, OrderUpdateDTO.builder()
                .problem(" Updated problem ")
                .build());

        assertThat(existingOrder.getProblem()).isEqualTo("Updated problem");
        assertThat(existingOrder.getEmployee()).isNull();
        assertThat(response.getEmployeeId()).isNull();
    }

    @Test
    void assignEmployeeShouldAllowMechanic() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(21).function(EmployeeType.MECHANIC).build();
        Order existingOrder = Order.builder()
                .id(15)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();
        Order updatedOrder = Order.builder()
                .id(15)
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();

        when(orderRepository.findById(15)).thenReturn(Optional.of(existingOrder));
        when(employeeRepository.findById(21)).thenReturn(Optional.of(employee));
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

        OrderResponseDTO response = orderService.assignEmployee(15, new OrderAssignmentDTO(21));

        assertThat(existingOrder.getEmployee()).isEqualTo(employee);
        assertThat(response.getEmployeeId()).isEqualTo(21);
    }

    @Test
    void assignEmployeeShouldAllowManager() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(22).function(EmployeeType.MANAGER).build();
        Order existingOrder = Order.builder()
                .id(16)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();
        Order updatedOrder = Order.builder()
                .id(16)
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();

        when(orderRepository.findById(16)).thenReturn(Optional.of(existingOrder));
        when(employeeRepository.findById(22)).thenReturn(Optional.of(employee));
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

        OrderResponseDTO response = orderService.assignEmployee(16, new OrderAssignmentDTO(22));

        assertThat(existingOrder.getEmployee()).isEqualTo(employee);
        assertThat(response.getEmployeeId()).isEqualTo(22);
    }

    @Test
    void assignEmployeeShouldRejectUnsupportedEmployeeType() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(23).function(EmployeeType.RECEPTIONIST).build();
        Order existingOrder = Order.builder()
                .id(17)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();

        when(orderRepository.findById(17)).thenReturn(Optional.of(existingOrder));
        when(employeeRepository.findById(23)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> orderService.assignEmployee(17, new OrderAssignmentDTO(23)))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Only mechanic or manager can be assigned to order");
    }

    @Test
    void assignEmployeeShouldRejectTerminalOrder() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(18)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.COMPLETED)
                .build();

        when(orderRepository.findById(18)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.assignEmployee(18, new OrderAssignmentDTO(21)))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order in status 'COMPLETED' can no longer be updated");
    }

    @Test
    void updateEstimateShouldRecalculateFinalAmount() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(19)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();
        Order updatedOrder = Order.builder()
                .id(19)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .costsTotal(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("15.00"))
                .finalAmount(new BigDecimal("85.00"))
                .build();

        when(orderRepository.findById(19)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

        OrderResponseDTO response = orderService.updateEstimate(19, new OrderEstimateUpdateDTO(
                new BigDecimal("100.00"),
                new BigDecimal("15.00")
        ));

        assertThat(existingOrder.getCostsTotal()).isEqualByComparingTo("100.00");
        assertThat(existingOrder.getDiscountAmount()).isEqualByComparingTo("15.00");
        assertThat(existingOrder.getFinalAmount()).isEqualByComparingTo("85.00");
        assertThat(response.getFinalAmount()).isEqualByComparingTo("85.00");
    }

    @Test
    void updateEstimateShouldRejectNegativeValues() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(20)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();

        when(orderRepository.findById(20)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.updateEstimate(20, new OrderEstimateUpdateDTO(
                new BigDecimal("-1.00"),
                BigDecimal.ZERO
        )))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Estimate amounts cannot be negative");
    }

    @Test
    void updateEstimateShouldRejectDiscountGreaterThanCosts() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(21)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .build();

        when(orderRepository.findById(21)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.updateEstimate(21, new OrderEstimateUpdateDTO(
                new BigDecimal("100.00"),
                new BigDecimal("120.00")
        )))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Discount amount cannot exceed total costs");
    }

    @Test
    void updateStatusShouldRejectNewToInProgressWithoutEmployee() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(22)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(22)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.updateStatus(22, new OrderStatusUpdateDTO(OrderStatus.IN_PROGRESS)))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order must have an assigned mechanic before moving to IN_PROGRESS");
    }

    @Test
    void updateStatusShouldRejectNewToInProgressWithManagerOnly() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(24).function(EmployeeType.MANAGER).build();
        Order existingOrder = Order.builder()
                .id(23)
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(23)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.updateStatus(23, new OrderStatusUpdateDTO(OrderStatus.IN_PROGRESS)))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order must have an assigned mechanic before moving to IN_PROGRESS");
    }

    @Test
    void updateStatusShouldRejectCompletionWithoutEstimate() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(25).function(EmployeeType.MECHANIC).build();
        Order existingOrder = Order.builder()
                .id(24)
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem("Diagnostics")
                .status(OrderStatus.IN_PROGRESS)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(24)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.updateStatus(24, new OrderStatusUpdateDTO(OrderStatus.COMPLETED)))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order estimate must be calculated before moving to COMPLETED");
    }
}
