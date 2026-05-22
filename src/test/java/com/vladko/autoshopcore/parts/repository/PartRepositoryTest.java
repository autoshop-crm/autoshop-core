package com.vladko.autoshopcore.parts.repository;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.parts.entity.Part;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PartRepositoryTest {

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private OrderPartItemRepository orderPartItemRepository;

    @Test
    void findByArticleNumberShouldReturnPersistedPart() {
        partRepository.save(Part.builder()
                .brand("Bosch")
                .name("Oil filter")
                .articleNumber("OF-123")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(10)
                .reservedQuantity(2)
                .build());

        assertThat(partRepository.findByArticleNumber("OF-123"))
                .isPresent()
                .get()
                .extracting(Part::getName)
                .isEqualTo("Oil filter");
    }

    @Test
    void articleNumberShouldBeUnique() {
        partRepository.saveAndFlush(Part.builder()
                .brand("Bosch")
                .name("Oil filter")
                .articleNumber("OF-123")
                .cost(new BigDecimal("15.50"))
                .stockQuantity(10)
                .reservedQuantity(0)
                .build());

        assertThatThrownBy(() -> partRepository.saveAndFlush(Part.builder()
                .brand("Mann")
                .name("Air filter")
                .articleNumber("OF-123")
                .cost(new BigDecimal("20.00"))
                .stockQuantity(3)
                .reservedQuantity(0)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
