package com.vladko.autoshopcore.parts.service.vehicle;

import com.vladko.autoshopcore.parts.dto.vehicle.VehicleScopedPartSearchResponseDTO;

public interface VehicleScopedPartSearchService {

    VehicleScopedPartSearchResponseDTO searchByName(Integer orderId,
                                                    String query,
                                                    Boolean availableOnly,
                                                    Integer limit,
                                                    Integer offset);
}
