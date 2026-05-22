package com.vladko.autoshopcore.employee.service;

import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.employee.dto.EmployeeAvailabilityResponseDTO;
import com.vladko.autoshopcore.entities.EmployeeType;

import java.time.Instant;
import java.util.Collection;

import java.util.List;

public interface EmployeeService {

    List<EmployeeResponseDTO> getAll();

    List<EmployeeResponseDTO> search(String query);

    List<EmployeeAvailabilityResponseDTO> searchAvailability(String query,
                                                            Collection<EmployeeType> roles,
                                                            Instant plannedVisitAt,
                                                            Integer slotMinutes,
                                                            Integer limit);
}
