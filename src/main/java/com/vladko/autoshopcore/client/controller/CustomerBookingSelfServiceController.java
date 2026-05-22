package com.vladko.autoshopcore.client.controller;

import com.vladko.autoshopcore.client.dto.CustomerBookingCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingServiceCatalogItemDTO;
import com.vladko.autoshopcore.client.dto.CustomerBookingUpdateDTO;
import com.vladko.autoshopcore.client.service.CustomerBookingSelfService;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/me")
@RequiredArgsConstructor
public class CustomerBookingSelfServiceController {

    private final CustomerBookingSelfService customerBookingSelfService;

    @GetMapping("/booking/services")
    public ResponseEntity<List<CustomerBookingServiceCatalogItemDTO>> getAvailableServices() {
        return ResponseEntity.ok(customerBookingSelfService.getAvailableServices());
    }

    @PostMapping("/bookings")
    public ResponseEntity<OrderResponseDTO> createBooking(@Valid @RequestBody CustomerBookingCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerBookingSelfService.createBooking(dto));
    }

    @PutMapping("/bookings/{orderId}")
    public ResponseEntity<OrderResponseDTO> updateBooking(@PathVariable Integer orderId,
                                                          @Valid @RequestBody CustomerBookingUpdateDTO dto) {
        return ResponseEntity.ok(customerBookingSelfService.updateBooking(orderId, dto));
    }

    @PostMapping("/bookings/{orderId}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelBooking(@PathVariable Integer orderId) {
        return ResponseEntity.ok(customerBookingSelfService.cancelBooking(orderId));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponseDTO> getCurrentCustomerOrder(@PathVariable Integer orderId) {
        return ResponseEntity.ok(customerBookingSelfService.getCurrentCustomerOrder(orderId));
    }
}
