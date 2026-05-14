package com.vladko.autoshopcore.order.service;

import com.vladko.autoshopcore.order.entity.LegacyOrderStatus;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class LegacyOrderStatusProjector {

    public LegacyOrderStatus project(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case NEW, WAITING_FOR_VISIT, ACCEPTED -> LegacyOrderStatus.NEW;
            case IN_PROGRESS, DIAGNOSIS_IN_PROGRESS, WAITING_FOR_OWNER_APPROVAL, WAITING_FOR_PART, REPAIR_IN_PROGRESS, READY_FOR_OWNER -> LegacyOrderStatus.IN_PROGRESS;
            case COMPLETED, HANDED_OVER -> LegacyOrderStatus.COMPLETED;
            case CANCELLED, CANCELLED_NO_SHOW, CANCELLED_BY_CUSTOMER, CANCELLED_INTERNAL -> LegacyOrderStatus.CANCELLED;
        };
    }
}
