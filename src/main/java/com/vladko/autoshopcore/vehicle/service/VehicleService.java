package com.vladko.autoshopcore.vehicle.service;

import com.vladko.autoshopcore.vehicle.dto.VehicleCreateDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleUpdateDTO;

import java.util.List;

public interface VehicleService {

    VehicleResponseDTO create(VehicleCreateDTO dto);

    VehicleResponseDTO getById(Integer id);

    VehicleResponseDTO getByVin(String vin);

    List<VehicleResponseDTO> getAllByCustomerId(Integer customerId);

    VehicleResponseDTO update(Integer id, VehicleUpdateDTO dto);

    void delete(Integer id);
}
