package com.vladko.autoshopcore.order.approval.repository;

import com.vladko.autoshopcore.order.approval.entity.OrderApprovalRequest;
import com.vladko.autoshopcore.order.approval.entity.OrderApprovalRequestStatus;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderApprovalRequestRepository extends BaseRepository<OrderApprovalRequest, Integer> {
    List<OrderApprovalRequest> findAllByOrderIdOrderByIdAsc(Integer orderId);
    Optional<OrderApprovalRequest> findByRequestToken(String requestToken);
    boolean existsByOrderIdAndStatus(Integer orderId, OrderApprovalRequestStatus status);

    @Query("select r from OrderApprovalRequest r where r.order.id = :orderId and r.status = :status order by r.id asc")
    List<OrderApprovalRequest> findAllByOrderIdAndStatus(@Param("orderId") Integer orderId,
                                                         @Param("status") OrderApprovalRequestStatus status);
}
