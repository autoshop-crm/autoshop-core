package com.vladko.autoshopcore.employee.dto;

import java.util.Set;

public record EmployeeSyncRequestDTO(
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        Boolean active
) {
}
