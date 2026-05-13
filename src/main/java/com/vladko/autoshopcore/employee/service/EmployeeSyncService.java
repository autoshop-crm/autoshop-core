package com.vladko.autoshopcore.employee.service;

import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.employee.dto.EmployeeSyncRequestDTO;

public interface EmployeeSyncService {

    EmployeeResponseDTO sync(EmployeeSyncRequestDTO request);
}
