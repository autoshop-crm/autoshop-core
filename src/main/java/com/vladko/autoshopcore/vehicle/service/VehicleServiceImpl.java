package com.vladko.autoshopcore.vehicle.service;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.exception.CustomerNotFoundException;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.vehicle.dto.VehicleCatalogLinkDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleCreateDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleResponseDTO;
import com.vladko.autoshopcore.vehicle.dto.VehicleUpdateDTO;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import com.vladko.autoshopcore.vehicle.exception.VehicleConflictException;
import com.vladko.autoshopcore.vehicle.exception.VehicleNotFoundException;
import com.vladko.autoshopcore.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public VehicleResponseDTO create(VehicleCreateDTO dto) {
        Customer customer = findCustomer(dto.getCustomerId());
        String normalizedVin = normalizeVin(dto.getVin());
        String normalizedLicensePlate = normalizeLicensePlate(dto.getLicensePlate());

        validateVinAvailability(normalizedVin, null);
        validateLicensePlateAvailability(normalizedLicensePlate, null);

        Vehicle vehicle = Vehicle.builder()
                .customer(customer)
                .brand(normalizeText(dto.getBrand()))
                .model(normalizeText(dto.getModel()))
                .vin(normalizedVin)
                .licensePlate(normalizedLicensePlate)
                .build();

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getById(Integer id) {
        return mapToResponse(findVehicle(id));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getByVin(String vin) {
        return mapToResponse(vehicleRepository.findByVin(normalizeVin(vin))
                .orElseThrow(() -> new VehicleNotFoundException(normalizeVin(vin))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> getAllByCustomerId(Integer customerId) {
        findCustomer(customerId);

        return vehicleRepository.findAllByCustomerIdOrderByIdAsc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public VehicleResponseDTO update(Integer id, VehicleUpdateDTO dto) {
        Vehicle vehicle = findVehicle(id);

        String normalizedBrand = normalizeOptionalText(dto.getBrand());
        String normalizedModel = normalizeOptionalText(dto.getModel());
        String normalizedVin = normalizeOptionalVin(dto.getVin());
        String normalizedLicensePlate = normalizeOptionalLicensePlate(dto.getLicensePlate());

        if (normalizedBrand != null) {
            vehicle.setBrand(normalizedBrand);
        }

        if (normalizedModel != null) {
            vehicle.setModel(normalizedModel);
        }

        if (normalizedVin != null && !normalizedVin.equals(vehicle.getVin())) {
            validateVinAvailability(normalizedVin, vehicle.getId());
            vehicle.setVin(normalizedVin);
        }

        if (normalizedLicensePlate != null && !normalizedLicensePlate.equals(vehicle.getLicensePlate())) {
            validateLicensePlateAvailability(normalizedLicensePlate, vehicle.getId());
            vehicle.setLicensePlate(normalizedLicensePlate);
        }

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        vehicleRepository.delete(findVehicle(id));
    }

    @Override
    @Transactional
    public VehicleResponseDTO linkCatalog(Integer id, VehicleCatalogLinkDTO dto) {
        Vehicle vehicle = findVehicle(id);
        vehicle.setUmapiType(normalizeCatalogType(dto.getType()));
        vehicle.setUmapiManufacturerId(dto.getManufacturerId());
        vehicle.setUmapiManufacturerName(normalizeText(dto.getManufacturerName()));
        vehicle.setUmapiModelSeriesId(dto.getModelSeriesId());
        vehicle.setUmapiModelSeriesName(normalizeText(dto.getModelSeriesName()));
        vehicle.setUmapiModificationId(dto.getModificationId());
        vehicle.setUmapiModificationName(normalizeText(dto.getModificationName()));
        vehicle.setUmapiEngineDescription(normalizeOptionalText(dto.getEngineDescription()));
        vehicle.setUmapiCatalogLinkedAt(Instant.now());

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponseDTO unlinkCatalog(Integer id) {
        Vehicle vehicle = findVehicle(id);
        vehicle.setUmapiType(null);
        vehicle.setUmapiManufacturerId(null);
        vehicle.setUmapiManufacturerName(null);
        vehicle.setUmapiModelSeriesId(null);
        vehicle.setUmapiModelSeriesName(null);
        vehicle.setUmapiModificationId(null);
        vehicle.setUmapiModificationName(null);
        vehicle.setUmapiEngineDescription(null);
        vehicle.setUmapiCatalogLinkedAt(null);

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    private Customer findCustomer(Integer customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private Vehicle findVehicle(Integer id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    private void validateVinAvailability(String vin, Integer currentVehicleId) {
        vehicleRepository.findByVin(vin)
                .filter(vehicle -> !vehicle.getId().equals(currentVehicleId))
                .ifPresent(vehicle -> {
                    throw new VehicleConflictException("Vehicle with vin '%s' already exists".formatted(vin));
                });
    }

    private void validateLicensePlateAvailability(String licensePlate, Integer currentVehicleId) {
        vehicleRepository.findByLicensePlate(licensePlate)
                .filter(vehicle -> !vehicle.getId().equals(currentVehicleId))
                .ifPresent(vehicle -> {
                    throw new VehicleConflictException(
                            "Vehicle with license plate '%s' already exists".formatted(licensePlate)
                    );
                });
    }

    private VehicleResponseDTO mapToResponse(Vehicle vehicle) {
        return VehicleResponseDTO.builder()
                .id(vehicle.getId())
                .customerId(vehicle.getCustomer().getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .vin(vehicle.getVin())
                .licensePlate(vehicle.getLicensePlate())
                .umapiType(vehicle.getUmapiType())
                .umapiManufacturerId(vehicle.getUmapiManufacturerId())
                .umapiManufacturerName(vehicle.getUmapiManufacturerName())
                .umapiModelSeriesId(vehicle.getUmapiModelSeriesId())
                .umapiModelSeriesName(vehicle.getUmapiModelSeriesName())
                .umapiModificationId(vehicle.getUmapiModificationId())
                .umapiModificationName(vehicle.getUmapiModificationName())
                .umapiEngineDescription(vehicle.getUmapiEngineDescription())
                .umapiCatalogLinkedAt(vehicle.getUmapiCatalogLinkedAt())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    private String normalizeVin(String vin) {
        return normalizeText(vin).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalVin(String vin) {
        String normalizedVin = normalizeOptionalText(vin);
        return normalizedVin == null ? null : normalizedVin.toUpperCase(Locale.ROOT);
    }

    private String normalizeLicensePlate(String licensePlate) {
        return normalizeText(licensePlate).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalLicensePlate(String licensePlate) {
        String normalizedLicensePlate = normalizeOptionalText(licensePlate);
        return normalizedLicensePlate == null ? null : normalizedLicensePlate.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException("Value must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeCatalogType(String type) {
        String normalizedType = normalizeText(type).toUpperCase(Locale.ROOT);
        if (!"PC".equals(normalizedType)) {
            throw new IllegalArgumentException("Catalog vehicle type is not supported");
        }
        return normalizedType;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
