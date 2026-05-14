package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.order.entity.OrderStatus;

import java.time.Instant;

public interface OrderAvailabilityProjection {
    Integer getId();
    Integer getEmployeeId();
    Instant getPlannedVisitAt();
    Integer getPlannedSlotMinutes();
    OrderStatus getStatus();
}
