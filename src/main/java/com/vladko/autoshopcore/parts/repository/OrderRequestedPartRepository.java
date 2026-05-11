package com.vladko.autoshopcore.parts.repository;

import com.vladko.autoshopcore.parts.entity.OrderRequestedPart;
import com.vladko.autoshopcore.parts.entity.OrderRequestedPartStatus;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface OrderRequestedPartRepository extends BaseRepository<OrderRequestedPart, Integer> {

    @EntityGraph(attributePaths = {"matchedLocalPart"})
    List<OrderRequestedPart> findAllByOrderIdOrderByIdAsc(Integer orderId);

    @EntityGraph(attributePaths = {"matchedLocalPart"})
    Optional<OrderRequestedPart> findByIdAndOrderId(Integer id, Integer orderId);

    List<OrderRequestedPart> findAllByOrderIdAndStatusInOrderByIdAsc(Integer orderId, List<OrderRequestedPartStatus> statuses);
}
