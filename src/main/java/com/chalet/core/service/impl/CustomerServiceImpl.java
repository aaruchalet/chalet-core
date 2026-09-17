package com.chalet.core.service.impl;

import static com.chalet.core.util.Constants.CUSTOMER_EMAIL_ALREADY_EXISTS;
import static com.chalet.core.util.Constants.CUSTOMER_NOT_FOUND;
import static com.chalet.core.util.Constants.CUSTOMER_PHONE_NUMBER_ALREADY_EXISTS;

import com.chalet.core.dto.request.CustomerRequest;
import com.chalet.core.dto.response.CustomerResponse;
import com.chalet.core.exception.DuplicateResourceException;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.mapper.CustomerMapper;
import com.chalet.core.repository.CustomerRepository;
import com.chalet.core.service.CustomerService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CustomerServiceImpl implements CustomerService {

  private final CustomerRepository customerRepository;
  private final CustomerMapper customerMapper;

  @Override
  public List<CustomerResponse> findAll() {
    return customerMapper.toDto(customerRepository.findAll());
  }

  @Override
  public CustomerResponse findById(Long id) {
    return customerMapper.toDto(customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND.formatted(id))));
  }

  @Override
  @Transactional
  public CustomerResponse create(CustomerRequest request) {
    if (customerRepository.existsByEmail(request.getEmail())) {
      throw new DuplicateResourceException(CUSTOMER_EMAIL_ALREADY_EXISTS);
    }

    if (customerRepository.existsByPhone(request.getPhone())) {
      throw new DuplicateResourceException(CUSTOMER_PHONE_NUMBER_ALREADY_EXISTS);
    }

    return customerMapper.toDto(customerRepository.save(customerMapper.toEntity(request)));
  }

  @Override
  public CustomerResponse findByPhone(String phone) {
    return customerMapper.toDto(customerRepository.findByPhone(phone)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Customer with phone %s not found.".formatted(phone))));
  }

  @Override
  public CustomerResponse findByEmail(String email) {
    return customerMapper.toDto(customerRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Customer with email %s not found.".formatted(email))));
  }
}
