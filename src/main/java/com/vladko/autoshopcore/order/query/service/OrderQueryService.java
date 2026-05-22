package com.vladko.autoshopcore.order.query.service;

import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.order.query.dto.OrderQueueSummaryDTO;
import com.vladko.autoshopcore.order.query.dto.OrderSearchResponseDTO;

import java.time.Instant;

public interface OrderQueryService {
    OrderSearchResponseDTO search(Integer customerId,
                                  Integer vehicleId,
                                  OrderStatus status,
                                  Integer employeeId,
                                  Instant plannedFrom,
                                  Instant plannedTo,
                                  String q,
                                  int page,
                                  int size);

    OrderQueueSummaryDTO queueSummary();
}
