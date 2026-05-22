package com.vladko.autoshopcore.loyalty.repository;

import com.vladko.autoshopcore.loyalty.entity.LoyaltyAccount;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoyaltyAccountRepository extends BaseRepository<LoyaltyAccount, Integer> {

    Optional<LoyaltyAccount> findByCustomerId(Integer customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoyaltyAccount a where a.customer.id = :customerId")
    Optional<LoyaltyAccount> findByCustomerIdForUpdate(@Param("customerId") Integer customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoyaltyAccount a where a.id = :id")
    Optional<LoyaltyAccount> findByIdForUpdate(@Param("id") Integer id);
}
