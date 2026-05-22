package com.vladko.autoshopcore.loyalty.repository;

import com.vladko.autoshopcore.loyalty.entity.LoyaltyTransaction;
import com.vladko.autoshopcore.loyalty.entity.LoyaltyTransactionReason;
import com.vladko.autoshopcore.loyalty.entity.OperationType;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;

public interface LoyaltyTransactionRepository extends BaseRepository<LoyaltyTransaction, Integer> {

    List<LoyaltyTransaction> findAllByAccountIdOrderByIdDesc(Integer accountId);

    List<LoyaltyTransaction> findAllByOrderIdOrderByIdAsc(Integer orderId);

    boolean existsByOrderIdAndOperationTypeAndReason(Integer orderId,
                                                     OperationType operationType,
                                                     LoyaltyTransactionReason reason);
}
