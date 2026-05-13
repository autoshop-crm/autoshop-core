package com.vladko.autoshopcore.employee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.employee-sync")
public record EmployeeSyncProperties(String token) {
}
