package com.vladko.autoshopcore.loyalty.controller;

import com.vladko.autoshopcore.loyalty.dto.LoyaltyAccountResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltySettingsResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTierResponseDTO;
import com.vladko.autoshopcore.loyalty.dto.LoyaltyTransactionResponseDTO;
import com.vladko.autoshopcore.loyalty.service.CrmLoyaltyFacade;
import com.vladko.autoshopcore.loyalty.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final CrmLoyaltyFacade crmLoyaltyFacade;

    @GetMapping("/settings")
    public ResponseEntity<LoyaltySettingsResponseDTO> getSettings() {
        return ResponseEntity.ok(crmLoyaltyFacade.getSettings());
    }

    @GetMapping("/accounts/customer/{customerId}")
    public ResponseEntity<LoyaltyAccountResponseDTO> getAccountByCustomerId(@PathVariable Integer customerId) {
        return ResponseEntity.ok(crmLoyaltyFacade.getVisibleAccountByCustomerId(customerId));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<LoyaltyTransactionResponseDTO>> getTransactions(@PathVariable Integer accountId) {
        return ResponseEntity.ok(loyaltyService.getTransactions(accountId));
    }

    @GetMapping("/tiers")
    public ResponseEntity<List<LoyaltyTierResponseDTO>> getTiers() {
        return ResponseEntity.ok(loyaltyService.getTiers());
    }
}
