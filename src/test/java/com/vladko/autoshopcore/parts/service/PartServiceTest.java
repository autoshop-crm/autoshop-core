package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.PartCreateDTO;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.dto.PartStockUpdateDTO;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.exception.PartConflictException;
import com.vladko.autoshopcore.parts.repository.OrderPartItemRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    @Mock
    private OrderPartItemRepository orderPartItemRepository;

    @InjectMocks
    private PartServiceImpl partService;

    @Test
    void createShouldNormalizeArticleNumberAndPersistPart() {
        PartCreateDTO dto = PartCreateDTO.builder()
                .brand(" Bosch ")
                .name(" Oil filter ")
                .articleNumber(" of-123 ")
                .cost(new BigDecimal("15.50"))
                .build();

        when(partRepository.findByArticleNumber("OF-123")).thenReturn(Optional.empty());
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> {
            Part part = invocation.getArgument(0);
            part.setId(11);
            return part;
        });

        PartResponseDTO response = partService.create(dto);

        assertThat(response.getId()).isEqualTo(11);
        assertThat(response.getArticleNumber()).isEqualTo("OF-123");
        assertThat(response.getName()).isEqualTo("Oil filter");
        assertThat(response.getAvailableQuantity()).isZero();
    }

    @Test
    void createShouldRejectDuplicateArticleNumber() {
        when(partRepository.findByArticleNumber("OF-123")).thenReturn(Optional.of(Part.builder().id(5).build()));

        assertThatThrownBy(() -> partService.create(PartCreateDTO.builder()
                .name("Oil filter")
                .articleNumber("of-123")
                .cost(new BigDecimal("15.50"))
                .build()))
                .isInstanceOf(PartConflictException.class)
                .hasMessage("Part with article number 'OF-123' already exists");
    }

    @Test
    void updateStockShouldRejectValueLowerThanReservedQuantity() {
        Part existingPart = Part.builder()
                .id(7)
                .name("Brake pad")
                .articleNumber("BP-777")
                .cost(new BigDecimal("50.00"))
                .stockQuantity(10)
                .reservedQuantity(4)
                .build();

        when(partRepository.findById(7)).thenReturn(Optional.of(existingPart));

        assertThatThrownBy(() -> partService.updateStock(7, new PartStockUpdateDTO(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock quantity cannot be lower than reserved quantity");
    }

    @Test
    void deleteShouldRejectPartUsedInOrders() {
        Part existingPart = Part.builder()
                .id(8)
                .name("Spark plug")
                .articleNumber("SP-8")
                .cost(new BigDecimal("12.00"))
                .stockQuantity(5)
                .reservedQuantity(0)
                .build();

        when(partRepository.findById(8)).thenReturn(Optional.of(existingPart));
        when(orderPartItemRepository.existsByPartId(8)).thenReturn(true);

        assertThatThrownBy(() -> partService.delete(8))
                .isInstanceOf(PartConflictException.class)
                .hasMessage("Part with id '8' is already used in orders");

        verify(partRepository, never()).delete(any(Part.class));
    }
}
