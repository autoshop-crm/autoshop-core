package com.vladko.autoshopcore.client.controller;

import com.vladko.autoshopcore.client.dto.CustomerFileDownloadUrlResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerFileMetadataDTO;
import com.vladko.autoshopcore.client.service.CustomerFilesSelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers/me")
@RequiredArgsConstructor
public class CustomerFilesSelfServiceController {

    private final CustomerFilesSelfService customerFilesSelfService;

    @GetMapping("/documents")
    public ResponseEntity<List<CustomerFileMetadataDTO>> getCurrentCustomerDocuments() {
        return ResponseEntity.ok(customerFilesSelfService.getCurrentCustomerDocuments());
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    public ResponseEntity<List<CustomerFileMetadataDTO>> getCurrentCustomerVehicleDocuments(@PathVariable Integer vehicleId) {
        return ResponseEntity.ok(customerFilesSelfService.getCurrentCustomerVehicleDocuments(vehicleId));
    }

    @GetMapping("/orders/{orderId}/documents")
    public ResponseEntity<List<CustomerFileMetadataDTO>> getCurrentCustomerOrderDocuments(@PathVariable Integer orderId) {
        return ResponseEntity.ok(customerFilesSelfService.getCurrentCustomerOrderDocuments(orderId));
    }

    @PostMapping("/files/{fileId}/presigned-download-url")
    public ResponseEntity<CustomerFileDownloadUrlResponseDTO> getPresignedDownloadUrl(@PathVariable String fileId) {
        return ResponseEntity.ok(customerFilesSelfService.getPresignedDownloadUrl(fileId));
    }
}
