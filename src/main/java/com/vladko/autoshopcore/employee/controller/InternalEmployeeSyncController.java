package com.vladko.autoshopcore.employee.controller;

import com.vladko.autoshopcore.employee.config.EmployeeSyncProperties;
import com.vladko.autoshopcore.employee.dto.EmployeeResponseDTO;
import com.vladko.autoshopcore.employee.dto.EmployeeSyncRequestDTO;
import com.vladko.autoshopcore.employee.exception.EmployeeSyncForbiddenException;
import com.vladko.autoshopcore.employee.service.EmployeeSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/employees")
@RequiredArgsConstructor
public class InternalEmployeeSyncController {

    public static final String SYNC_TOKEN_HEADER = "X-Employee-Sync-Token";

    private final EmployeeSyncService employeeSyncService;
    private final EmployeeSyncProperties employeeSyncProperties;

    @PostMapping("/sync")
    public ResponseEntity<EmployeeResponseDTO> sync(@RequestHeader(SYNC_TOKEN_HEADER) String syncToken,
                                                    @RequestBody EmployeeSyncRequestDTO request) {
        validateSyncToken(syncToken);
        return ResponseEntity.ok(employeeSyncService.sync(request));
    }

    private void validateSyncToken(String syncToken) {
        if (!StringUtils.hasText(syncToken) || !syncToken.equals(employeeSyncProperties.token())) {
            throw new EmployeeSyncForbiddenException();
        }
    }
}
