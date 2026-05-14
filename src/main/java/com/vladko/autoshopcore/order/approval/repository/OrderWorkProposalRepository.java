package com.vladko.autoshopcore.order.approval.repository;

import com.vladko.autoshopcore.order.approval.entity.OrderWorkProposal;
import com.vladko.autoshopcore.order.approval.entity.OrderWorkProposalStatus;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;

public interface OrderWorkProposalRepository extends BaseRepository<OrderWorkProposal, Integer> {
    List<OrderWorkProposal> findAllByOrderIdOrderByIdAsc(Integer orderId);
    List<OrderWorkProposal> findAllByOrderIdAndStatusInOrderByIdAsc(Integer orderId, List<OrderWorkProposalStatus> statuses);
}
