package com.chalet.core.repository;

import com.chalet.core.entity.DbAuthAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccountRepository extends JpaRepository<DbAuthAccount, Long> {

  Optional<DbAuthAccount> findByEmailIgnoreCase(String email);

  Optional<DbAuthAccount> findByPhone(String phone);

  Optional<DbAuthAccount> findByGoogleSubject(String googleSubject);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByPhone(String phone);
}
