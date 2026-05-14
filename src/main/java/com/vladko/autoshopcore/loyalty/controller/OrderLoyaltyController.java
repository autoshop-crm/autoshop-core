package com.vladko.autoshopcore.loyalty.controller;

import com.vladko.autoshopcore.loyalty.dto.OrderLoyaltySpendDTO;
import com.vladko.autoshopcore.loyalty.service.CrmLoyaltyFacade;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/{orderId}/loyalty")
@RequiredArgsConstructor
public class OrderLoyaltyController {

    private final CrmLoyaltyFacade loyaltyService;
    private final OrderService orderService;

    @PutMapping("/spend")
    public ResponseEntity<OrderResponseDTO> spendPoints(@PathVariable Integer orderId,
                                                        @Valid @RequestBody OrderLoyaltySpendDTO dto) {
        loyaltyService.applyPointsToOrder(orderId, dto.getPoints());
        return ResponseEntity.ok(orderService.getById(orderId));
    }

    @DeleteMapping("/spend")
    public ResponseEntity<OrderResponseDTO> removePoints(@PathVariable Integer orderId) {
        loyaltyService.removePointsFromOrder(orderId);
        return ResponseEntity.ok(orderService.getById(orderId));
    }
}
