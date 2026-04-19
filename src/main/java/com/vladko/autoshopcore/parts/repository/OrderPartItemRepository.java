package com.vladko.autoshopcore.parts.repository;

import com.vladko.autoshopcore.parts.entity.OrderPartItem;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface OrderPartItemRepository extends BaseRepository<OrderPartItem, Integer> {

    List<OrderPartItem> findAllByOrderIdOrderByIdAsc(Integer orderId);

    Optional<OrderPartItem> findByIdAndOrderId(Integer id, Integer orderId);

    Optional<OrderPartItem> findByOrderIdAndPartId(Integer orderId, Integer partId);

    boolean existsByPartId(Integer partId);
}
