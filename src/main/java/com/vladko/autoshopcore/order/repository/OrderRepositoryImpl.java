package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.order.entity.Order;
import com.vladko.autoshopcore.order.entity.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Order> searchForCrm(Integer customerId,
                                    Integer vehicleId,
                                    OrderStatus status,
                                    Integer employeeId,
                                    Instant plannedFrom,
                                    Instant plannedTo) {
        return search(customerId, vehicleId, status, employeeId, plannedFrom, plannedTo, null);
    }

    @Override
    public List<Order> searchForCrmByQuery(Integer customerId,
                                           Integer vehicleId,
                                           OrderStatus status,
                                           Integer employeeId,
                                           Instant plannedFrom,
                                           Instant plannedTo,
                                           String q) {
        return search(customerId, vehicleId, status, employeeId, plannedFrom, plannedTo, q);
    }

    private List<Order> search(Integer customerId,
                               Integer vehicleId,
                               OrderStatus status,
                               Integer employeeId,
                               Instant plannedFrom,
                               Instant plannedTo,
                               String q) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> order = cq.from(Order.class);

        List<Predicate> predicates = new ArrayList<>();
        if (customerId != null) {
            predicates.add(cb.equal(order.get("customer").get("id"), customerId));
        }
        if (vehicleId != null) {
            predicates.add(cb.equal(order.get("vehicle").get("id"), vehicleId));
        }
        if (status != null) {
            predicates.add(cb.equal(order.get("status"), status));
        }
        if (employeeId != null) {
            predicates.add(cb.equal(order.get("employee").get("id"), employeeId));
        }
        if (plannedFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(order.get("plannedVisitAt"), plannedFrom));
        }
        if (plannedTo != null) {
            predicates.add(cb.lessThanOrEqualTo(order.get("plannedVisitAt"), plannedTo));
        }
        if (q != null && !q.isBlank()) {
            predicates.add(cb.like(cb.lower(order.get("problem")), "%" + q.toLowerCase() + "%"));
        }

        cq.select(order)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(cb.desc(order.get("createdAt")), cb.desc(order.get("id")));

        TypedQuery<Order> query = entityManager.createQuery(cq);
        return query.getResultList();
    }
}
