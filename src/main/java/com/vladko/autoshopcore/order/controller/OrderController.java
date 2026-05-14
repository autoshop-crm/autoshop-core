package com.vladko.autoshopcore.order.controller;

import com.vladko.autoshopcore.order.dto.OrderAssignmentDTO;
import com.vladko.autoshopcore.order.dto.OrderCreateDTO;
import com.vladko.autoshopcore.order.dto.OrderEstimateUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderResponseDTO;
import com.vladko.autoshopcore.order.dto.OrderStatusUpdateDTO;
import com.vladko.autoshopcore.order.dto.OrderUpdateDTO;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(dto));
    }

    @PostMapping("/drop-off")
    public ResponseEntity<OrderResponseDTO> createImmediateDropOff(@Valid @RequestBody OrderCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createImmediateDropOff(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> update(@PathVariable Integer id,
                                                   @Valid @RequestBody OrderUpdateDTO dto) {
        return ResponseEntity.ok(orderService.update(id, dto));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<OrderResponseDTO> assignEmployee(@PathVariable Integer id,
                                                           @Valid @RequestBody OrderAssignmentDTO dto) {
        return ResponseEntity.ok(orderService.assignEmployee(id, dto));
    }

    @PutMapping("/{id}/estimate")
    public ResponseEntity<OrderResponseDTO> updateEstimate(@PathVariable Integer id,
                                                           @Valid @RequestBody OrderEstimateUpdateDTO dto) {
        return ResponseEntity.ok(orderService.updateEstimate(id, dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(@PathVariable Integer id,
                                                         @Valid @RequestBody OrderStatusUpdateDTO dto) {
        return ResponseEntity.ok(orderService.updateStatus(id, dto));
    }

    @PutMapping("/{id}/check-in")
    public ResponseEntity<OrderResponseDTO> checkInVehicle(@PathVariable Integer id) {
        return ResponseEntity.ok(orderService.checkInVehicle(id));
    }

    @PutMapping("/{id}/no-show")
    public ResponseEntity<OrderResponseDTO> cancelNoShow(@PathVariable Integer id) {
        return ResponseEntity.ok(orderService.cancelNoShow(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponseDTO>> getAllByCustomerId(@PathVariable Integer customerId) {
        return ResponseEntity.ok(orderService.getAllByCustomerId(customerId));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<OrderResponseDTO>> getAllByVehicleId(@PathVariable Integer vehicleId) {
        return ResponseEntity.ok(orderService.getAllByVehicleId(vehicleId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponseDTO>> getAllByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getAllByStatus(status));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<OrderResponseDTO>> getBookings(@RequestParam Instant from,
                                                              @RequestParam Instant to) {
        return ResponseEntity.ok(orderService.getBookings(from, to));
    }

    @GetMapping("/bookings/daily")
    public ResponseEntity<List<OrderResponseDTO>> getDailyArrivals(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.getDailyArrivals(date));
    }

    @GetMapping("/bookings/unassigned")
    public ResponseEntity<List<OrderResponseDTO>> getUnassignedBookings(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.getUnassignedBookings(date));
    }
}
