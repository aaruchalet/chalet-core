package com.chalet.core.repository;

import com.chalet.core.entity.DbCustomer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<DbCustomer, Long> {
  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);

  Optional<DbCustomer> findByEmail(String email);

  Optional<DbCustomer> findByPhone(String phone);
}
