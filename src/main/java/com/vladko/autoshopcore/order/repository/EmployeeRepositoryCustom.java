package com.vladko.autoshopcore.order.repository;

import com.vladko.autoshopcore.entities.Employee;
import com.vladko.autoshopcore.entities.EmployeeType;

import java.util.Collection;
import java.util.List;

public interface EmployeeRepositoryCustom {
    List<Employee> searchAvailabilityCandidates(String query, Collection<EmployeeType> roles, int limit);
}
