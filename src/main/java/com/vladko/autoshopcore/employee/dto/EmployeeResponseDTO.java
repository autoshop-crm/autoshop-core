package com.vladko.autoshopcore.employee.dto;

import com.vladko.autoshopcore.entities.EmployeeType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EmployeeResponseDTO {

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private EmployeeType function;
    private Instant createdAt;
}
