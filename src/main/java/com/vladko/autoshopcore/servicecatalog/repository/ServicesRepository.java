package com.vladko.autoshopcore.servicecatalog.repository;

import com.vladko.autoshopcore.entities.Services;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;

public interface ServicesRepository extends BaseRepository<Services, Integer> {
    List<Services> findAllByActiveTrueOrderByNameAsc();
    List<Services> findAllByCategoryIdOrderByNameAsc(Integer categoryId);
}
