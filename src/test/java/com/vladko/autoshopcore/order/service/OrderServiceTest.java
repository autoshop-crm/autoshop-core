package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerNotFoundException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.event.notification.OrderCompletedNotificationPayload;
import com.vladko.autoshopcore.event.notification.OrderCreatedNotificationPayload;
import com.vladko.autoshopcore.event.notification.OrderNotificationPayloadFactory;
import com.vladko.autoshopcore.event.notification.OrderStatusChangedNotificationPayload;
import com.vladko.autoshopcore.loyalty.service.CrmLoyaltyFacade;
import com.vladko.autoshopcore.order.dto.OrderAssignmentDTO;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderEstimateUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderUpdateDTO;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.event.OrderCompletedDomainEvent;
import com.vladko.autoshopcore.order.event.OrderCreatedDomainEvent;
import com.vladko.autoshopcore.order.event.OrderStatusChangedDomainEvent;
import com.vladko.autoshopcore.order.exception.EmployeeNotFoundException;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderAvailabilityProjection;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.repository.OrderServiceItemRepository;
import com.vladko.autoshopcore.order.timeline.service.OrderTimelineService;
import com.vladko.autoshopcore.parts.service.OrderPartInventoryCoordinator;
import com.vladko.autoshopcore.security.AuthenticatedUser;
import com.vladko.autoshopcore.security.CoreSecurityService;
import com.vladko.autoshopcore.servicecatalog.repository.ServicesRepository;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import com.vladko.autoshopcore.vehicle.exception.VehicleNotFoundException;
import com.vladko.autoshopcore.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    @Mock
    private OrderFinancialsService orderFinancialsService;

    @Mock
    private OrderPartInventoryCoordinator orderPartInventoryCoordinator;

    @Mock
    private CrmLoyaltyFacade loyaltyService;

    @Mock
    private LegacyOrderStatusProjector legacyOrderStatusProjector;

    @Mock
    private OrderNotificationPayloadFactory orderNotificationPayloadFactory;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ServicesRepository servicesRepository;

    @Mock
    private OrderServiceItemRepository orderServiceItemRepository;

    @Mock
    private CoreSecurityService coreSecurityService;

    @Mock
    private OrderTimelineService orderTimelineService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        lenient().when(orderServiceItemRepository.findAllByOrderIdOrderByIdAsc(any(Integer.class))).thenReturn(List.of());
        lenient().when(coreSecurityService.currentActor()).thenReturn(new com.vladko.autoshopcore.security.CoreActor(1L, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.MANAGER));
        lenient().when(coreSecurityService.requireAnyStaff()).thenReturn(new com.vladko.autoshopcore.security.CoreActor(1L, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.MANAGER));
        lenient().when(coreSecurityService.requireRoles(any())).thenReturn(new com.vladko.autoshopcore.security.CoreActor(1L, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.MANAGER));
        lenient().doNothing().when(coreSecurityService).requireCustomerAccess(any());
    }

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createShouldPersistOrderForConsistentCustomerAndVehicle() {
        Customer customer = Customer.builder().id(1).firstName("Ivan").lastName("Petrov").email("ivan@test.com").phoneNumber("79990001122").build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).brand("BMW").model("X5").vin("VIN12345678901234").licensePlate("A123AA77").build();
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
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .createdAt(Instant.parse("2026-04-14T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-14T10:15:30Z"))
                .build();

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(2)).thenReturn(Optional.of(vehicle));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        OrderCreatedNotificationPayload payload = createdPayload();
        when(orderNotificationPayloadFactory.orderCreated(savedOrder)).thenReturn(payload);

        OrderResponseDTO response = orderService.create(dto);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(captor.capture());
        Order orderToSave = captor.getAllValues().get(0);

        assertThat(orderToSave.getCustomer()).isEqualTo(customer);
        assertThat(orderToSave.getVehicle()).isEqualTo(vehicle);
        assertThat(orderToSave.getProblem()).isEqualTo("Engine diagnostics");
        assertThat(orderToSave.getStatus()).isEqualTo(OrderStatus.NEW);
        verify(orderFinancialsService).initialize(orderToSave);
        ArgumentCaptor<OrderCreatedDomainEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedDomainEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().payload()).isEqualTo(payload);
        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getCustomerEmail()).isEqualTo("ivan@test.com");
        assertThat(response.getVehicleLicensePlate()).isEqualTo("A123AA77");
    }

    @Test
    void getMyOrdersShouldReturnOrdersForCurrentMechanic() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(7).email("test2@test.com").function(EmployeeType.MECHANIC).build();
        when(coreSecurityService.requireRoles("MECHANIC"))
                .thenReturn(new com.vladko.autoshopcore.security.CoreActor(7L, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.MECHANIC));
        when(employeeRepository.findByEmail("test2@test.com")).thenReturn(Optional.of(employee));
        when(orderRepository.findAllByEmployeeIdOrderByIdDesc(7)).thenReturn(List.of(
                Order.builder().id(31).customer(customer).vehicle(vehicle).employee(employee).problem("A").status(OrderStatus.NEW).laborTotal(BigDecimal.ZERO).partsTotal(BigDecimal.ZERO).costsTotal(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO).finalAmount(BigDecimal.ZERO).build(),
                Order.builder().id(30).customer(customer).vehicle(vehicle).employee(employee).problem("B").status(OrderStatus.ACCEPTED).laborTotal(BigDecimal.ZERO).partsTotal(BigDecimal.ZERO).costsTotal(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO).finalAmount(BigDecimal.ZERO).build()
        ));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(12L, "test2@test.com", java.util.Set.of("MECHANIC"), "jti", Instant.parse("2026-05-20T10:00:00Z")),
                null,
                java.util.List.of()
        ));

        var response = orderService.getMyOrders();

        assertThat(response).hasSize(2);
        assertThat(response).extracting(OrderResponseDTO::getId).containsExactly(31, 30);
    }

    @Test
    void createShouldRejectBusyEmployeeForSelectedSlot() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(7).function(EmployeeType.MECHANIC).build();
        OrderCreateDTO dto = OrderCreateDTO.builder()
                .customerId(1)
                .vehicleId(2)
                .employeeId(7)
                .problem("Diagnostics")
                .plannedVisitAt(Instant.parse("2026-05-20T10:00:00Z"))
                .plannedSlotMinutes(90)
                .build();

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(2)).thenReturn(Optional.of(vehicle));
        when(employeeRepository.findById(7)).thenReturn(Optional.of(employee));
        when(orderRepository.findFirstAvailabilityConflict(eq(7), isNull(), any(), any(), any()))
                .thenReturn(List.of(new Projection(99, 7, Instant.parse("2026-05-20T10:30:00Z"), 60, OrderStatus.ACCEPTED)));

        assertThatThrownBy(() -> orderService.create(dto))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Employee is not available for the selected time slot");
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
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
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
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(5)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);
        OrderStatusChangedNotificationPayload payload = statusChangedPayload(OrderStatus.NEW, OrderStatus.IN_PROGRESS);
        when(orderNotificationPayloadFactory.orderStatusChanged(updatedOrder, OrderStatus.NEW, OrderStatus.IN_PROGRESS, ""))
                .thenReturn(payload);

        OrderResponseDTO response = orderService.updateStatus(5, new OrderStatusUpdateDTO(OrderStatus.IN_PROGRESS));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        ArgumentCaptor<OrderStatusChangedDomainEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderStatusChangedDomainEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().payload()).isEqualTo(payload);
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
                .laborTotal(new BigDecimal("100.00"))
                .partsTotal(BigDecimal.ZERO)
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
                .laborTotal(new BigDecimal("100.00"))
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("100.00"))
                .completedAt(Instant.parse("2026-04-14T11:15:30Z"))
                .build();

        when(orderRepository.findById(7)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(completedOrder);
        when(loyaltyService.processOrderCompleted(existingOrder)).thenReturn(5);
        OrderStatusChangedNotificationPayload statusPayload =
                statusChangedPayload(OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED);
        OrderCompletedNotificationPayload completedPayload = completedPayload();
        when(orderNotificationPayloadFactory.orderStatusChanged(completedOrder, OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED, ""))
                .thenReturn(statusPayload);
        when(orderNotificationPayloadFactory.orderCompleted(completedOrder, 5)).thenReturn(completedPayload);

        OrderResponseDTO response = orderService.updateStatus(7, new OrderStatusUpdateDTO(OrderStatus.COMPLETED));

        verify(orderPartInventoryCoordinator).finalizeReservations(existingOrder);
        verify(loyaltyService).processOrderCompleted(existingOrder);
        assertThat(existingOrder.getCompletedAt()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isEqualTo(Instant.parse("2026-04-14T11:15:30Z"));
        verify(applicationEventPublisher).publishEvent(new OrderStatusChangedDomainEvent(statusPayload));
        verify(applicationEventPublisher).publishEvent(new OrderCompletedDomainEvent(completedPayload));
    }

    @Test
    void updateStatusShouldSkipEventsWhenStatusDoesNotChange() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Order existingOrder = Order.builder()
                .id(8)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.NEW)
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(8)).thenReturn(Optional.of(existingOrder));

        OrderResponseDTO response = orderService.updateStatus(8, new OrderStatusUpdateDTO(OrderStatus.NEW));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.NEW);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(orderNotificationPayloadFactory, applicationEventPublisher);
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
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
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
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .completedAt(null)
                .build();

        when(orderRepository.findById(9)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(cancelledOrder);

        OrderResponseDTO response = orderService.updateStatus(9, new OrderStatusUpdateDTO(OrderStatus.CANCELLED));

        verify(orderPartInventoryCoordinator).releaseReservations(existingOrder);
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
        verifyNoInteractions(orderNotificationPayloadFactory, applicationEventPublisher);
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
        Customer customer = Customer.builder().id(1).firstName("Ivan").lastName("Petrov").email("ivan@test.com").phoneNumber("79990001122").build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).brand("BMW").model("X5").vin("VIN12345678901234").licensePlate("A123AA77").build();
        Employee employee = Employee.builder().id(22).firstName("Anna").lastName("Manager").email("anna@test.com").function(EmployeeType.MANAGER).build();
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
        assertThat(response.getEmployeeEmail()).isEqualTo("anna@test.com");
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
    void assignEmployeeShouldRejectBusyEmployeeForSelectedSlot() {
        Customer customer = Customer.builder().id(1).build();
        Vehicle vehicle = Vehicle.builder().id(2).customer(customer).build();
        Employee employee = Employee.builder().id(21).function(EmployeeType.MECHANIC).build();
        Order existingOrder = Order.builder()
                .id(20)
                .customer(customer)
                .vehicle(vehicle)
                .problem("Diagnostics")
                .status(OrderStatus.WAITING_FOR_VISIT)
                .plannedVisitAt(Instant.parse("2026-05-20T10:00:00Z"))
                .plannedSlotMinutes(60)
                .build();

        when(orderRepository.findById(20)).thenReturn(Optional.of(existingOrder));
        when(employeeRepository.findById(21)).thenReturn(Optional.of(employee));
        when(orderRepository.findFirstAvailabilityConflict(eq(21), eq(20), any(), any(), any()))
                .thenReturn(List.of(new Projection(101, 21, Instant.parse("2026-05-20T10:30:00Z"), 60, OrderStatus.ACCEPTED)));

        assertThatThrownBy(() -> orderService.assignEmployee(20, new OrderAssignmentDTO(21)))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Employee is not available for the selected time slot");
    }

    private record Projection(Integer id, Integer employeeId, Instant plannedVisitAt, Integer plannedSlotMinutes,
                              OrderStatus status) implements OrderAvailabilityProjection {
        @Override public Integer getId() { return id; }
        @Override public Integer getEmployeeId() { return employeeId; }
        @Override public Instant getPlannedVisitAt() { return plannedVisitAt; }
        @Override public Integer getPlannedSlotMinutes() { return plannedSlotMinutes; }
        @Override public OrderStatus getStatus() { return status; }
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
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
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
                .laborTotal(new BigDecimal("100.00"))
                .partsTotal(BigDecimal.ZERO)
                .costsTotal(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("15.00"))
                .finalAmount(new BigDecimal("85.00"))
                .build();

        when(orderRepository.findById(19)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setLaborTotal(new BigDecimal("100.00"));
            order.setPartsTotal(BigDecimal.ZERO);
            order.setCostsTotal(new BigDecimal("100.00"));
            order.setDiscountAmount(new BigDecimal("15.00"));
            order.setFinalAmount(new BigDecimal("85.00"));
            return null;
        }).when(orderFinancialsService).updateEstimate(existingOrder, new BigDecimal("100.00"), new BigDecimal("15.00"));

        OrderResponseDTO response = orderService.updateEstimate(19, new OrderEstimateUpdateDTO(
                new BigDecimal("100.00"),
                new BigDecimal("15.00")
        ));

        verify(orderFinancialsService).updateEstimate(existingOrder, new BigDecimal("100.00"), new BigDecimal("15.00"));
        assertThat(existingOrder.getLaborTotal()).isEqualByComparingTo("100.00");
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
        doThrow(new OrderConflictException("Estimate amounts cannot be negative"))
                .when(orderFinancialsService).updateEstimate(existingOrder, new BigDecimal("-1.00"), BigDecimal.ZERO);

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
        doThrow(new OrderConflictException("Discount amount cannot exceed total costs"))
                .when(orderFinancialsService).updateEstimate(existingOrder, new BigDecimal("100.00"), new BigDecimal("120.00"));

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
        verifyNoInteractions(orderNotificationPayloadFactory, applicationEventPublisher);
    }

    private OrderCreatedNotificationPayload createdPayload() {
        return new OrderCreatedNotificationPayload(
                10L,
                "AS-2026-00010",
                1L,
                "Ivan",
                "Petrov",
                "ivan@example.com",
                2L,
                "Toyota",
                "Camry",
                "A123BC77",
                Instant.parse("2026-04-14T10:15:30Z")
        );
    }

    private OrderStatusChangedNotificationPayload statusChangedPayload(OrderStatus previousStatus, OrderStatus newStatus) {
        return new OrderStatusChangedNotificationPayload(
                5L,
                "AS-2026-00005",
                1L,
                "Ivan",
                "Petrov",
                "ivan@example.com",
                previousStatus.name(),
                newStatus.name(),
                Instant.parse("2026-04-14T11:15:30Z"),
                ""
        );
    }

    private OrderCompletedNotificationPayload completedPayload() {
        return new OrderCompletedNotificationPayload(
                7L,
                "AS-2026-00007",
                1L,
                "Ivan",
                "Petrov",
                "ivan@example.com",
                Instant.parse("2026-04-14T11:15:30Z"),
                new BigDecimal("100.00"),
                "RUB",
                5
        );
    }
}
