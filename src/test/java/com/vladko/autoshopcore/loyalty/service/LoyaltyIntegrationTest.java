package com.vladko.autoshopcore.loyalty.service;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.client.dto.CustomerCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.service.CustomerService;
import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyAccountResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTransactionResponseDTO;
import com.vladko.autoshopcore.loyalty.entity.LoyaltyTransactionReason;
import com.vladko.autoshopcore.loyalty.entity.OperationType;
import com.vladko.autoshopcore.loyalty.repository.LoyaltyAccountRepository;
import com.vladko.autoshopcore.order.dto.OrderAssignmentDTO;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderEstimateUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.repository.EmployeeRepository;
import com.vladko.autoshopcore.order.service.OrderService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false"
})
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoyaltyIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LoyaltyAccountRepository accountRepository;

    @Test
    @Transactional
    void completedOrderShouldEarnPointsAndPromoteTier() {
        TestContext context = createCustomerVehicleAndMechanic("loyalty-earn@example.com", "+79990000301");
        OrderResponseDTO order = createOrder(context, "Transmission repair", "10000.00");

        orderService.updateStatus(order.getId(), new OrderStatusUpdateDTO(OrderStatus.IN_PROGRESS));
        orderService.updateStatus(order.getId(), new OrderStatusUpdateDTO(OrderStatus.COMPLETED));

        LoyaltyAccountResponseDTO account = loyaltyService.getOrCreateAccountByCustomerId(context.customer().getId());
        List<LoyaltyTransactionResponseDTO> transactions = loyaltyService.getTransactions(account.getId());

        assertThat(account.getBalance()).isEqualTo(500);
        assertThat(account.getTotalEarnedPoints()).isEqualTo(500);
        assertThat(account.getTotalSpent()).isEqualByComparingTo("10000.00");
        assertThat(account.getTier().getName()).isEqualTo("SILVER");
        assertThat(transactions)
                .anySatisfy(transaction -> {
                    assertThat(transaction.getOperationType()).isEqualTo(OperationType.EARN);
                    assertThat(transaction.getReason()).isEqualTo(LoyaltyTransactionReason.ORDER_COMPLETED);
                    assertThat(transaction.getPointsAmount()).isEqualTo(500);
                });
    }

    @Test
    @Transactional
    void activeOrderShouldSpendPointsAndCancellationShouldRefundThem() {
        TestContext context = createCustomerVehicleAndMechanic("loyalty-spend@example.com", "+79990000302");
        OrderResponseDTO earnOrder = createOrder(context, "Engine repair", "10000.00");
        orderService.updateStatus(earnOrder.getId(), new OrderStatusUpdateDTO(OrderStatus.IN_PROGRESS));
        orderService.updateStatus(earnOrder.getId(), new OrderStatusUpdateDTO(OrderStatus.COMPLETED));

        OrderResponseDTO spendOrder = createOrder(context, "Brake service", "200.00");
        loyaltyService.applyPointsToOrder(spendOrder.getId(), 40);

        OrderResponseDTO afterSpend = orderService.getById(spendOrder.getId());
        LoyaltyAccountResponseDTO afterSpendAccount = loyaltyService.getOrCreateAccountByCustomerId(context.customer().getId());

        assertThat(afterSpend.getLoyaltyPointsSpent()).isEqualTo(40);
        assertThat(afterSpend.getPointsDiscountAmount()).isEqualByComparingTo("40.00");
        assertThat(afterSpend.getDiscountAmount()).isEqualByComparingTo("40.00");
        assertThat(afterSpend.getFinalAmount()).isEqualByComparingTo("160.00");
        assertThat(afterSpendAccount.getBalance()).isEqualTo(460);

        orderService.updateStatus(spendOrder.getId(), new OrderStatusUpdateDTO(OrderStatus.CANCELLED));

        OrderResponseDTO cancelledOrder = orderService.getById(spendOrder.getId());
        LoyaltyAccountResponseDTO refundedAccount = loyaltyService.getOrCreateAccountByCustomerId(context.customer().getId());
        List<LoyaltyTransactionResponseDTO> transactions = loyaltyService.getTransactions(refundedAccount.getId());

        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.getLoyaltyPointsSpent()).isZero();
        assertThat(cancelledOrder.getPointsDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(refundedAccount.getBalance()).isEqualTo(500);
        assertThat(transactions)
                .anySatisfy(transaction -> {
                    assertThat(transaction.getOperationType()).isEqualTo(OperationType.SPEND);
                    assertThat(transaction.getReason()).isEqualTo(LoyaltyTransactionReason.POINTS_APPLIED);
                    assertThat(transaction.getPointsAmount()).isEqualTo(40);
                })
                .anySatisfy(transaction -> {
                    assertThat(transaction.getOperationType()).isEqualTo(OperationType.REFUND);
                    assertThat(transaction.getReason()).isEqualTo(LoyaltyTransactionReason.ORDER_CANCELLED);
                    assertThat(transaction.getPointsAmount()).isEqualTo(40);
                });
    }

    @Test
    @Transactional
    void reducingEstimateBelowAppliedPointsShouldRefundExcessAutomatically() {
        TestContext context = createCustomerVehicleAndMechanic("loyalty-recap@example.com", "+79990000303");
        OrderResponseDTO earnOrder = createOrder(context, "Large repair", "10000.00");
        orderService.updateStatus(earnOrder.getId(), new OrderStatusUpdateDTO(OrderStatus.IN_PROGRESS));
        orderService.updateStatus(earnOrder.getId(), new OrderStatusUpdateDTO(OrderStatus.COMPLETED));

        OrderResponseDTO spendOrder = createOrder(context, "Small repair", "200.00");
        loyaltyService.applyPointsToOrder(spendOrder.getId(), 40);

        orderService.updateEstimate(spendOrder.getId(), new OrderEstimateUpdateDTO(new BigDecimal("100.00"), BigDecimal.ZERO));

        OrderResponseDTO adjustedOrder = orderService.getById(spendOrder.getId());
        LoyaltyAccountResponseDTO account = loyaltyService.getOrCreateAccountByCustomerId(context.customer().getId());

        assertThat(adjustedOrder.getLoyaltyPointsSpent()).isEqualTo(20);
        assertThat(adjustedOrder.getPointsDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(adjustedOrder.getFinalAmount()).isEqualByComparingTo("80.00");
        assertThat(account.getBalance()).isEqualTo(480);
    }

    @Test
    @Transactional
    void accountShouldBeCreatedLazilyForExistingCustomer() {
        CustomerResponseDTO customer = customerService.create(CustomerCreateDTO.builder()
                .firstName("Lazy")
                .lastName("Account")
                .email("loyalty-lazy@example.com")
                .phoneNumber("+79990000304")
                .build());

        LoyaltyAccountResponseDTO account = loyaltyService.getOrCreateAccountByCustomerId(customer.getId());

        assertThat(account.getBalance()).isZero();
        assertThat(account.getTier().getName()).isEqualTo("BRONZE");
        assertThat(accountRepository.findByCustomerId(customer.getId())).isPresent();
    }

    private TestContext createCustomerVehicleAndMechanic(String email, String phoneNumber) {
        CustomerResponseDTO customer = customerService.create(CustomerCreateDTO.builder()
                .firstName("Ivan")
                .lastName("Loyalty")
                .email(email)
                .phoneNumber(phoneNumber)
                .build());
        VehicleResponseDTO vehicle = vehicleService.create(VehicleCreateDTO.builder()
                .customerId(customer.getId())
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV%s".formatted(phoneNumber.substring(phoneNumber.length() - 7)))
                .licensePlate("L%s".formatted(phoneNumber.substring(phoneNumber.length() - 5)))
                .build());
        Employee mechanic = employeeRepository.save(Employee.builder()
                .firstName("Petr")
                .lastName("Mechanic")
                .function(EmployeeType.MECHANIC)
                .build());

        return new TestContext(customer, vehicle, mechanic);
    }

    private OrderResponseDTO createOrder(TestContext context, String problem, String laborTotal) {
        OrderResponseDTO order = orderService.create(OrderCreateDTO.builder()
                .customerId(context.customer().getId())
                .vehicleId(context.vehicle().getId())
                .problem(problem)
                .build());
        orderService.assignEmployee(order.getId(), new OrderAssignmentDTO(context.mechanic().getId()));
        return orderService.updateEstimate(
                order.getId(),
                new OrderEstimateUpdateDTO(new BigDecimal(laborTotal), BigDecimal.ZERO)
        );
    }

    private record TestContext(CustomerResponseDTO customer, VehicleResponseDTO vehicle, Employee mechanic) {
    }
}
