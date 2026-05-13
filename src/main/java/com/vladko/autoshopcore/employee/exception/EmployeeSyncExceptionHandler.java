package com.vladko.autoshopcore.employee.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class EmployeeSyncExceptionHandler {

    @ExceptionHandler(EmployeeSyncForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(EmployeeSyncForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }
}
