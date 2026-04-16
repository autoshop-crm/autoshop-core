package com.vladko.autoshopcore.client.service;

import com.vladko.autoshopcore.client.dto.CustomerCreateDTO;
import com.vladko.autoshopcore.client.dto.CustomerResponseDTO;
import com.vladko.autoshopcore.client.dto.CustomerUpdateDTO;

import java.util.List;

public interface CustomerService {

    CustomerResponseDTO create(CustomerCreateDTO dto);

    CustomerResponseDTO getById(Integer id);

    CustomerResponseDTO update(Integer id, CustomerUpdateDTO dto);

    void delete(Integer id);

    List<CustomerResponseDTO> search(String email, String phoneNumber, String firstName, String lastName);
}
