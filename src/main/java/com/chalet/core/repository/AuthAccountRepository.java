package com.chalet.core.repository;

import com.chalet.core.entity.DbAuthAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthAccountRepository extends JpaRepository<DbAuthAccount, Long> {

  Optional<DbAuthAccount> findByEmailIgnoreCase(String email);

  Optional<DbAuthAccount> findByPhone(String phone);

  Optional<DbAuthAccount> findByGoogleSubject(String googleSubject);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select account from auth_account account where account.id = :id")
  Optional<DbAuthAccount> findByIdForUpdate(@Param("id") Long id);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByPhone(String phone);
}
