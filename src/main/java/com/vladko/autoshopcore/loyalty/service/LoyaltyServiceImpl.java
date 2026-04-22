package com.vladko.autoshopcore.loyalty.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerNotFoundException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyAccountResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTierResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTransactionResponseDTO;
import com.vladko.autoshopcore.loyalty.entity.LoyaltyAccount;
import com.vladko.autoshopcore.loyalty.entity.LoyaltyTier;
import com.vladko.autoshopcore.loyalty.entity.LoyaltyTransaction;
import com.vladko.autoshopcore.loyalty.entity.LoyaltyTransactionReason;
import com.vladko.autoshopcore.loyalty.entity.OperationType;
import com.vladko.autoshopcore.loyalty.exception.InsufficientLoyaltyBalanceException;
import com.vladko.autoshopcore.loyalty.exception.InvalidLoyaltyOperationException;
import com.vladko.autoshopcore.loyalty.exception.LoyaltyAccountNotFoundException;
import com.vladko.autoshopcore.loyalty.exception.LoyaltyTierNotFoundException;
import com.vladko.autoshopcore.loyalty.repository.LoyaltyAccountRepository;
import com.vladko.autoshopcore.loyalty.repository.LoyaltyTierRepository;
import com.vladko.autoshopcore.loyalty.repository.LoyaltyTransactionRepository;
import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.exception.InvalidOrderStateException;
import com.vladko.autoshopcore.order.exception.OrderNotFoundException;
import com.vladko.autoshopcore.order.repository.OrderRepository;
import com.vladko.autoshopcore.order.service.OrderFinancialsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private static final String BRONZE_TIER = "BRONZE";
    private static final BigDecimal EARN_RATE = new BigDecimal("0.05");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTierRepository tierRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderFinancialsService orderFinancialsService;

    @Override
    @Transactional
    public LoyaltyAccountResponseDTO getOrCreateAccountByCustomerId(Integer customerId) {
        return mapAccount(getOrCreateAccount(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoyaltyTierResponseDTO> getTiers() {
        return tierRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(LoyaltyTier::getEntrySpentMoney))
                .map(this::mapTier)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponseDTO> getTransactions(Integer accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new LoyaltyAccountNotFoundException(accountId);
        }

        return transactionRepository.findAllByAccountIdOrderByIdDesc(accountId)
                .stream()
                .map(this::mapTransaction)
                .toList();
    }

    @Override
    @Transactional
    public void applyPointsToOrder(Integer orderId, Integer points) {
        if (points == null || points <= 0) {
            throw new InvalidLoyaltyOperationException("Points amount must be greater than zero");
        }

        Order order = findOrderForUpdate(orderId);
        ensureOrderAllowsLoyaltyChange(order);

        LoyaltyAccount account = getOrCreateAccountForUpdate(order.getCustomer().getId());
        int currentPoints = defaultIfNull(order.getLoyaltyPointsSpent());
        int effectiveBalance = account.getBalance() + currentPoints;
        int maxAllowedPoints = calculateMaxAllowedPoints(order, account, effectiveBalance);

        if (points > maxAllowedPoints) {
            throw new InsufficientLoyaltyBalanceException(
                    "Requested loyalty points exceed available balance or tier limit"
            );
        }

        int delta = points - currentPoints;
        if (delta > 0) {
            withdrawPoints(account, delta);
            createTransaction(account, order, OperationType.SPEND, LoyaltyTransactionReason.POINTS_APPLIED, delta);
        } else if (delta < 0) {
            int refundPoints = Math.abs(delta);
            refundPoints(account, refundPoints);
            createTransaction(account, order, OperationType.REFUND, LoyaltyTransactionReason.POINTS_UPDATED, refundPoints);
        }

        applyOrderPoints(order, points);
        accountRepository.save(account);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void removePointsFromOrder(Integer orderId) {
        Order order = findOrderForUpdate(orderId);
        ensureOrderAllowsLoyaltyChange(order);
        removeAppliedPoints(order, LoyaltyTransactionReason.POINTS_REMOVED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void refreshAppliedPointsAfterOrderChange(Order order) {
        int currentPoints = defaultIfNull(order.getLoyaltyPointsSpent());
        if (currentPoints == 0) {
            orderFinancialsService.recalculate(order);
            return;
        }

        LoyaltyAccount account = accountRepository.findByCustomerIdForUpdate(order.getCustomer().getId())
                .orElseThrow(() -> new InvalidLoyaltyOperationException("Order has loyalty points but account is missing"));
        int effectiveBalance = account.getBalance() + currentPoints;
        int maxAllowedPoints = calculateMaxAllowedPoints(order, account, effectiveBalance);

        if (currentPoints > maxAllowedPoints) {
            int refundPoints = currentPoints - maxAllowedPoints;
            refundPoints(account, refundPoints);
            createTransaction(account, order, OperationType.REFUND, LoyaltyTransactionReason.POINTS_UPDATED, refundPoints);
            applyOrderPoints(order, maxAllowedPoints);
            accountRepository.save(account);
            return;
        }

        orderFinancialsService.recalculate(order);
    }

    @Override
    @Transactional
    public void processOrderCompleted(Order order) {
        if (transactionRepository.existsByOrderIdAndOperationTypeAndReason(
                order.getId(),
                OperationType.EARN,
                LoyaltyTransactionReason.ORDER_COMPLETED
        )) {
            return;
        }

        LoyaltyAccount account = getOrCreateAccountForUpdate(order.getCustomer().getId());
        orderFinancialsService.recalculate(order);

        int earnedPoints = order.getFinalAmount()
                .multiply(EARN_RATE)
                .setScale(0, RoundingMode.FLOOR)
                .intValue();

        account.setBalance(account.getBalance() + earnedPoints);
        account.setTotalEarnedPoints(account.getTotalEarnedPoints() + earnedPoints);
        account.setTotalSpent(account.getTotalSpent().add(order.getFinalAmount()));
        account.setTier(selectTier(account.getTotalSpent()));

        createTransaction(account, order, OperationType.EARN, LoyaltyTransactionReason.ORDER_COMPLETED, earnedPoints);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void processOrderCancelled(Order order) {
        if (transactionRepository.existsByOrderIdAndOperationTypeAndReason(
                order.getId(),
                OperationType.REFUND,
                LoyaltyTransactionReason.ORDER_CANCELLED
        )) {
            return;
        }

        int currentPoints = defaultIfNull(order.getLoyaltyPointsSpent());
        if (currentPoints == 0) {
            return;
        }

        LoyaltyAccount account = accountRepository.findByCustomerIdForUpdate(order.getCustomer().getId())
                .orElseThrow(() -> new InvalidLoyaltyOperationException("Order has loyalty points but account is missing"));
        refundPoints(account, currentPoints);
        createTransaction(account, order, OperationType.REFUND, LoyaltyTransactionReason.ORDER_CANCELLED, currentPoints);
        applyOrderPoints(order, 0);
        accountRepository.save(account);
    }

    private Order findOrderForUpdate(Integer orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private LoyaltyAccount getOrCreateAccount(Integer customerId) {
        return accountRepository.findByCustomerId(customerId)
                .orElseGet(() -> createAccount(customerId));
    }

    private LoyaltyAccount getOrCreateAccountForUpdate(Integer customerId) {
        return accountRepository.findByCustomerIdForUpdate(customerId)
                .orElseGet(() -> createAccount(customerId));
    }

    private LoyaltyAccount createAccount(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        LoyaltyAccount account = LoyaltyAccount.builder()
                .customer(customer)
                .tier(findBronzeTier())
                .balance(0)
                .totalSpent(BigDecimal.ZERO)
                .totalEarnedPoints(0)
                .build();
        return accountRepository.save(account);
    }

    private LoyaltyTier findBronzeTier() {
        return tierRepository.findByName(BRONZE_TIER)
                .orElseThrow(() -> new LoyaltyTierNotFoundException(BRONZE_TIER));
    }

    private LoyaltyTier selectTier(BigDecimal totalSpent) {
        return tierRepository.findTopByEntrySpentMoneyLessThanEqualOrderByEntrySpentMoneyDesc(totalSpent)
                .orElseGet(this::findBronzeTier);
    }

    private void ensureOrderAllowsLoyaltyChange(Order order) {
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new InvalidOrderStateException(
                    "Order in status '%s' cannot use loyalty points".formatted(order.getStatus())
            );
        }
    }

    private int calculateMaxAllowedPoints(Order order, LoyaltyAccount account, int effectiveBalance) {
        BigDecimal spendableBase = order.getCostsTotal()
                .subtract(defaultIfNull(order.getManualDiscountAmount()));
        if (spendableBase.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        int orderLimit = spendableBase.setScale(0, RoundingMode.FLOOR).intValue();
        int tierLimit = spendableBase
                .multiply(BigDecimal.valueOf(account.getTier().getMaxPointsPaymentPercent()))
                .divide(ONE_HUNDRED, 0, RoundingMode.FLOOR)
                .intValue();

        return Math.min(Math.min(effectiveBalance, orderLimit), tierLimit);
    }

    private void withdrawPoints(LoyaltyAccount account, int points) {
        if (account.getBalance() < points) {
            throw new InsufficientLoyaltyBalanceException("Not enough loyalty points on account");
        }
        account.setBalance(account.getBalance() - points);
    }

    private void refundPoints(LoyaltyAccount account, int points) {
        account.setBalance(account.getBalance() + points);
    }

    private void removeAppliedPoints(Order order, LoyaltyTransactionReason reason) {
        int currentPoints = defaultIfNull(order.getLoyaltyPointsSpent());
        if (currentPoints == 0) {
            return;
        }

        LoyaltyAccount account = accountRepository.findByCustomerIdForUpdate(order.getCustomer().getId())
                .orElseThrow(() -> new InvalidLoyaltyOperationException("Order has loyalty points but account is missing"));
        refundPoints(account, currentPoints);
        createTransaction(account, order, OperationType.REFUND, reason, currentPoints);
        applyOrderPoints(order, 0);
        accountRepository.save(account);
    }

    private void applyOrderPoints(Order order, int points) {
        order.setLoyaltyPointsSpent(points);
        order.setPointsDiscountAmount(BigDecimal.valueOf(points));
        orderFinancialsService.recalculate(order);
    }

    private void createTransaction(LoyaltyAccount account,
                                   Order order,
                                   OperationType operationType,
                                   LoyaltyTransactionReason reason,
                                   int points) {
        transactionRepository.save(LoyaltyTransaction.builder()
                .account(account)
                .order(order)
                .operationType(operationType)
                .reason(reason)
                .pointsAmount(points)
                .build());
    }

    private LoyaltyAccountResponseDTO mapAccount(LoyaltyAccount account) {
        return LoyaltyAccountResponseDTO.builder()
                .id(account.getId())
                .customerId(account.getCustomer().getId())
                .balance(account.getBalance())
                .totalSpent(account.getTotalSpent())
                .totalEarnedPoints(account.getTotalEarnedPoints())
                .tier(mapTier(account.getTier()))
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private LoyaltyTierResponseDTO mapTier(LoyaltyTier tier) {
        return LoyaltyTierResponseDTO.builder()
                .id(tier.getId())
                .name(tier.getName())
                .entrySpentMoney(tier.getEntrySpentMoney())
                .discountPercent(tier.getDiscountPercent())
                .maxPointsPaymentPercent(tier.getMaxPointsPaymentPercent())
                .build();
    }

    private LoyaltyTransactionResponseDTO mapTransaction(LoyaltyTransaction transaction) {
        return LoyaltyTransactionResponseDTO.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .orderId(transaction.getOrder() == null ? null : transaction.getOrder().getId())
                .operationType(transaction.getOperationType())
                .reason(transaction.getReason())
                .pointsAmount(transaction.getPointsAmount())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
