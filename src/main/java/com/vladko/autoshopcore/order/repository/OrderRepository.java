package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends BaseRepository<Order, Integer>, OrderRepositoryCustom {

    List<Order> findAllByCustomerIdOrderByIdDesc(Integer customerId);

    List<Order> findAllByVehicleIdOrderByIdDesc(Integer vehicleId);

    List<Order> findAllByEmployeeIdOrderByIdDesc(Integer employeeId);

    List<Order> findAllByStatusOrderByIdDesc(OrderStatus status);

    List<Order> findAllByStatusInOrderByIdDesc(Collection<OrderStatus> statuses);

    List<Order> findAllByPlannedVisitAtBetweenOrderByPlannedVisitAtAscIdAsc(Instant from, Instant to);

    List<Order> findAllByPlannedVisitAtBetweenAndEmployeeIsNullOrderByPlannedVisitAtAscIdAsc(Instant from, Instant to);

    @Query("select o from CustomerOrder o where o.plannedVisitAt between :from and :to and o.status in :statuses order by o.plannedVisitAt asc, o.id asc")
    List<Order> findBookingsByWindowAndStatuses(@Param("from") Instant from,
                                                @Param("to") Instant to,
                                                @Param("statuses") Collection<OrderStatus> statuses);

    @Query(value = """
            SELECT o.id AS id,
                   o.employee_id AS employeeId,
                   o.planned_visit_at AS plannedVisitAt,
                   o.planned_slot_minutes AS plannedSlotMinutes,
                   o.status AS status
            FROM orders o
            WHERE o.employee_id IN (:employeeIds)
              AND o.status IN (:statuses)
              AND o.planned_visit_at IS NOT NULL
              AND o.planned_slot_minutes IS NOT NULL
              AND o.planned_visit_at < :requestedEnd
              AND (o.planned_visit_at + (o.planned_slot_minutes * INTERVAL '1 minute')) > :requestedStart
            ORDER BY o.employee_id ASC, o.planned_visit_at ASC, o.id ASC
            """, nativeQuery = true)
    List<OrderAvailabilityProjection> findAvailabilityConflicts(@Param("employeeIds") Collection<Integer> employeeIds,
                                                                @Param("statuses") Collection<String> statuses,
                                                                @Param("requestedStart") Instant requestedStart,
                                                                @Param("requestedEnd") Instant requestedEnd);

    @Query(value = """
            SELECT o.id AS id,
                   o.employee_id AS employeeId,
                   o.planned_visit_at AS plannedVisitAt,
                   o.planned_slot_minutes AS plannedSlotMinutes,
                   o.status AS status
            FROM orders o
            WHERE o.employee_id = :employeeId
              AND (:excludeOrderId IS NULL OR o.id <> :excludeOrderId)
              AND o.status IN (:statuses)
              AND o.planned_visit_at IS NOT NULL
              AND o.planned_slot_minutes IS NOT NULL
              AND o.planned_visit_at < :requestedEnd
              AND (o.planned_visit_at + (o.planned_slot_minutes * INTERVAL '1 minute')) > :requestedStart
            ORDER BY o.planned_visit_at ASC, o.id ASC
            LIMIT 1
            """, nativeQuery = true)
    List<OrderAvailabilityProjection> findFirstAvailabilityConflict(@Param("employeeId") Integer employeeId,
                                                                    @Param("excludeOrderId") Integer excludeOrderId,
                                                                    @Param("statuses") Collection<String> statuses,
                                                                    @Param("requestedStart") Instant requestedStart,
                                                                    @Param("requestedEnd") Instant requestedEnd);

    List<Order> searchForCrm(@Param("customerId") Integer customerId,
                             @Param("vehicleId") Integer vehicleId,
                             @Param("status") OrderStatus status,
                             @Param("employeeId") Integer employeeId,
                             @Param("plannedFrom") Instant plannedFrom,
                             @Param("plannedTo") Instant plannedTo);

    List<Order> searchForCrmByQuery(@Param("customerId") Integer customerId,
                                    @Param("vehicleId") Integer vehicleId,
                                    @Param("status") OrderStatus status,
                                    @Param("employeeId") Integer employeeId,
                                    @Param("plannedFrom") Instant plannedFrom,
                                    @Param("plannedTo") Instant plannedTo,
                                    @Param("q") String q);

    @EntityGraph(attributePaths = "vehicle")
    Optional<Order> findWithVehicleById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Integer id);
}
