package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.PartCreateDTO;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.dto.PartStockUpdateDTO;
import com.vladko.autoshopcore.parts.dto.PartUpdateDTO;
import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.parts.exception.PartConflictException;
import com.vladko.autoshopcore.parts.exception.PartNotFoundException;
import com.vladko.autoshopcore.parts.repository.OrderPartItemRepository;
import com.vladko.autoshopcore.parts.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PartServiceImpl implements PartService {

    private final PartRepository partRepository;
    private final OrderPartItemRepository orderPartItemRepository;

    @Override
    @Transactional
    public PartResponseDTO create(PartCreateDTO dto) {
        String normalizedArticleNumber = normalizeArticleNumber(dto.getArticleNumber());
        validateArticleNumberAvailability(normalizedArticleNumber, null);

        Part part = Part.builder()
                .brand(normalizeOptionalText(dto.getBrand()))
                .name(normalizeText(dto.getName()))
                .articleNumber(normalizedArticleNumber)
                .cost(normalizeMoney(dto.getCost()))
                .stockQuantity(0)
                .reservedQuantity(0)
                .build();

        return mapToResponse(partRepository.save(part));
    }

    @Override
    @Transactional(readOnly = true)
    public PartResponseDTO getById(Integer id) {
        return mapToResponse(findPart(id));
    }

    @Override
    @Transactional
    public PartResponseDTO update(Integer id, PartUpdateDTO dto) {
        Part part = findPart(id);

        String normalizedBrand = normalizeOptionalText(dto.getBrand());
        String normalizedName = normalizeOptionalText(dto.getName());
        String normalizedArticleNumber = normalizeOptionalArticleNumber(dto.getArticleNumber());
        BigDecimal normalizedCost = normalizeOptionalMoney(dto.getCost());

        if (normalizedBrand != null) {
            part.setBrand(normalizedBrand);
        }

        if (normalizedName != null) {
            part.setName(normalizedName);
        }

        if (normalizedArticleNumber != null && !normalizedArticleNumber.equals(part.getArticleNumber())) {
            validateArticleNumberAvailability(normalizedArticleNumber, part.getId());
            part.setArticleNumber(normalizedArticleNumber);
        }

        if (normalizedCost != null) {
            part.setCost(normalizedCost);
        }

        return mapToResponse(partRepository.save(part));
    }

    @Override
    @Transactional
    public PartResponseDTO updateStock(Integer id, PartStockUpdateDTO dto) {
        Part part = findPart(id);
        Integer stockQuantity = normalizeStockQuantity(dto.getStockQuantity());

        if (stockQuantity < part.getReservedQuantity()) {
            throw new IllegalArgumentException("Stock quantity cannot be lower than reserved quantity");
        }

        part.setStockQuantity(stockQuantity);
        return mapToResponse(partRepository.save(part));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Part part = findPart(id);
        if (orderPartItemRepository.existsByPartId(id)) {
            throw new PartConflictException("Part with id '%s' is already used in orders".formatted(id));
        }
        partRepository.delete(part);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartResponseDTO> search(String articleNumber, String brand, String name, Boolean availableOnly) {
        String normalizedArticleNumber = normalizeOptionalArticleNumber(articleNumber);
        String normalizedBrand = normalizeOptionalText(brand);
        String normalizedName = normalizeOptionalText(name);
        boolean filterAvailableOnly = Boolean.TRUE.equals(availableOnly);

        return partRepository.findAll()
                .stream()
                .filter(part -> matchesArticleNumber(part, normalizedArticleNumber))
                .filter(part -> matchesBrand(part, normalizedBrand))
                .filter(part -> matchesName(part, normalizedName))
                .filter(part -> !filterAvailableOnly || getAvailableQuantity(part) > 0)
                .sorted(Comparator.comparing(Part::getId))
                .map(this::mapToResponse)
                .toList();
    }

    private Part findPart(Integer id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new PartNotFoundException(id));
    }

    private void validateArticleNumberAvailability(String articleNumber, Integer currentPartId) {
        partRepository.findByArticleNumber(articleNumber)
                .filter(part -> !part.getId().equals(currentPartId))
                .ifPresent(part -> {
                    throw new PartConflictException(
                            "Part with article number '%s' already exists".formatted(articleNumber)
                    );
                });
    }

    private PartResponseDTO mapToResponse(Part part) {
        return PartResponseDTO.builder()
                .id(part.getId())
                .brand(part.getBrand())
                .name(part.getName())
                .articleNumber(part.getArticleNumber())
                .cost(part.getCost())
                .stockQuantity(part.getStockQuantity())
                .reservedQuantity(part.getReservedQuantity())
                .availableQuantity(getAvailableQuantity(part))
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }

    private boolean matchesArticleNumber(Part part, String articleNumber) {
        return articleNumber == null || part.getArticleNumber().equals(articleNumber);
    }

    private boolean matchesBrand(Part part, String brand) {
        return brand == null
                || (part.getBrand() != null
                && part.getBrand().toLowerCase(Locale.ROOT).contains(brand.toLowerCase(Locale.ROOT)));
    }

    private boolean matchesName(Part part, String name) {
        return name == null || part.getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT));
    }

    private int getAvailableQuantity(Part part) {
        return part.getStockQuantity() - part.getReservedQuantity();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal normalizedValue = normalizeOptionalMoney(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        return normalizedValue;
    }

    private BigDecimal normalizeOptionalMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Value must not be negative");
        }
        return value;
    }

    private Integer normalizeStockQuantity(Integer stockQuantity) {
        if (stockQuantity == null) {
            throw new IllegalArgumentException("Stock quantity must not be null");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity must not be negative");
        }
        return stockQuantity;
    }

    private String normalizeArticleNumber(String articleNumber) {
        String normalizedArticleNumber = normalizeOptionalArticleNumber(articleNumber);
        if (normalizedArticleNumber == null) {
            throw new IllegalArgumentException("Value must not be blank");
        }
        return normalizedArticleNumber;
    }

    private String normalizeOptionalArticleNumber(String articleNumber) {
        String normalizedArticleNumber = normalizeOptionalText(articleNumber);
        return normalizedArticleNumber == null ? null : normalizedArticleNumber.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException("Value must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
