package com.vladko.autoshopcore.integration.umapi.mapper;

import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiPassengerModificationResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UmapiVehicleCatalogMapperTest {

    private final UmapiVehicleCatalogMapper mapper = new UmapiVehicleCatalogMapper();

    @Test
    void mapModificationShouldBuildDisplayName() {
        UmapiPassengerModificationResponse item = new UmapiPassengerModificationResponse();
        item.setModificationId(333);
        item.setName("Camry 2.5");
        item.setPowerPs(181);
        item.setFuelType("Petrol");

        var dto = mapper.mapModification("PC", 222, item);

        assertThat(dto.getModificationId()).isEqualTo(333);
        assertThat(dto.getDisplayName()).isEqualTo("Camry 2.5, 181 hp, Petrol");
    }
}
