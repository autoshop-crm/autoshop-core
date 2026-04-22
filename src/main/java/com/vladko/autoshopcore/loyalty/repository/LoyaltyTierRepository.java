package com.vladko.autoshopcore.loyalty.repository;

import com.vladko.autoshopcore.loyalty.entity.LoyaltyTier;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.math.BigDecimal;
import java.util.Optional;

public interface LoyaltyTierRepository extends BaseRepository<LoyaltyTier, Integer> {

    Optional<LoyaltyTier> findByName(String name);

    Optional<LoyaltyTier> findTopByEntrySpentMoneyLessThanEqualOrderByEntrySpentMoneyDesc(BigDecimal totalSpent);
}
