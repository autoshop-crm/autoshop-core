package com.vladko.autoshopcore.order.approval.repository;

import com.vladko.autoshopcore.order.approval.entity.OrderApprovalDecision;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface OrderApprovalDecisionRepository extends BaseRepository<OrderApprovalDecision, Integer> {
    Optional<OrderApprovalDecision> findByIdempotencyKey(String idempotencyKey);
    List<OrderApprovalDecision> findAllByApprovalRequestIdOrderByIdAsc(Integer approvalRequestId);
}
