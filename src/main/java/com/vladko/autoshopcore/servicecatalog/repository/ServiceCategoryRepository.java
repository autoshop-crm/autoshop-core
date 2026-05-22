package com.vladko.autoshopcore.servicecatalog.repository;

import com.vladko.autoshopcore.servicecatalog.entity.ServiceCategory;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;

public interface ServiceCategoryRepository extends BaseRepository<ServiceCategory, Integer> {
    List<ServiceCategory> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
}
