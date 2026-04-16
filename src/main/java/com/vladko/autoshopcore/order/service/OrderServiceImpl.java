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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public OrderResponseDTO create(OrderCreateDTO dto) {
        Customer customer = findCustomer(dto.getCustomerId());
        Vehicle vehicle = findVehicle(dto.getVehicleId());
        validateCustomerVehicleConsistency(customer, vehicle);

        Employee employee = dto.getEmployeeId() == null ? null : findAssignableEmployee(dto.getEmployeeId());

        Order order = Order.builder()
                .customer(customer)
                .vehicle(vehicle)
                .employee(employee)
                .problem(normalizeText(dto.getProblem()))
                .status(OrderStatus.NEW)
                .costsTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .build();

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getById(Integer id) {
        return mapToResponse(findOrder(id));
    }

    @Override
    @Transactional
    public OrderResponseDTO update(Integer id, OrderUpdateDTO dto) {
        Order order = findOrder(id);
        ensureOrderIsMutable(order);

        String normalizedProblem = normalizeOptionalText(dto.getProblem());
        if (normalizedProblem != null) {
            order.setProblem(normalizedProblem);
        }

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO assignEmployee(Integer id, OrderAssignmentDTO dto) {
        Order order = findOrder(id);
        ensureOrderIsMutable(order);

        Employee employee = findAssignableEmployee(dto.getEmployeeId());
        order.setEmployee(employee);

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO updateEstimate(Integer id, OrderEstimateUpdateDTO dto) {
        Order order = findOrder(id);
        ensureEstimateIsEditable(order);
        applyEstimate(order, dto.getCostsTotal(), dto.getDiscountAmount());

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDTO updateStatus(Integer id, OrderStatusUpdateDTO dto) {
        Order order = findOrder(id);
        OrderStatus targetStatus = dto.getStatus();

        if (order.getStatus() == targetStatus) {
            return mapToResponse(order);
        }

        validateStatusTransition(order.getStatus(), targetStatus);
        validateStatusGuards(order, targetStatus);

        order.setStatus(targetStatus);
        if (targetStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(Instant.now());
        } else if (targetStatus == OrderStatus.CANCELLED) {
            order.setCompletedAt(null);
        }

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByCustomerId(Integer customerId) {
        findCustomer(customerId);
        return orderRepository.findAllByCustomerIdOrderByIdDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByVehicleId(Integer vehicleId) {
        findVehicle(vehicleId);
        return orderRepository.findAllByVehicleIdOrderByIdDesc(vehicleId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllByStatus(OrderStatus status) {
        return orderRepository.findAllByStatusOrderByIdDesc(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
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
            throw new InvalidOrderStateException(
                    "Order in status '%s' can no longer be updated".formatted(order.getStatus())
            );
        }
    }

    private void ensureEstimateIsEditable(Order order) {
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new InvalidOrderStateException(
                    "Order in status '%s' can no longer be updated".formatted(order.getStatus())
            );
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        boolean validTransition = switch (currentStatus) {
            case NEW -> targetStatus == OrderStatus.IN_PROGRESS || targetStatus == OrderStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == OrderStatus.COMPLETED || targetStatus == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new InvalidOrderStateException(
                    "Cannot transition order status from '%s' to '%s'".formatted(currentStatus, targetStatus)
            );
        }
    }

    private void validateStatusGuards(Order order, OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.IN_PROGRESS) {
            if (order.getEmployee() == null || order.getEmployee().getFunction() != EmployeeType.MECHANIC) {
                throw new InvalidOrderStateException("Order must have an assigned mechanic before moving to IN_PROGRESS");
            }
        }

        if (targetStatus == OrderStatus.COMPLETED && order.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderStateException("Order estimate must be calculated before moving to COMPLETED");
        }
    }

    private boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED;
    }

    private void applyEstimate(Order order, BigDecimal costsTotal, BigDecimal discountAmount) {
        validateEstimateValues(costsTotal, discountAmount);

        order.setCostsTotal(costsTotal);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(costsTotal.subtract(discountAmount));
    }

    private void validateEstimateValues(BigDecimal costsTotal, BigDecimal discountAmount) {
        Objects.requireNonNull(costsTotal, "Costs total must not be null");
        Objects.requireNonNull(discountAmount, "Discount amount must not be null");

        if (costsTotal.compareTo(BigDecimal.ZERO) < 0 || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderConflictException("Estimate amounts cannot be negative");
        }

        if (discountAmount.compareTo(costsTotal) > 0) {
            throw new OrderConflictException("Discount amount cannot exceed total costs");
        }
    }

    private OrderResponseDTO mapToResponse(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .vehicleId(order.getVehicle().getId())
                .employeeId(order.getEmployee() == null ? null : order.getEmployee().getId())
                .problem(order.getProblem())
                .status(order.getStatus())
                .costsTotal(order.getCostsTotal())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .build();
    }

    private String normalizeText(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException("Value must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
