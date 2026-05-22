package com.vladko.autoshopcore.order.timeline.repository;

import com.vladko.autoshopcore.order.timeline.entity.OrderTimelineEntry;
import com.vladko.autoshopcore.shared.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface OrderTimelineEntryRepository extends BaseRepository<OrderTimelineEntry, Integer> {
    List<OrderTimelineEntry> findAllByOrderIdOrderByOccurredAtAscIdAsc(Integer orderId);
    Optional<OrderTimelineEntry> findByOrderIdAndDedupeKey(Integer orderId, String dedupeKey);
}
