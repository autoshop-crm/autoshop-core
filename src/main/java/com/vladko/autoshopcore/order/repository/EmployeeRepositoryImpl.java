package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Repository
public class EmployeeRepositoryImpl implements EmployeeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Employee> searchAvailabilityCandidates(String query, Collection<EmployeeType> roles, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
        Root<Employee> employee = cq.from(Employee.class);

        List<Predicate> predicates = new ArrayList<>();
        if (roles != null && !roles.isEmpty()) {
            predicates.add(employee.get("function").in(roles));
        }
        if (query != null && !query.isBlank()) {
            String normalized = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(employee.get("firstName")), normalized),
                    cb.like(cb.lower(employee.get("lastName")), normalized),
                    cb.like(cb.lower(employee.get("email")), normalized)
            ));
        }

        List<Order> ordering = List.of(
                cb.asc(employee.get("function")),
                cb.asc(employee.get("lastName")),
                cb.asc(employee.get("firstName")),
                cb.asc(employee.get("id"))
        );
        cq.select(employee).where(predicates.toArray(Predicate[]::new)).orderBy(ordering);
        TypedQuery<Employee> typedQuery = entityManager.createQuery(cq);
        typedQuery.setMaxResults(limit);
        return typedQuery.getResultList();
    }
}
