package com.chalet.core.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.chalet.core.dto.response.CustomerResponse;
import com.chalet.core.entity.DbCustomer;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.mapper.CustomerMapper;
import com.chalet.core.repository.CustomerRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

  @Mock
  private CustomerRepository customerRepository;

  @Mock
  private CustomerMapper customerMapper;

  private CustomerServiceImpl customerService;

  @BeforeEach
  void setUp() {
    customerService = new CustomerServiceImpl(customerRepository, customerMapper);
  }

  @Test
  void findByEmailReturnsMappedCustomer() {
    DbCustomer customer = new DbCustomer();
    customer.setId(1L);
    customer.setEmail("guest@example.com");
    CustomerResponse response = new CustomerResponse(
            1L, "Guest", "guest@example.com", "9999999999", null, false);

    when(customerRepository.findByEmail("guest@example.com")).thenReturn(Optional.of(customer));
    when(customerMapper.toDto(customer)).thenReturn(response);

    assertThat(customerService.findByEmail("guest@example.com")).isEqualTo(response);
  }

  @Test
  void findByPhoneReturnsMappedCustomer() {
    DbCustomer customer = new DbCustomer();
    customer.setId(1L);
    customer.setPhone("9999999999");
    CustomerResponse response = new CustomerResponse(
            1L, "Guest", "guest@example.com", "9999999999", null, false);

    when(customerRepository.findByPhone("9999999999")).thenReturn(Optional.of(customer));
    when(customerMapper.toDto(customer)).thenReturn(response);

    assertThat(customerService.findByPhone("9999999999")).isEqualTo(response);
  }

  @Test
  void findByEmailThrowsWhenCustomerDoesNotExist() {
    when(customerRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> customerService.findByEmail("missing@example.com"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Customer with email missing@example.com not found.");
  }

  @Test
  void findByPhoneThrowsWhenCustomerDoesNotExist() {
    when(customerRepository.findByPhone("0000000000")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> customerService.findByPhone("0000000000"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Customer with phone 0000000000 not found.");
  }
}
