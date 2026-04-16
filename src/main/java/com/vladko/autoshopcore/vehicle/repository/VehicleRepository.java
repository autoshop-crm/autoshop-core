package com.vladko.autoshopcore.vehicle.repository;

import com.vladko.autoshopcore.shared.repository.BaseRepository;
import com.vladko.autoshopcore.vehicle.entity.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends BaseRepository<Vehicle, Integer> {

    Optional<Vehicle> findByVin(String vin);

    boolean existsByVin(String vin);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    List<Vehicle> findAllByCustomerIdOrderByIdAsc(Integer customerId);
}
