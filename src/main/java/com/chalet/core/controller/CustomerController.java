package com.chalet.core.controller;

import com.chalet.core.dto.common.ApiResponse;
import com.chalet.core.dto.request.CustomerRequest;
import com.chalet.core.dto.response.CustomerResponse;
import com.chalet.core.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @GetMapping
  public ResponseEntity<ApiResponse<?>> findAll() {
    List<CustomerResponse> customers = customerService.findAll();
    if (customers.isEmpty()) {
      return ResponseEntity.ok(ApiResponse.success("No Customers Present In Database"));
    }
    return ResponseEntity.ok(ApiResponse.success(customers));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CustomerResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(customerService.findById(id)));
  }

  @GetMapping("/by-email")
  public ResponseEntity<ApiResponse<CustomerResponse>> findByEmail(@RequestParam String email) {
    return ResponseEntity.ok(ApiResponse.success(customerService.findByEmail(email)));
  }

  @GetMapping("/by-phone")
  public ResponseEntity<ApiResponse<CustomerResponse>> findByPhone(@RequestParam String phone) {
    return ResponseEntity.ok(ApiResponse.success(customerService.findByPhone(phone)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
          @Valid @RequestBody CustomerRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(customerService.create(request)));
  }
}
