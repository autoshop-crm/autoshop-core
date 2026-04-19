package com.vladko.autoshopcore.parts.service;

import com.vladko.autoshopcore.parts.dto.PartCreateDTO;
import com.vladko.autoshopcore.parts.dto.PartResponseDTO;
import com.vladko.autoshopcore.parts.dto.PartStockUpdateDTO;
import com.vladko.autoshopcore.parts.dto.PartUpdateDTO;

import java.util.List;

public interface PartService {

    PartResponseDTO create(PartCreateDTO dto);

    PartResponseDTO getById(Integer id);

    PartResponseDTO update(Integer id, PartUpdateDTO dto);

    PartResponseDTO updateStock(Integer id, PartStockUpdateDTO dto);

    void delete(Integer id);

    List<PartResponseDTO> search(String articleNumber, String brand, String name, Boolean availableOnly);
}
