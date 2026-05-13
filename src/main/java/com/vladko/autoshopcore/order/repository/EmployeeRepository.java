package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends BaseRepository<Employee, Integer> {

    List<Employee> findAllByOrderByIdAsc();

    Optional<Employee> findByEmail(String email);

    @Query(value = """
            SELECT *
            FROM employee
            WHERE email IS NOT NULL
              AND lower(email) LIKE concat(:emailPrefix, '%')
            ORDER BY CASE WHEN lower(email) = :emailPrefix THEN 0 ELSE 1 END,
                     id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Employee> searchByEmailPrefix(@Param("emailPrefix") String emailPrefix, @Param("limit") int limit);
}
