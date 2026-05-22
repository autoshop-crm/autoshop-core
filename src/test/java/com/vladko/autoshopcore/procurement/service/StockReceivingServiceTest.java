package com.vladko.autoshopcore.procurement.service;

import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import com.vladko.autoshopcore.procurement.dto.StockReceiptDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReceivingServiceTest {

    @Mock
    private PartRepository partRepository;

    @InjectMocks
    private StockReceivingServiceImpl stockReceivingService;

    @Test
    void receiveShouldIncreaseStockAndUpdateSalePrice() {
        Part part = Part.builder()
                .id(12)
                .brand("Knecht")
                .name("Oil filter")
                .articleNumber("OC47")
                .cost(new BigDecimal("200.00"))
                .stockQuantity(3)
                .reservedQuantity(1)
                .build();

        when(partRepository.findById(12)).thenReturn(Optional.of(part));
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = stockReceivingService.receive(StockReceiptDTO.builder()
                .targetPartId(12)
                .receivedQuantity(5)
                .salePrice(new BigDecimal("220.00"))
                .build());

        assertThat(response.getStockQuantity()).isEqualTo(8);
        assertThat(response.getAvailableQuantity()).isEqualTo(7);
        assertThat(response.getCost()).isEqualByComparingTo("220.00");
    }
}
