package com.vladko.autoshopcore.employee.controller;

import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.employee.dto.EmployeeAvailabilityResponseDTO;
import com.vladko.autoshopcore.entities.EmployeeType;
import com.vladko.autoshopcore.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<EmployeeResponseDTO>> getAll() {
        return ResponseEntity.ok(employeeService.getAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmployeeResponseDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(employeeService.search(query));
    }

    @GetMapping("/availability-search")
    public ResponseEntity<List<EmployeeAvailabilityResponseDTO>> searchAvailability(@RequestParam(required = false) String query,
                                                                                    @RequestParam(required = false) List<EmployeeType> roles,
                                                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant plannedVisitAt,
                                                                                    @RequestParam Integer slotMinutes,
                                                                                    @RequestParam(defaultValue = "20") Integer limit) {
        return ResponseEntity.ok(employeeService.searchAvailability(query, roles, plannedVisitAt, slotMinutes, limit));
    }
}
