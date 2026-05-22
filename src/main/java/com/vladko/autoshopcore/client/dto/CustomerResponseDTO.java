package com.vladko.autoshopcore.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerResponseDTO {
    private Integer id;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String email;

    private Boolean emailVerified;

    private Instant createdAt;

    private Instant updatedAt;
}
