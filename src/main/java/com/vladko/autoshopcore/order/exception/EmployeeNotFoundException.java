package com.vladko.autoshopcore.order.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Integer id) {
        super("Employee with id '%s' was not found".formatted(id));
    }
}
