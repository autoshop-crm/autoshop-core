package com.vladko.autoshopcore.parts.service.catalog;

import com.vladko.autoshopcore.integration.shared.ExternalApiRetryExecutor;
import com.vladko.autoshopcore.integration.shared.JsonRedisCacheService;
import com.vladko.autoshopcore.integration.umapi.client.UmapiClient;
import com.vladko.autoshopcore.integration.umapi.config.UmapiProperties;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiManufacturerResponse;
import com.vladko.autoshopcore.integration.umapi.dto.catalog.UmapiPassengerModificationResponse;
import com.vladko.autoshopcore.integration.umapi.mapper.UmapiVehicleCatalogMapper;
import com.vladko.autoshopcore.integration.umapi.support.UmapiCatalogCacheKeyFactory;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogManufacturerResponseDTO;
import com.vladko.autoshopcore.parts.dto.catalog.CatalogModificationResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UmapiVehicleCatalogLookupServiceTest {

    private final UmapiClient umapiClient = mock(UmapiClient.class);
    private final JsonRedisCacheService cacheService = mock(JsonRedisCacheService.class);
    private final UmapiVehicleCatalogLookupService service = new UmapiVehicleCatalogLookupService(
            umapiClient,
            properties(),
            new ExternalApiRetryExecutor(1, Duration.ZERO),
            cacheService,
            new UmapiCatalogCacheKeyFactory(),
            new UmapiVehicleCatalogMapper()
    );

    @Test
    void getManufacturersShouldReturnCachedListWithoutCallingUmapi() {
        when(cacheService.getList(anyString(), eq(CatalogManufacturerResponseDTO.class)))
                .thenReturn(Optional.of(List.of(CatalogManufacturerResponseDTO.builder()
                        .type("PC")
                        .manufacturerId(111)
                        .name("TOYOTA")
                        .build())));

        var response = service.getManufacturers("PC", true);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("TOYOTA");
        verify(umapiClient, never()).getManufacturers(anyString(), anyBoolean());
    }

    @Test
    void getManufacturersShouldFetchAndCacheWhenMissing() {
        when(cacheService.getList(anyString(), eq(CatalogManufacturerResponseDTO.class)))
                .thenReturn(Optional.empty());
        UmapiManufacturerResponse item = new UmapiManufacturerResponse();
        item.setMfaId(111);
        item.setManufacturer("TOYOTA");
        when(umapiClient.getManufacturers("PC", true)).thenReturn(List.of(item));

        var response = service.getManufacturers("pc", true);

        assertThat(response.get(0).getManufacturerId()).isEqualTo(111);
        verify(cacheService).put(anyString(), any(), any(Duration.class));
    }

    @Test
    void getModificationsShouldMapDecimalStringFieldsFromUmapi() {
        when(cacheService.getList(anyString(), eq(CatalogModificationResponseDTO.class)))
                .thenReturn(Optional.empty());

        UmapiPassengerModificationResponse item = new UmapiPassengerModificationResponse();
        item.setModificationId(18099);
        item.setName("1.3 (BK14)");
        item.setPowerPs(new BigDecimal("84.0000"));
        item.setCapacityLiters(new BigDecimal("1.3000"));
        item.setEngineType("Бензиновый двигатель");
        item.setBodyType("Хэтчбэк");
        item.setFuelType("бензин");
        when(umapiClient.getPassengerModifications("PC", 5062)).thenReturn(List.of(item));

        var response = service.getModifications("PC", 5062);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getModificationId()).isEqualTo(18099);
        assertThat(response.get(0).getPowerPs()).isEqualTo(84);
        assertThat(response.get(0).getCapacityLiters()).isEqualTo(new java.math.BigDecimal("1.3000"));
        assertThat(response.get(0).getDisplayName()).contains("1.3 (BK14)", "84 hp", "бензин", "Хэтчбэк");
        verify(cacheService).put(anyString(), any(), any(Duration.class));
    }

    private UmapiProperties properties() {
        return new UmapiProperties(
                "https://api.umapi.ru",
                "umapi-key",
                "ru",
                "WWW",
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                new UmapiProperties.Cache(Duration.ofHours(6)),
                new UmapiProperties.Retry(1, Duration.ZERO)
        );
    }
}
