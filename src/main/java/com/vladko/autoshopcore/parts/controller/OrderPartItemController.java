package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.OrderPartItemCreateDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemResponseDTO;
import com.vladko.autoshopcore.parts.dto.OrderPartItemUpdateDTO;
import com.vladko.autoshopcore.parts.service.OrderPartItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/parts")
@RequiredArgsConstructor
public class OrderPartItemController {

    private final OrderPartItemService orderPartItemService;

    @PostMapping
    public ResponseEntity<OrderPartItemResponseDTO> create(@PathVariable Integer orderId,
                                                           @Valid @RequestBody OrderPartItemCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderPartItemService.create(orderId, dto));
    }

    @GetMapping
    public ResponseEntity<List<OrderPartItemResponseDTO>> getAllByOrderId(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderPartItemService.getAllByOrderId(orderId));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<OrderPartItemResponseDTO> update(@PathVariable Integer orderId,
                                                           @PathVariable Integer itemId,
                                                           @Valid @RequestBody OrderPartItemUpdateDTO dto) {
        return ResponseEntity.ok(orderPartItemService.update(orderId, itemId, dto));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable Integer orderId, @PathVariable Integer itemId) {
        orderPartItemService.delete(orderId, itemId);
        return ResponseEntity.noContent().build();
    }
}
