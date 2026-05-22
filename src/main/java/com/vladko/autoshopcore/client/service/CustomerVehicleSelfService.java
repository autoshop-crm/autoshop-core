package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerVehicleCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerVehicleUpdateDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;

import java.util.List;

public interface CustomerVehicleSelfService {
    List<VehicleResponseDTO> getCurrentCustomerVehicles();
    VehicleResponseDTO getCurrentCustomerVehicle(Integer vehicleId);
    VehicleResponseDTO createVehicle(CustomerVehicleCreateDTO dto);
    VehicleResponseDTO updateVehicle(Integer vehicleId, CustomerVehicleUpdateDTO dto);
    void deleteVehicle(Integer vehicleId);
}
