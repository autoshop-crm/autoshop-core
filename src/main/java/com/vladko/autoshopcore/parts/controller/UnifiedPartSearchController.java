package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.UnifiedPartSearchResponseDTO;
import com.vladko.autoshopcore.parts.service.UnifiedPartSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parts/unified")
@RequiredArgsConstructor
public class UnifiedPartSearchController {

    private final UnifiedPartSearchService unifiedPartSearchService;

    @GetMapping("/search")
    public ResponseEntity<UnifiedPartSearchResponseDTO> search(@RequestParam String articleNumber,
                                                               @RequestParam(required = false) String brand,
                                                               @RequestParam(required = false) Boolean availableOnly,
                                                               @RequestParam(required = false) Integer limit,
                                                               @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(unifiedPartSearchService.search(articleNumber, brand, availableOnly, limit, offset));
    }
}
