package com.chalet.core.repository;

import com.chalet.core.entity.DbAuthOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthOtpRepository extends JpaRepository<DbAuthOtp, Long> {

  Optional<DbAuthOtp> findTopByIdentifierAndConsumedAtIsNullOrderByCreatedAtDesc(String identifier);
}
