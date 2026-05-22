package com.vladko.autoshopcore.client.repository;

import com.vladko.autoshopcore.client.entity.Customer;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends BaseRepository<Customer, Integer> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    @Query(value = """
            SELECT *
            FROM customer
            WHERE lower(email) LIKE concat(:emailPrefix, '%')
            ORDER BY CASE WHEN lower(email) = :emailPrefix THEN 0 ELSE 1 END,
                     updated_at DESC NULLS LAST,
                     id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Customer> searchByEmailPrefix(@Param("emailPrefix") String emailPrefix, @Param("limit") int limit);

    @Query(value = """
            SELECT *
            FROM customer
            WHERE regexp_replace(phone_number, '[^0-9]', '', 'g') LIKE concat(:phoneDigitsPrefix, '%')
            ORDER BY CASE
                         WHEN regexp_replace(phone_number, '[^0-9]', '', 'g') = :phoneDigitsPrefix THEN 0
                         ELSE 1
                     END,
                     updated_at DESC NULLS LAST,
                     id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Customer> searchByPhoneDigitsPrefix(@Param("phoneDigitsPrefix") String phoneDigitsPrefix,
                                             @Param("limit") int limit);

    List<Customer> findByFirstNameContainingIgnoreCase(String firstName);

    List<Customer> findByLastNameContainingIgnoreCase(String lastName);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
