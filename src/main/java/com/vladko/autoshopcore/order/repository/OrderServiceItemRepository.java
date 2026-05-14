package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.entities.OrderServiceItem;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;

public interface OrderServiceItemRepository extends BaseRepository<OrderServiceItem, Integer> {
    List<OrderServiceItem> findAllByOrderIdOrderByIdAsc(Integer orderId);
    void deleteAllByOrderId(Integer orderId);
}
