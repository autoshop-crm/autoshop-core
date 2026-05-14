package com.vladko.autoshopcore.order.query.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderQueueSummaryDTO {
    long waitingForVisit;
    long accepted;
    long diagnosisInProgress;
    long waitingForOwnerApproval;
    long waitingForPart;
    long repairInProgress;
    long readyForOwner;
}
