package com.chalet.core.service;

import com.chalet.core.dto.request.CustomerRequest;
import com.chalet.core.dto.response.CustomerResponse;
import java.util.List;

public interface CustomerService {
  List<CustomerResponse> findAll();

  CustomerResponse findById(Long id);

  CustomerResponse findByPhone(String phone);

  CustomerResponse findByEmail(String email);

  CustomerResponse create(CustomerRequest request);

  CustomerResponse update(Long id, CustomerRequest request);

  void delete(Long id);
}
