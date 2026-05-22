package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerFileDownloadUrlResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerFileMetadataDTO;

import java.util.List;

public interface CustomerFilesSelfService {
    List<CustomerFileMetadataDTO> getCurrentCustomerDocuments();
    List<CustomerFileMetadataDTO> getCurrentCustomerVehicleDocuments(Integer vehicleId);
    List<CustomerFileMetadataDTO> getCurrentCustomerOrderDocuments(Integer orderId);
    CustomerFileDownloadUrlResponseDTO getPresignedDownloadUrl(String fileId);
}
