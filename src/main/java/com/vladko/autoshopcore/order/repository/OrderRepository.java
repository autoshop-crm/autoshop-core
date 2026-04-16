package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends BaseRepository<Order, Integer> {

    List<Order> findAllByCustomerIdOrderByIdDesc(Integer customerId);

    List<Order> findAllByVehicleIdOrderByIdDesc(Integer vehicleId);

    List<Order> findAllByStatusOrderByIdDesc(OrderStatus status);

    List<Order> findAllByStatusInOrderByIdDesc(Collection<OrderStatus> statuses);
}
