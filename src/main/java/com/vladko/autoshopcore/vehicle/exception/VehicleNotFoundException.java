package com.vladko.autoshopcore.vehicle.exception;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(Integer id) {
        super("Vehicle with id '%s' was not found".formatted(id));
    }

    public VehicleNotFoundException(String vin) {
        super("Vehicle with vin '%s' was not found".formatted(vin));
    }
}
