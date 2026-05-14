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

    List<Order> findAllByStatusOrderByIdDesc(OrderStatus status);

    List<Order> findAllByStatusInOrderByIdDesc(Collection<OrderStatus> statuses);

    List<Order> findAllByPlannedVisitAtBetweenOrderByPlannedVisitAtAscIdAsc(Instant from, Instant to);

    List<Order> findAllByPlannedVisitAtBetweenAndEmployeeIsNullOrderByPlannedVisitAtAscIdAsc(Instant from, Instant to);

    @Query("select o from CustomerOrder o where o.plannedVisitAt between :from and :to and o.status in :statuses order by o.plannedVisitAt asc, o.id asc")
    List<Order> findBookingsByWindowAndStatuses(@Param("from") Instant from,
                                                @Param("to") Instant to,
                                                @Param("statuses") Collection<OrderStatus> statuses);

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
