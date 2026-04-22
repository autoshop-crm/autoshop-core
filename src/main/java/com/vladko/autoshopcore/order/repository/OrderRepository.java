package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends BaseRepository<Order, Integer> {

    List<Order> findAllByCustomerIdOrderByIdDesc(Integer customerId);

    List<Order> findAllByVehicleIdOrderByIdDesc(Integer vehicleId);

    List<Order> findAllByStatusOrderByIdDesc(OrderStatus status);

    List<Order> findAllByStatusInOrderByIdDesc(Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = "vehicle")
    Optional<Order> findWithVehicleById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Integer id);
}
