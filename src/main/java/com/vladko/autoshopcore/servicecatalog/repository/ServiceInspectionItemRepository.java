package com.vladko.autoshopcore.servicecatalog.repository;

import com.vladko.autoshopcore.servicecatalog.entity.ServiceInspectionItem;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;

public interface ServiceInspectionItemRepository extends BaseRepository<ServiceInspectionItem, Integer> {
    List<ServiceInspectionItem> findAllByServiceIdOrderByIdAsc(Integer serviceId);
}
