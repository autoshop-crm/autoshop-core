package com.vladko.autoshopcore.client.controller;

import com.vladko.autoshopcore.client.dto.CustomerLoyaltyOverviewDTO;
import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerSelfServiceDashboardDTO;
import com.vladko.autoshopcore.client.dto.CustomerUpdateDTO;
import com.vladko.autoshopcore.client.service.CustomerSelfService;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/me")
@RequiredArgsConstructor
public class CustomerSelfServiceController {

    private final CustomerSelfService customerSelfService;

    @GetMapping
    public ResponseEntity<CustomerResponseDTO> getCurrentCustomer() {
        return ResponseEntity.ok(customerSelfService.getCurrentCustomer());
    }

    @PutMapping
    public ResponseEntity<CustomerResponseDTO> updateCurrentCustomer(@Valid @RequestBody CustomerUpdateDTO dto) {
        return ResponseEntity.ok(customerSelfService.updateCurrentCustomer(dto));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDTO>> getCurrentCustomerOrders() {
        return ResponseEntity.ok(customerSelfService.getCurrentCustomerOrders());
    }

    @GetMapping("/loyalty")
    public ResponseEntity<CustomerLoyaltyOverviewDTO> getCurrentCustomerLoyalty() {
        return ResponseEntity.ok(customerSelfService.getCurrentCustomerLoyalty());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<CustomerSelfServiceDashboardDTO> getCurrentCustomerDashboard() {
        return ResponseEntity.ok(customerSelfService.getCurrentCustomerDashboard());
    }
}
