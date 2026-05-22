package com.vladko.autoshopcore.client.controller;

import com.vladko.autoshopcore.client.dto.CustomerBookingSlotResponseDTO;
import com.vladko.autoshopcore.client.service.CustomerBookingSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customers/me/booking")
@RequiredArgsConstructor
public class CustomerBookingSlotController {

    private final CustomerBookingSlotService customerBookingSlotService;

    @GetMapping("/slots")
    public ResponseEntity<List<CustomerBookingSlotResponseDTO>> lookupSlots(@RequestParam Integer vehicleId,
                                                                            @RequestParam(required = false) List<Integer> serviceIds,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                                            @RequestParam(required = false) Integer days,
                                                                            @RequestParam(required = false) Integer slotMinutes) {
        return ResponseEntity.ok(customerBookingSlotService.lookupSlots(vehicleId, serviceIds, dateFrom, days, slotMinutes));
    }
}
