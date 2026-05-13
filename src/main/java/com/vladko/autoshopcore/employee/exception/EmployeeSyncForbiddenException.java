package com.vladko.autoshopcore.employee.exception;

public class EmployeeSyncForbiddenException extends RuntimeException {

    public EmployeeSyncForbiddenException() {
        super("Employee sync token is invalid");
    }
}
