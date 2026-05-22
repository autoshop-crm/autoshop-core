package com.vladko.autoshopcore.procurement.service;

import com.vladko.autoshopcore.integration.carreta.client.CarretaClient;
import com.vladko.autoshopcore.integration.carreta.config.CarretaProperties;
import com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderResponse;
import com.vladko.autoshopcore.integration.carreta.support.CarretaQuantityParser;
import com.vladko.autoshopcore.procurement.dto.CarretaQuoteOrderDTO;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderCreateDTO;
import com.vladko.autoshopcore.procurement.dto.PurchaseOrderResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarretaPurchaseOrderServiceTest {

    @Mock
    private CarretaClient carretaClient;

    @Test
    void createShouldCallCarretaInTestModeAndKeepPurchaseAndSalePricesSeparate() {
        CarretaPurchaseOrderService service = new CarretaPurchaseOrderService(
                carretaClient,
                properties(),
                new CarretaQuantityParser()
        );
        CarretaOrderResponse externalOrder = new CarretaOrderResponse();
        externalOrder.setId(1001);
        externalOrder.setNumber("A-1");
        externalOrder.setStatus(2);
        externalOrder.setStatusDisplay("Получен в заказ");
        when(carretaClient.createOrder(any(), eq(true))).thenReturn(externalOrder);

        PurchaseOrderResponseDTO response = service.create(PurchaseOrderCreateDTO.builder()
                .quote(quote())
                .quantity(2)
                .salePrice(new BigDecimal("220.00"))
                .clientComment("AS-77")
                .createExternalOrder(true)
                .build());

        assertThat(response.getExternalOrderId()).isEqualTo(1001);
        assertThat(response.isTestMode()).isTrue();
        assertThat(response.getPurchaseUnitPrice()).isEqualByComparingTo("152.02");
        assertThat(response.getSaleUnitPrice()).isEqualByComparingTo("220.00");
        assertThat(response.getPurchaseTotal()).isEqualByComparingTo("304.04");
        assertThat(response.getSaleTotal()).isEqualByComparingTo("440.00");

        ArgumentCaptor<com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderCreateRequest> captor =
                ArgumentCaptor.forClass(com.vladko.autoshopcore.integration.carreta.dto.CarretaOrderCreateRequest.class);
        verify(carretaClient).createOrder(captor.capture(), eq(true));
        assertThat(captor.getValue().getPositionSignature()).isEqualTo("signature");
        assertThat(captor.getValue().getOrderQuantity()).isEqualTo(2);
    }

    @Test
    void createShouldRejectSalePriceLowerThanPurchasePrice() {
        CarretaPurchaseOrderService service = new CarretaPurchaseOrderService(
                carretaClient,
                properties(),
                new CarretaQuantityParser()
        );

        assertThatThrownBy(() -> service.create(PurchaseOrderCreateDTO.builder()
                .quote(quote())
                .quantity(1)
                .salePrice(new BigDecimal("100.00"))
                .createExternalOrder(false)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sale price cannot be lower than purchase price");
    }

    private CarretaQuoteOrderDTO quote() {
        return CarretaQuoteOrderDTO.builder()
                .positionSignature("signature")
                .articleNumber("OC47")
                .brand("KNECHT/MAHLE")
                .name("Oil filter")
                .purchasePrice(new BigDecimal("152.02"))
                .deliveryDaysMin(7)
                .deliveryDaysMax(7)
                .minOrderQuantity(1)
                .quantityRaw("500")
                .build();
    }

    private CarretaProperties properties() {
        return new CarretaProperties(
                "https://api.carreta.ru",
                "test-key",
                "",
                true,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                new CarretaProperties.Cache(Duration.ofMinutes(30)),
                new CarretaProperties.Retry(3, Duration.ofMillis(10))
        );
    }
}
