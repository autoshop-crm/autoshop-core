package com.vladko.autoshopcore.client.repository;

import com.vladko.autoshopcore.client.entity.Customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findById(Integer id);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    @Query(value = "SELECT u FROM Customer u WHERE LOWER(u.firstName) = LOWER(:name)")
    Optional<Customer> findByFirstName(@Param("name") String Firstname);

    @Query(value = "SELECT u FROM Customer u WHERE LOWER(u.lastName) = LOWER(:lastName)")
    Optional<Customer> findByLastName(@Param("lastName") String LastName);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

}