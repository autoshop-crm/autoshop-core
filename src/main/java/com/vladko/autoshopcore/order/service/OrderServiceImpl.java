package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerNotFoundException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.entities.OrderServiceItem;
import com.vladko.autoshopcore.entities.Services;
import com.vladko.autoshopcore.event.notification.OrderCompletedNotificationPayload;
import com.vladko.autoshopcore.event.notification.OrderCreatedNotificationPayload;
import com.vladko.autoshopcore.event.notification.OrderNotificationPayloadFactory;
import com.vladko.autoshopcore.event.notification.OrderStatusChangedNotificationPayload;
import com.vladko.autoshopcore.loyalty.service.CrmLoyaltyFacade;
import com.vladko.autoshopcore.order.dto.OrderAssignmentDTO;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderEstimateUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderServiceLineDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderUpdateDTO;
import com.vladko.autoshopcore.order.entity.BookingChannel;
import com.vladko.autoshopcore.order.entity.CancellationReason;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.service.LegacyOrderStatusProjector;
import com.vladko.autoshopcore.order.event.OrderCompletedDomainEvent;
import com.vladko.autoshopcore.order.event.OrderCreatedDomainEvent;
import com.vladko.autoshopcore.order.event.OrderStatusChangedDomainEvent;
import com.vladko.autoshopcore.order.exception.EmployeeNotFoundException;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.exception.OrderConflictException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.repository.OrderServiceItemRepository;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEventType;
import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineVisibility;
import com.vladko.autoshopcore.order.timeline.service.OrderTimelineService;
import com.vladko.autoshopcore.parts.service.OrderPartInventoryCoordinator;
import com.vladko.autoshopcore.security.CoreActor;
import com.vladko.autoshopcore.security.CoreSecurityService;
import com.vladko.autoshopcore.servicecatalog.repository.ServicesRepository;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import com.vladko.autoshopcore.vehicle.exception.VehicleNotFoundException;
import com.vladko.autoshopcore.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final EnumSet<OrderStatus> BOOKING_STATUSES = EnumSet.of(
            OrderStatus.WAITING_FOR_VISIT,
            OrderStatus.ACCEPTED,
            OrderStatus.DIAGNOSIS_IN_PROGRESS,
            OrderStatus.WAITING_FOR_OWNER_APPROVAL,
            OrderStatus.WAITING_FOR_PART,
            OrderStatus.REPAIR_IN_PROGRESS,
            OrderStatus.READY_FOR_OWNER
    );

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderFinancialsService orderFinancialsService;
    private final OrderPartInventoryCoordinator orderPartInventoryCoordinator;
    private final CrmLoyaltyFacade loyaltyService;
    private final LegacyOrderStatusProjector legacyOrderStatusProjector;
    private final OrderNotificationPayloadFactory orderNotificationPayloadFactory;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ServicesRepository servicesRepository;
    private final OrderServiceItemRepository orderServiceItemRepository;
    private final CoreSecurityService coreSecurityService;
    private final OrderTimelineService orderTimelineService;

    @Override
    @Transactional
    public OrderResponseDTO create(OrderCreateDTO dto) {
        coreSecurityService.requireRoles("ADMIN", "MANAGER", "RECEPTIONIST");
        return createInternal(dto, Boolean.TRUE.equals(dto.getImmediateDropOff()));
    }

    @Override
    @Transactional
    public OrderResponseDTO createImmediateDropOff(OrderCreateDTO dto) {
        coreSecurityService.requireRoles("ADMIN", "MANAGER", "RECEPTIONIST");
        return createInternal(dto, true);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getById(Integer id) {
        Order order = findOrder(id);
        coreSecurityService.requireCustomerAccess(order);
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO update(Integer id, OrderUpdateDTO dto) {
        coreSecurityService.requireRoles("ADMIN", "MANAGER", "RECEPTIONIST");
        Order order = findOrder(id);
        ensureOrderIsMutable(order);

        String normalizedProblem = normalizeOptionalText(dto.getProblem());
        if (dto.getProblem() != null) {
            order.setProblem(normalizedProblem == null ? order.getProblem() : normalizedProblem);
        }
        if (dto.getPlannedVisitAt() != null) {
            ensurePlannedVisitMutable(order);
            order.setPlannedVisitAt(dto.getPlannedVisitAt());
        }
        if (dto.getPlannedSlotMinutes() != null) {
            order.setPlannedSlotMinutes(dto.getPlannedSlotMinutes());
        }
        if (dto.getBookingChannel() != null) {
            order.setBookingChannel(dto.getBookingChannel());
        }
        if (dto.getIntakeNotes() != null) {
            order.setIntakeNotes(normalizeOptionalText(dto.getIntakeNotes()));
        }
        if (dto.getRequiresOwnerApprovalForEveryExtraWork() != null) {
            order.setRequiresOwnerApprovalForEveryExtraWork(dto.getRequiresOwnerApprovalForEveryExtraWork());
        }
        Order saved = orderRepository.save(order);
        replaceServiceLines(saved, dto.getSelectedServiceIds());
        repriceLaborFromSelectedServices(saved);
        return mapToResponse(orderRepository.save(saved));
    }

    @Override
    @Transactional
    public OrderResponseDTO assignEmployee(Integer id, OrderAssignmentDTO dto) {
        coreSecurityService.requireRoles("ADMIN", "MANAGER");
        Order order = findOrder(id);
        ensureOrderIsMutable(order);
        order.setEmployee(dto.getEmployeeId() == null ? null : findAssignableEmployee(dto.getEmployeeId()));
        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO updateEstimate(Integer id, OrderEstimateUpdateDTO dto) {
        coreSecurityService.requireRoles("ADMIN", "MANAGER", "MECHANIC");
        Order order = findOrder(id);
        ensureEstimateIsEditable(order);
        orderFinancialsService.updateEstimate(order, dto.getLaborTotal(), dto.getDiscountAmount());
        loyaltyService.refreshAppliedPointsAfterOrderChange(order);
        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO updateStatus(Integer id, OrderStatusUpdateDTO dto) {
        Order order = findOrder(id);
        requireRoleForStatusTransition(order, dto.getStatus());
        OrderStatus previousStatus = order.getStatus();
        OrderStatus targetStatus = dto.getStatus();
        if (previousStatus == targetStatus) {
            return mapToResponse(order);
        }

        validateStatusTransition(previousStatus, targetStatus);
        applyStatusGuards(order, targetStatus);
        applyStatusSideEffects(order, targetStatus, dto.getCancellationReason());

        Integer loyaltyPointsEarned = null;
        if (isCompletionLike(targetStatus)) {
            orderPartInventoryCoordinator.finalizeReservations(order);
            loyaltyPointsEarned = loyaltyService.processOrderCompleted(order);
        } else if (isCancellationLike(targetStatus)) {
            orderPartInventoryCoordinator.releaseReservations(order);
            loyaltyService.processOrderCancelled(order);
        }

        order.setStatus(targetStatus);
        Order savedOrder = orderRepository.save(order);
        OrderStatusChangedNotificationPayload statusChangedPayload = orderNotificationPayloadFactory.orderStatusChanged(
                savedOrder,
                previousStatus,
                targetStatus,
                ""
        );
        applicationEventPublisher.publishEvent(new OrderStatusChangedDomainEvent(statusChangedPayload));

        CoreActor actor = safeActor();
        orderTimelineService.append(savedOrder, OrderTimelineEventType.STATUS_CHANGED, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), savedOrder.getStatus(), "Order status changed to " + savedOrder.getStatus().name(), null, "status-" + savedOrder.getId() + "-" + savedOrder.getStatus() + "-" + savedOrder.getUpdatedAt());
        if (targetStatus == OrderStatus.READY_FOR_OWNER) {
            orderTimelineService.append(savedOrder, OrderTimelineEventType.READY_FOR_OWNER_MARKED, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), savedOrder.getStatus(), "Order is ready for owner", null, "ready-" + savedOrder.getId() + "-" + savedOrder.getReadyForOwnerAt());
        }
        if (isCancellationLike(targetStatus)) {
            orderTimelineService.append(savedOrder, OrderTimelineEventType.ORDER_CANCELLED, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), savedOrder.getStatus(), "Order cancelled", null, "cancelled-" + savedOrder.getId() + "-" + savedOrder.getCancelledAt());
        }
        if (isCompletionLike(targetStatus)) {
            OrderCompletedNotificationPayload completedPayload = orderNotificationPayloadFactory.orderCompleted(
                    savedOrder,
                    loyaltyPointsEarned == null ? 0 : loyaltyPointsEarned
            );
            applicationEventPublisher.publishEvent(new OrderCompletedDomainEvent(completedPayload));
        }

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO checkInVehicle(Integer id) {
        coreSecurityService.requireRoles("ADMIN", "MANAGER", "RECEPTIONIST");
        Order order = findOrder(id);
        if (order.getStatus() != OrderStatus.WAITING_FOR_VISIT) {
            throw new InvalidOrderStateException("Only waiting bookings can be checked in");
        }
        order.setCheckedInAt(Instant.now());
        order.setStatus(OrderStatus.ACCEPTED);
        Order saved = orderRepository.save(order);
        CoreActor actor = safeActor();
        orderTimelineService.append(saved, OrderTimelineEventType.VEHICLE_CHECKED_IN, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), saved.getStatus(), "Vehicle checked in", null, "checkin-" + saved.getId() + "-" + saved.getCheckedInAt());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<OrderResponseDTO> getAll() {
        return orderRepository.findAll().stream()
                .sorted((left, right) -> Integer.compare(right.getId(), left.getId()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelNoShow(Integer id) {
        coreSecurityService.requireRoles("ADMIN", "MANAGER", "RECEPTIONIST");
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (order.getStatus() == OrderStatus.CANCELLED_NO_SHOW) {
            return mapToResponse(order);
        }
        if (order.getCheckedInAt() != null) {
            throw new InvalidOrderStateException("Checked-in order cannot be cancelled as no-show");
        }
        if (order.getPlannedVisitAt() == null || !order.getPlannedVisitAt().isBefore(Instant.now())) {
            throw new InvalidOrderStateException("No-show requires an expired planned visit");
        }
        applyStatusSideEffects(order, OrderStatus.CANCELLED_NO_SHOW, CancellationReason.NO_SHOW);
        order.setStatus(OrderStatus.CANCELLED_NO_SHOW);
        loyaltyService.processOrderCancelled(order);
        Order saved = orderRepository.save(order);
        CoreActor actor = safeActor();
        orderTimelineService.append(saved, OrderTimelineEventType.ORDER_CANCELLED, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), saved.getStatus(), "Order cancelled as no-show", null, "no-show-" + saved.getId() + "-" + saved.getPlannedVisitAt());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByCustomerId(Integer customerId) {
        findCustomer(customerId);
        return orderRepository.findAllByCustomerIdOrderByIdDesc(customerId).stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByVehicleId(Integer vehicleId) {
        findVehicle(vehicleId);
        return orderRepository.findAllByVehicleIdOrderByIdDesc(vehicleId).stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByStatus(OrderStatus status) {
        return orderRepository.findAllByStatusOrderByIdDesc(status).stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getBookings(Instant from, Instant to) {
        return orderRepository.findBookingsByWindowAndStatuses(from, to, BOOKING_STATUSES).stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getDailyArrivals(LocalDate date) {
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return getBookings(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUnassignedBookings(LocalDate date) {
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return orderRepository.findAllByPlannedVisitAtBetweenAndEmployeeIsNullOrderByPlannedVisitAtAscIdAsc(from, to)
                .stream()
                .filter(order -> BOOKING_STATUSES.contains(order.getStatus()))
                .map(this::mapToResponse)
                .toList();
    }

    private OrderResponseDTO createInternal(OrderCreateDTO dto, boolean immediateDropOff) {
        Customer customer = findCustomer(dto.getCustomerId());
        Vehicle vehicle = findVehicle(dto.getVehicleId());
        validateCustomerVehicleConsistency(customer, vehicle);
        validateCreateInput(dto);

        Employee employee = dto.getEmployeeId() == null ? null : findAssignableEmployee(dto.getEmployeeId());
        OrderStatus initialStatus = immediateDropOff ? OrderStatus.ACCEPTED : dto.getPlannedVisitAt() != null ? OrderStatus.WAITING_FOR_VISIT : OrderStatus.NEW;
        Instant now = Instant.now();

        Order order = Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem(defaultProblem(dto.getProblem(), dto.getSelectedServiceIds()))
                .status(initialStatus)
                .plannedVisitAt(dto.getPlannedVisitAt())
                .plannedSlotMinutes(dto.getPlannedSlotMinutes())
                .bookingChannel(dto.getBookingChannel() == null ? BookingChannel.INTERNAL : dto.getBookingChannel())
                .intakeNotes(normalizeOptionalText(dto.getIntakeNotes()))
                .requiresOwnerApprovalForEveryExtraWork(Boolean.TRUE.equals(dto.getRequiresOwnerApprovalForEveryExtraWork()))
                .plannedDropOff(immediateDropOff)
                .checkedInAt(immediateDropOff ? now : null)
                .laborTotal(BigDecimal.ZERO)
                .partsTotal(BigDecimal.ZERO)
                .manualDiscountAmount(BigDecimal.ZERO)
                .pointsDiscountAmount(BigDecimal.ZERO)
                .loyaltyPointsSpent(0)
                .build();
        orderFinancialsService.initialize(order);

        Order savedOrder = orderRepository.save(order);
        replaceServiceLines(savedOrder, dto.getSelectedServiceIds());
        repriceLaborFromSelectedServices(savedOrder);
        savedOrder = orderRepository.save(savedOrder);
        OrderCreatedNotificationPayload payload = orderNotificationPayloadFactory.orderCreated(savedOrder);
        applicationEventPublisher.publishEvent(new OrderCreatedDomainEvent(payload));
        CoreActor actor = safeActor();
        orderTimelineService.append(savedOrder, OrderTimelineEventType.ORDER_BOOKED, OrderTimelineVisibility.BOTH, actor.actorType(), actor.actorId(), savedOrder.getStatus(), immediateDropOff ? "Vehicle checked in" : "Order booked", null, "order-created-" + savedOrder.getId());

        return mapToResponse(savedOrder);
    }

    private void validateCreateInput(OrderCreateDTO dto) {
        boolean hasProblem = normalizeOptionalText(dto.getProblem()) != null;
        boolean hasServices = dto.getSelectedServiceIds() != null && !dto.getSelectedServiceIds().isEmpty();
        if (!hasProblem && !hasServices) {
            throw new OrderConflictException("Order requires either problem description or selected services");
        }
    }

    private String defaultProblem(String problem, List<Integer> selectedServiceIds) {
        String normalized = normalizeOptionalText(problem);
        if (normalized != null) {
            return normalized;
        }
        if (selectedServiceIds != null && !selectedServiceIds.isEmpty()) {
            return "Service catalog intake";
        }
        throw new OrderConflictException("Order requires either problem description or selected services");
    }

    private void replaceServiceLines(Order order, List<Integer> selectedServiceIds) {
        orderServiceItemRepository.deleteAllByOrderId(order.getId());
        if (selectedServiceIds == null || selectedServiceIds.isEmpty()) {
            return;
        }
        List<Services> services = servicesRepository.findAllById(selectedServiceIds);
        if (services.size() != selectedServiceIds.size()) {
            throw new OrderConflictException("One or more selected services do not exist");
        }
        List<OrderServiceItem> items = services.stream()
                .map(service -> OrderServiceItem.builder()
                        .order(order)
                        .employee(order.getEmployee())
                        .service(service)
                        .price(service.getBasePrice())
                        .build())
                .toList();
        orderServiceItemRepository.saveAll(items);
    }

    private void repriceLaborFromSelectedServices(Order order) {
        BigDecimal laborTotal = orderServiceItemRepository.findAllByOrderIdOrderByIdAsc(order.getId()).stream()
                .map(OrderServiceItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setLaborTotal(laborTotal);
        orderFinancialsService.initialize(order);
    }

    private void ensurePlannedVisitMutable(Order order) {
        if (order.getCheckedInAt() != null && order.getStatus() != OrderStatus.ACCEPTED && order.getStatus() != OrderStatus.WAITING_FOR_VISIT) {
            throw new InvalidOrderStateException("Planned visit cannot be changed after check-in");
        }
    }

    private Order findOrder(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Customer findCustomer(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private Vehicle findVehicle(Integer id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    private Employee findEmployee(Integer id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Employee findAssignableEmployee(Integer id) {
        Employee employee = findEmployee(id);
        if (employee.getFunction() != EmployeeType.MECHANIC && employee.getFunction() != EmployeeType.MANAGER) {
            throw new OrderConflictException("Only mechanic or manager can be assigned to order");
        }
        return employee;
    }

    private void validateCustomerVehicleConsistency(Customer customer, Vehicle vehicle) {
        if (!vehicle.getCustomer().getId().equals(customer.getId())) {
            throw new OrderConflictException("Vehicle does not belong to the specified customer");
        }
    }

    private void ensureOrderIsMutable(Order order) {
        if (isTerminal(order.getStatus())) {
            throw new InvalidOrderStateException("Order in status '%s' can no longer be updated".formatted(order.getStatus()));
        }
    }

    private void ensureEstimateIsEditable(Order order) {
        if (isTerminal(order.getStatus()) || order.getStatus() == OrderStatus.READY_FOR_OWNER || order.getStatus() == OrderStatus.HANDED_OVER) {
            throw new InvalidOrderStateException("Order in status '%s' can no longer be updated".formatted(order.getStatus()));
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        boolean validTransition = switch (currentStatus) {
            case NEW -> targetStatus == OrderStatus.IN_PROGRESS || targetStatus == OrderStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == OrderStatus.COMPLETED || targetStatus == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED, HANDED_OVER, CANCELLED_NO_SHOW, CANCELLED_BY_CUSTOMER, CANCELLED_INTERNAL -> false;
            case WAITING_FOR_VISIT -> targetStatus == OrderStatus.ACCEPTED || targetStatus == OrderStatus.CANCELLED_NO_SHOW
                    || targetStatus == OrderStatus.CANCELLED_BY_CUSTOMER || targetStatus == OrderStatus.CANCELLED_INTERNAL;
            case ACCEPTED -> targetStatus == OrderStatus.DIAGNOSIS_IN_PROGRESS || targetStatus == OrderStatus.REPAIR_IN_PROGRESS
                    || targetStatus == OrderStatus.CANCELLED_INTERNAL;
            case DIAGNOSIS_IN_PROGRESS -> targetStatus == OrderStatus.WAITING_FOR_OWNER_APPROVAL || targetStatus == OrderStatus.WAITING_FOR_PART
                    || targetStatus == OrderStatus.REPAIR_IN_PROGRESS || targetStatus == OrderStatus.READY_FOR_OWNER
                    || targetStatus == OrderStatus.CANCELLED_INTERNAL;
            case WAITING_FOR_OWNER_APPROVAL -> targetStatus == OrderStatus.DIAGNOSIS_IN_PROGRESS || targetStatus == OrderStatus.REPAIR_IN_PROGRESS
                    || targetStatus == OrderStatus.READY_FOR_OWNER || targetStatus == OrderStatus.CANCELLED_BY_CUSTOMER
                    || targetStatus == OrderStatus.CANCELLED_INTERNAL;
            case WAITING_FOR_PART -> targetStatus == OrderStatus.REPAIR_IN_PROGRESS || targetStatus == OrderStatus.CANCELLED_BY_CUSTOMER
                    || targetStatus == OrderStatus.CANCELLED_INTERNAL;
            case REPAIR_IN_PROGRESS -> targetStatus == OrderStatus.WAITING_FOR_OWNER_APPROVAL || targetStatus == OrderStatus.WAITING_FOR_PART
                    || targetStatus == OrderStatus.READY_FOR_OWNER || targetStatus == OrderStatus.CANCELLED_INTERNAL;
            case READY_FOR_OWNER -> targetStatus == OrderStatus.HANDED_OVER || targetStatus == OrderStatus.CANCELLED_INTERNAL;
        };

        if (!validTransition) {
            throw new InvalidOrderStateException("Cannot transition order status from '%s' to '%s'".formatted(currentStatus, targetStatus));
        }
    }

    private void applyStatusGuards(Order order, OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.IN_PROGRESS || targetStatus == OrderStatus.REPAIR_IN_PROGRESS) {
            if (order.getEmployee() == null || order.getEmployee().getFunction() != EmployeeType.MECHANIC) {
                throw new InvalidOrderStateException("Order must have an assigned mechanic before moving to %s".formatted(targetStatus));
            }
        }
        if (targetStatus == OrderStatus.ACCEPTED && order.getCheckedInAt() == null) {
            order.setCheckedInAt(Instant.now());
        }
        if ((targetStatus == OrderStatus.COMPLETED || targetStatus == OrderStatus.HANDED_OVER) && order.getCostsTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderStateException("Order estimate must be calculated before moving to %s".formatted(targetStatus));
        }
    }

    private void applyStatusSideEffects(Order order, OrderStatus targetStatus, CancellationReason cancellationReason) {
        Instant now = Instant.now();
        switch (targetStatus) {
            case ACCEPTED -> order.setCheckedInAt(order.getCheckedInAt() == null ? now : order.getCheckedInAt());
            case READY_FOR_OWNER -> order.setReadyForOwnerAt(now);
            case HANDED_OVER -> {
                order.setHandedOverAt(now);
                order.setCompletedAt(now);
            }
            case COMPLETED -> order.setCompletedAt(now);
            case CANCELLED, CANCELLED_NO_SHOW, CANCELLED_BY_CUSTOMER, CANCELLED_INTERNAL -> {
                order.setCancelledAt(now);
                order.setCompletedAt(null);
                order.setCancellationReason(resolveCancellationReason(targetStatus, cancellationReason));
            }
            default -> {
            }
        }
    }

    private CancellationReason resolveCancellationReason(OrderStatus targetStatus, CancellationReason provided) {
        if (provided != null) {
            return provided;
        }
        return switch (targetStatus) {
            case CANCELLED_NO_SHOW -> CancellationReason.NO_SHOW;
            case CANCELLED_BY_CUSTOMER, CANCELLED -> CancellationReason.CUSTOMER_CANCELLED;
            case CANCELLED_INTERNAL -> CancellationReason.INTERNAL_SHOP_CANCELLED;
            default -> null;
        };
    }



    private CoreActor safeActor() {
        CoreActor actor = coreSecurityService.currentActor();
        return actor == null ? new CoreActor(null, com.vladko.autoshopcore.order.timeline.entity.OrderTimelineActorType.SYSTEM) : actor;
    }

    private void requireRoleForStatusTransition(Order order, OrderStatus targetStatus) {
        switch (targetStatus) {
            case ACCEPTED, HANDED_OVER, CANCELLED_NO_SHOW -> coreSecurityService.requireRoles("ADMIN", "MANAGER", "RECEPTIONIST");
            case DIAGNOSIS_IN_PROGRESS, REPAIR_IN_PROGRESS, READY_FOR_OWNER -> coreSecurityService.requireRoles("ADMIN", "MANAGER", "MECHANIC");
            case CANCELLED_BY_CUSTOMER -> coreSecurityService.requireRoles("CUSTOMER", "ADMIN", "MANAGER");
            case CANCELLED_INTERNAL, WAITING_FOR_PART -> coreSecurityService.requireRoles("ADMIN", "MANAGER");
            case WAITING_FOR_OWNER_APPROVAL -> coreSecurityService.requireRoles("ADMIN", "MANAGER", "MECHANIC");
            default -> coreSecurityService.requireAnyStaff();
        }
    }

    private boolean isTerminal(OrderStatus status) {
        return isCompletionLike(status) || isCancellationLike(status);
    }

    private boolean isCompletionLike(OrderStatus status) {
        return status == OrderStatus.COMPLETED || status == OrderStatus.HANDED_OVER;
    }

    private boolean isCancellationLike(OrderStatus status) {
        return status == OrderStatus.CANCELLED || status == OrderStatus.CANCELLED_NO_SHOW
                || status == OrderStatus.CANCELLED_BY_CUSTOMER || status == OrderStatus.CANCELLED_INTERNAL;
    }

    private OrderResponseDTO mapToResponse(Order order) {
        List<OrderServiceLineDTO> serviceLines = order.getId() == null
                ? List.of()
                : orderServiceItemRepository.findAllByOrderIdOrderByIdAsc(order.getId()).stream()
                .map(item -> OrderServiceLineDTO.builder()
                        .serviceId(item.getService().getId())
                        .serviceName(item.getService().getName())
                        .price(item.getPrice())
                        .build())
                .toList();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .vehicleId(order.getVehicle().getId())
                .employeeId(order.getEmployee() == null ? null : order.getEmployee().getId())
                .problem(order.getProblem())
                .status(order.getStatus())
                .crmStatus(order.getStatus())
                .legacyStatus(legacyOrderStatusProjector.project(order.getStatus()))
                .plannedVisitAt(order.getPlannedVisitAt())
                .plannedSlotMinutes(order.getPlannedSlotMinutes())
                .bookingChannel(order.getBookingChannel())
                .intakeNotes(order.getIntakeNotes())
                .requiresOwnerApprovalForEveryExtraWork(order.getRequiresOwnerApprovalForEveryExtraWork())
                .plannedDropOff(order.getPlannedDropOff())
                .checkedInAt(order.getCheckedInAt())
                .readyForOwnerAt(order.getReadyForOwnerAt())
                .handedOverAt(order.getHandedOverAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .laborTotal(order.getLaborTotal())
                .partsTotal(order.getPartsTotal())
                .costsTotal(order.getCostsTotal())
                .manualDiscountAmount(order.getManualDiscountAmount())
                .pointsDiscountAmount(order.getPointsDiscountAmount())
                .loyaltyPointsSpent(order.getLoyaltyPointsSpent())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .serviceLines(serviceLines)
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
