package com.vladko.autoshopcore.vehicle.repository;

import com.vladko.autoshopcore.PostgresTestcontainersConfiguration;
import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.client.repository.CustomerRepository;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PostgresTestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findByVinShouldReturnPersistedVehicle() {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Petr")
                .lastName("Ivanov")
                .email("petr@example.com")
                .phoneNumber("+79990000031")
                .build());

        vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .brand("Toyota")
                .model("Camry")
                .vin("JT2BG22KXV0123456")
                .licensePlate("A123BC77")
                .build());

        assertThat(vehicleRepository.findByVin("JT2BG22KXV0123456"))
                .isPresent()
                .get()
                .extracting(Vehicle::getLicensePlate)
                .isEqualTo("A123BC77");
    }

    @Test
    void findAllByCustomerIdOrderByIdAscShouldReturnCustomerVehicles() {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Olga")
                .lastName("Petrova")
                .email("olga@example.com")
                .phoneNumber("+79990000032")
                .build());

        vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .brand("Skoda")
                .model("Octavia")
                .vin("TMBDC41U962123456")
                .licensePlate("B456CD77")
                .build());
        vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .brand("BMW")
                .model("X5")
                .vin("WBAFB71010LX12345")
                .licensePlate("C789DE77")
                .build());

        List<Vehicle> vehicles = vehicleRepository.findAllByCustomerIdOrderByIdAsc(customer.getId());

        assertThat(vehicles)
                .hasSize(2)
                .extracting(Vehicle::getVin)
                .containsExactly("TMBDC41U962123456", "WBAFB71010LX12345");
    }
}
