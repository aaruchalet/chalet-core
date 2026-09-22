package com.chalet.core.service;

import com.chalet.core.dto.request.OtpRequest;
import com.chalet.core.dto.request.OtpVerifyRequest;
import com.chalet.core.dto.request.SignInRequest;
import com.chalet.core.dto.request.SignUpRequest;
import com.chalet.core.dto.response.AuthResponse;
import com.chalet.core.dto.response.OtpChallengeResponse;
import com.chalet.core.entity.DbAuthAccount;
import com.chalet.core.entity.DbAuthOtp;
import com.chalet.core.entity.DbCustomer;
import com.chalet.core.exception.AuthenticationFailedException;
import com.chalet.core.exception.DuplicateResourceException;
import com.chalet.core.repository.AuthAccountRepository;
import com.chalet.core.repository.AuthOtpRepository;
import com.chalet.core.repository.CustomerRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private static final int MAX_OTP_ATTEMPTS = 5;
  private static final int WELCOME_REWARD_POINTS = 500;

  private final AuthAccountRepository authAccountRepository;
  private final AuthOtpRepository authOtpRepository;
  private final CustomerRepository customerRepository;
  private final PasswordEncoder passwordEncoder;

  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${chalet.auth.otp-expiry:5m}")
  private Duration otpExpiry;

  @Value("${chalet.auth.expose-dev-otp:false}")
  private boolean exposeDevOtp;

  @Transactional
  public AuthResponse signUp(SignUpRequest request) {
    String email = normalizeEmail(request.email());
    String phone = request.phone().trim();

    if (authAccountRepository.existsByEmailIgnoreCase(email)) {
      throw new DuplicateResourceException("An account already exists with this email.");
    }
    if (authAccountRepository.existsByPhone(phone)) {
      throw new DuplicateResourceException("An account already exists with this phone number.");
    }

    DbCustomer customer = resolveOrCreateCustomer(
            request.name().trim(), email, phone, request.location().trim());

    DbAuthAccount account = new DbAuthAccount();
    account.setCustomerId(customer.getId());
    account.setName(request.name().trim());
    account.setEmail(email);
    account.setPhone(phone);
    account.setLocation(request.location().trim());
    account.setPasswordHash(passwordEncoder.encode(request.password()));
    account.setAuthProvider("LOCAL");
    account.setEnabled(true);
    account.setRewardPoints(WELCOME_REWARD_POINTS);

    return toResponse(authAccountRepository.save(account));
  }

  @Transactional(readOnly = true)
  public AuthResponse signIn(SignInRequest request) {
    DbAuthAccount account = findEnabledAccount(request.identifier());
    if (account.getPasswordHash() == null
            || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
      throw new AuthenticationFailedException("Invalid email/phone or password.");
    }
    return toResponse(account);
  }

  @Transactional
  public OtpChallengeResponse requestOtp(OtpRequest request) {
    DbAuthAccount account = findExistingMemberForOtp(request.identifier());
    String identifier = canonicalIdentifier(account, request.identifier());
    String code = String.format("%06d", secureRandom.nextInt(1_000_000));

    DbAuthOtp otp = new DbAuthOtp();
    otp.setIdentifier(identifier);
    otp.setCodeHash(passwordEncoder.encode(code));
    otp.setExpiresAt(LocalDateTime.now().plus(otpExpiry));
    otp.setAttempts(0);
    authOtpRepository.save(otp);

    if (exposeDevOtp) {
      log.info("Local development OTP for {} is {}", mask(identifier), code);
    }

    return new OtpChallengeResponse(
            mask(identifier),
            otpExpiry.toSeconds(),
            exposeDevOtp ? code : null
    );
  }

  @Transactional
  public AuthResponse verifyOtp(OtpVerifyRequest request) {
    DbAuthAccount account = findEnabledAccount(request.identifier());
    String identifier = canonicalIdentifier(account, request.identifier());

    DbAuthOtp otp = authOtpRepository
            .findTopByIdentifierAndConsumedAtIsNullOrderByCreatedAtDesc(identifier)
            .orElseThrow(() -> new AuthenticationFailedException(
                    "No active OTP found. Request a new OTP."));

    if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new AuthenticationFailedException("OTP has expired. Request a new OTP.");
    }
    if (otp.getAttempts() >= MAX_OTP_ATTEMPTS) {
      throw new AuthenticationFailedException("Too many OTP attempts. Request a new OTP.");
    }

    if (!passwordEncoder.matches(request.code(), otp.getCodeHash())) {
      otp.setAttempts(otp.getAttempts() + 1);
      authOtpRepository.save(otp);
      throw new AuthenticationFailedException("Invalid OTP.");
    }

    otp.setConsumedAt(LocalDateTime.now());
    authOtpRepository.save(otp);
    return toResponse(account);
  }

  @Transactional
  public AuthResponse signInWithGoogle(String email, String name, String googleSubject) {
    if (email == null || email.isBlank() || googleSubject == null || googleSubject.isBlank()) {
      throw new AuthenticationFailedException("Google did not return a usable account identity.");
    }

    String normalizedEmail = normalizeEmail(email);
    Optional<DbAuthAccount> byGoogle = authAccountRepository.findByGoogleSubject(googleSubject);
    Optional<DbAuthAccount> byEmail = authAccountRepository.findByEmailIgnoreCase(normalizedEmail);

    DbAuthAccount account = byGoogle.orElseGet(() -> byEmail.orElseGet(DbAuthAccount::new));

    if (account.getId() == null) {
      account.setEmail(normalizedEmail);
      account.setName(name == null || name.isBlank() ? normalizedEmail : name.trim());
      account.setAuthProvider("GOOGLE");
      account.setEnabled(true);
      customerRepository.findByEmail(normalizedEmail)
              .ifPresent(customer -> account.setCustomerId(customer.getId()));
    } else if (account.getName() == null || account.getName().isBlank()) {
      account.setName(name == null || name.isBlank() ? normalizedEmail : name.trim());
    }

    account.setGoogleSubject(googleSubject);
    return toResponse(authAccountRepository.save(account));
  }

  @Transactional
  public AuthResponse linkCustomer(Long accountId, Long customerId) {
    DbAuthAccount account = authAccountRepository.findById(accountId)
            .filter(DbAuthAccount::isEnabled)
            .orElseThrow(() -> new AuthenticationFailedException("Session is no longer valid."));

    DbCustomer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new AuthenticationFailedException("Guest profile was not found."));

    if (!account.getEmail().equalsIgnoreCase(customer.getEmail())) {
      throw new AuthenticationFailedException(
              "The guest profile email must match the signed-in member email.");
    }

    account.setCustomerId(customer.getId());
    if ((account.getLocation() == null || account.getLocation().isBlank())
            && customer.getAddress() != null) {
      account.setLocation(customer.getAddress());
    }
    return toResponse(authAccountRepository.save(account));
  }

  @Transactional(readOnly = true)
  public AuthResponse findById(Long accountId) {
    DbAuthAccount account = authAccountRepository.findById(accountId)
            .filter(DbAuthAccount::isEnabled)
            .orElseThrow(() -> new AuthenticationFailedException("Session is no longer valid."));
    return toResponse(account);
  }

  private DbAuthAccount findExistingMemberForOtp(String rawIdentifier) {
    String identifier = normalizeIdentifier(rawIdentifier);
    Optional<DbAuthAccount> account = identifier.contains("@")
            ? authAccountRepository.findByEmailIgnoreCase(identifier)
            : authAccountRepository.findByPhone(identifier);

    return account
            .filter(DbAuthAccount::isEnabled)
            .orElseThrow(() -> new AuthenticationFailedException(
                    "We couldn’t find an Aaru’s Chalet membership for that email or phone number. "
                            + "If you’re new here, please sign up first."));
  }

  private DbAuthAccount findEnabledAccount(String rawIdentifier) {
    String identifier = normalizeIdentifier(rawIdentifier);
    Optional<DbAuthAccount> account = identifier.contains("@")
            ? authAccountRepository.findByEmailIgnoreCase(identifier)
            : authAccountRepository.findByPhone(identifier);

    return account
            .filter(DbAuthAccount::isEnabled)
            .orElseThrow(() -> new AuthenticationFailedException(
                    "No account found for that email or phone number."));
  }

  private DbCustomer resolveOrCreateCustomer(
          String name, String email, String phone, String location) {
    Optional<DbCustomer> byEmail = customerRepository.findByEmail(email);
    Optional<DbCustomer> byPhone = customerRepository.findByPhone(phone);

    if (byEmail.isPresent() && byPhone.isPresent()
            && !byEmail.get().getId().equals(byPhone.get().getId())) {
      throw new DuplicateResourceException(
              "The email and phone number belong to different guest profiles.");
    }

    DbCustomer customer = byEmail.orElseGet(() -> byPhone.orElseGet(DbCustomer::new));
    customer.setName(name);
    customer.setEmail(email);
    customer.setPhone(phone);
    customer.setAddress(location);
    customer.setMember(true);
    return customerRepository.save(customer);
  }

  private String canonicalIdentifier(DbAuthAccount account, String requestedIdentifier) {
    String identifier = normalizeIdentifier(requestedIdentifier);
    return identifier.contains("@") ? normalizeEmail(account.getEmail()) : account.getPhone();
  }

  private String normalizeIdentifier(String identifier) {
    if (identifier == null) {
      return "";
    }
    String value = identifier.trim();
    return value.contains("@") ? normalizeEmail(value) : value.replaceAll("\\s+", "");
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String mask(String identifier) {
    if (identifier.contains("@")) {
      int at = identifier.indexOf('@');
      String local = identifier.substring(0, at);
      String domain = identifier.substring(at);
      String visible = local.substring(0, Math.min(2, local.length()));
      return visible + "***" + domain;
    }
    if (identifier.length() <= 4) {
      return "****";
    }
    return "******" + identifier.substring(identifier.length() - 4);
  }

  private AuthResponse toResponse(DbAuthAccount account) {
    return new AuthResponse(
            account.getId(),
            account.getCustomerId(),
            account.getName(),
            account.getEmail(),
            account.getPhone(),
            account.getLocation(),
            account.getAuthProvider(),
            account.getRewardPoints()
    );
  }
}
