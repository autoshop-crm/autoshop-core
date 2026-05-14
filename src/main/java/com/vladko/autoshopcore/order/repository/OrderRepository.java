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

public interface OrderRepository extends BaseRepository<Order, Integer> {

    List<Order> findAllByCustomerIdOrderByIdDesc(Integer customerId);

    List<Order> findAllByVehicleIdOrderByIdDesc(Integer vehicleId);

    List<Order> findAllByStatusOrderByIdDesc(OrderStatus status);

    List<Order> findAllByStatusInOrderByIdDesc(Collection<OrderStatus> statuses);

    List<Order> findAllByPlannedVisitAtBetweenOrderByPlannedVisitAtAscIdAsc(Instant from, Instant to);

    List<Order> findAllByPlannedVisitAtBetweenAndEmployeeIsNullOrderByPlannedVisitAtAscIdAsc(Instant from, Instant to);

    @Query("select o from CustomerOrder o where o.plannedVisitAt between :from and :to and o.status in :statuses order by o.plannedVisitAt asc, o.id asc")
    List<Order> findBookingsByWindowAndStatuses(@Param("from") Instant from,
                                                @Param("to") Instant to,
                                                @Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            select o from CustomerOrder o
            where (:customerId is null or o.customer.id = :customerId)
              and (:vehicleId is null or o.vehicle.id = :vehicleId)
              and (:status is null or o.status = :status)
              and (:employeeId is null or o.employee.id = :employeeId)
              and (:plannedFrom is null or o.plannedVisitAt >= :plannedFrom)
              and (:plannedTo is null or o.plannedVisitAt <= :plannedTo)
              and (:q is null or lower(o.problem) like concat('%', :q, '%'))
            order by o.createdAt desc, o.id desc
            """)
    List<Order> searchForCrm(@Param("customerId") Integer customerId,
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
