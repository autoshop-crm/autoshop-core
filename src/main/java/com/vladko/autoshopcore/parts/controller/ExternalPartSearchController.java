package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.ExternalPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.ExternalPartSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parts/external")
@RequiredArgsConstructor
public class ExternalPartSearchController {

    private final ExternalPartSearchService externalPartSearchService;

    @GetMapping("/search")
    public ResponseEntity<ExternalPartSearchResponseDTO> search(@RequestParam String articleNumber,
                                                                @RequestParam(required = false) String brand,
                                                                @RequestParam(required = false) Integer limit,
                                                                @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(externalPartSearchService.search(articleNumber, brand, limit, offset));
    }
}
