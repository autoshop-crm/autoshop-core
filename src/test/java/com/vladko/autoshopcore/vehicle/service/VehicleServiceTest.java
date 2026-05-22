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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Test
    void createShouldPersistNormalizedVehicle() {
        Customer customer = Customer.builder().id(3).build();
        VehicleCreateDTO dto = VehicleCreateDTO.builder()
                .customerId(3)
                .brand(" Toyota ")
                .model(" Camry ")
                .vin(" jt2bg22kxv0123456 ")
                .licensePlate(" a123bc77 ")
                .build();

        Vehicle savedVehicle = Vehicle.builder()
                .id(11)
                .customer(customer)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0123456")
                .licensePlate("A123BC77")
                .createdAt(Instant.parse("2026-04-13T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-13T10:15:30Z"))
                .build();

        when(customerRepository.findById(3)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findByVin("JT2BG22KXV0123456")).thenReturn(Optional.empty());
        when(vehicleRepository.findByLicensePlate("A123BC77")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        VehicleResponseDTO response = vehicleService.create(dto);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());

        Vehicle vehicleToSave = captor.getValue();
        assertThat(vehicleToSave.getCustomer()).isEqualTo(customer);
        assertThat(vehicleToSave.getBrand()).isEqualTo("Toyota");
        assertThat(vehicleToSave.getModel()).isEqualTo("Camry");
        assertThat(vehicleToSave.getVin()).isEqualTo("JT2BG22KXV0123456");
        assertThat(vehicleToSave.getLicensePlate()).isEqualTo("A123BC77");

        assertThat(response.getId()).isEqualTo(11);
        assertThat(response.getCustomerId()).isEqualTo(3);
        assertThat(response.getVin()).isEqualTo("JT2BG22KXV0123456");
    }

    @Test
    void createShouldThrowConflictWhenVinAlreadyExists() {
        Customer customer = Customer.builder().id(5).build();
        VehicleCreateDTO dto = VehicleCreateDTO.builder()
                .customerId(5)
                .brand("BMW")
                .model("X5")
                .vin("WBAFB71010LX12345")
                .licensePlate("X555XX77")
                .build();

        when(customerRepository.findById(5)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findByVin("WBAFB71010LX12345"))
                .thenReturn(Optional.of(Vehicle.builder().id(8).vin("WBAFB71010LX12345").build()));

        assertThatThrownBy(() -> vehicleService.create(dto))
                .isInstanceOf(VehicleConflictException.class)
                .hasMessage("Vehicle with vin 'WBAFB71010LX12345' already exists");

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createShouldThrowNotFoundWhenCustomerMissing() {
        VehicleCreateDTO dto = VehicleCreateDTO.builder()
                .customerId(404)
                .brand("Audi")
                .model("A6")
                .vin("WAUZZZ4F96N123456")
                .licensePlate("M001MM77")
                .build();

        when(customerRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.create(dto))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer with id '404' was not found");
    }

    @Test
    void getByIdShouldThrowWhenVehicleMissing() {
        when(vehicleRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getById(404))
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessage("Vehicle with id '404' was not found");
    }

    @Test
    void updateShouldChangeOnlyProvidedFieldsAndNormalizeValues() {
        Customer customer = Customer.builder().id(7).build();
        Vehicle existingVehicle = Vehicle.builder()
                .id(12)
                .customer(customer)
                .brand("Skoda")
                .model("Octavia")
                .vin("TMBDC41U962123456")
                .licensePlate("B456CD77")
                .createdAt(Instant.parse("2026-04-13T10:15:30Z"))
                .updatedAt(Instant.parse("2026-04-13T10:15:30Z"))
                .build();
        Vehicle updatedVehicle = Vehicle.builder()
                .id(12)
                .customer(customer)
                .brand("Skoda")
                .model("Superb")
                .vin("TMBDC41U962123456")
                .licensePlate("K700KT77")
                .createdAt(existingVehicle.getCreatedAt())
                .updatedAt(Instant.parse("2026-04-13T11:15:30Z"))
                .build();
        VehicleUpdateDTO dto = VehicleUpdateDTO.builder()
                .model(" Superb ")
                .licensePlate(" k700kt77 ")
                .build();

        when(vehicleRepository.findById(12)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.findByLicensePlate("K700KT77")).thenReturn(Optional.empty());
        when(vehicleRepository.save(existingVehicle)).thenReturn(updatedVehicle);

        VehicleResponseDTO response = vehicleService.update(12, dto);

        assertThat(existingVehicle.getBrand()).isEqualTo("Skoda");
        assertThat(existingVehicle.getModel()).isEqualTo("Superb");
        assertThat(existingVehicle.getLicensePlate()).isEqualTo("K700KT77");
        assertThat(response.getLicensePlate()).isEqualTo("K700KT77");
    }

    @Test
    void linkCatalogShouldPersistUmapiModification() {
        Customer customer = Customer.builder().id(7).build();
        Vehicle vehicle = Vehicle.builder()
                .id(12)
                .customer(customer)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0123456")
                .licensePlate("A123BC77")
                .build();
        VehicleCatalogLinkDTO dto = VehicleCatalogLinkDTO.builder()
                .type(" pc ")
                .manufacturerId(111)
                .manufacturerName(" TOYOTA ")
                .modelSeriesId(222)
                .modelSeriesName(" CAMRY ")
                .modificationId(333)
                .modificationName(" Camry 2.5 ")
                .engineDescription(" 2.5, petrol ")
                .build();

        when(vehicleRepository.findById(12)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        VehicleResponseDTO response = vehicleService.linkCatalog(12, dto);

        assertThat(vehicle.getUmapiType()).isEqualTo("PC");
        assertThat(vehicle.getUmapiManufacturerName()).isEqualTo("TOYOTA");
        assertThat(vehicle.getUmapiModificationId()).isEqualTo(333);
        assertThat(vehicle.getUmapiCatalogLinkedAt()).isNotNull();
        assertThat(response.getUmapiModificationName()).isEqualTo("Camry 2.5");
    }

    @Test
    void unlinkCatalogShouldClearUmapiModification() {
        Customer customer = Customer.builder().id(7).build();
        Vehicle vehicle = Vehicle.builder()
                .id(12)
                .customer(customer)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0123456")
                .licensePlate("A123BC77")
                .umapiType("PC")
                .umapiManufacturerId(111)
                .umapiManufacturerName("TOYOTA")
                .umapiModelSeriesId(222)
                .umapiModelSeriesName("CAMRY")
                .umapiModificationId(333)
                .umapiModificationName("Camry 2.5")
                .umapiCatalogLinkedAt(Instant.parse("2026-04-22T10:00:00Z"))
                .build();

        when(vehicleRepository.findById(12)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        VehicleResponseDTO response = vehicleService.unlinkCatalog(12);

        assertThat(vehicle.getUmapiModificationId()).isNull();
        assertThat(vehicle.getUmapiCatalogLinkedAt()).isNull();
        assertThat(response.getUmapiModificationId()).isNull();
    }
}
