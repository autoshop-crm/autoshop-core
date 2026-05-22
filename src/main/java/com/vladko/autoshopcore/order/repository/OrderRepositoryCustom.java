package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;

import java.time.Instant;
import java.util.List;

public interface OrderRepositoryCustom {

    List<Order> searchForCrm(Integer customerId,
                             Integer vehicleId,
                             OrderStatus status,
                             Integer employeeId,
                             Instant plannedFrom,
                             Instant plannedTo);

    List<Order> searchForCrmByQuery(Integer customerId,
                                    Integer vehicleId,
                                    OrderStatus status,
                                    Integer employeeId,
                                    Instant plannedFrom,
                                    Instant plannedTo,
                                    String q);
}
