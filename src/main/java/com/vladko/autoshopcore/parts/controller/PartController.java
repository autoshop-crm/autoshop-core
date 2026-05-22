package com.vladko.autoshopcore.parts.controller;

import com.vladko.autoshopcore.parts.dto.PartCreateDTO;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.dto.PartStockUpdateDTO;
import com.vladko.autoshopcore.parts.dto.PartUpdateDTO;
import com.vladko.autoshopcore.parts.service.PartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    @PostMapping
    public ResponseEntity<PartResponseDTO> create(@Valid @RequestBody PartCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(partService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartResponseDTO> update(@PathVariable Integer id,
                                                  @Valid @RequestBody PartUpdateDTO dto) {
        return ResponseEntity.ok(partService.update(id, dto));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<PartResponseDTO> updateStock(@PathVariable Integer id,
                                                       @Valid @RequestBody PartStockUpdateDTO dto) {
        return ResponseEntity.ok(partService.updateStock(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        partService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PartResponseDTO>> search(@RequestParam(required = false) String articleNumber,
                                                        @RequestParam(required = false) String brand,
                                                        @RequestParam(required = false) String name,
                                                        @RequestParam(required = false) Boolean availableOnly) {
        return ResponseEntity.ok(partService.search(articleNumber, brand, name, availableOnly));
    }
}
