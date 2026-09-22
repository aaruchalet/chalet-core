package com.chalet.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chalet.core.dto.request.OtpRequest;
import com.chalet.core.dto.request.SignInRequest;
import com.chalet.core.dto.request.SignUpRequest;
import com.chalet.core.dto.response.AuthResponse;
import com.chalet.core.entity.DbAuthAccount;
import com.chalet.core.entity.DbCustomer;
import com.chalet.core.exception.AuthenticationFailedException;
import com.chalet.core.repository.AuthAccountRepository;
import com.chalet.core.repository.AuthOtpRepository;
import com.chalet.core.repository.CustomerRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthAccountRepository authAccountRepository;

  @Mock
  private AuthOtpRepository authOtpRepository;

  @Mock
  private CustomerRepository customerRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(
            authAccountRepository,
            authOtpRepository,
            customerRepository,
            passwordEncoder);
  }

  @Test
  void signupAwardsFiveHundredWelcomePoints() {
    SignUpRequest request = new SignUpRequest(
            "New Member",
            "member@example.com",
            "9876543210",
            "password123",
            "Ghaziabad, Uttar Pradesh");

    when(authAccountRepository.existsByEmailIgnoreCase("member@example.com")).thenReturn(false);
    when(authAccountRepository.existsByPhone("9876543210")).thenReturn(false);
    when(customerRepository.findByEmail("member@example.com")).thenReturn(Optional.empty());
    when(customerRepository.findByPhone("9876543210")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

    when(customerRepository.save(any(DbCustomer.class))).thenAnswer(invocation -> {
      DbCustomer customer = invocation.getArgument(0);
      customer.setId(41L);
      return customer;
    });

    when(authAccountRepository.save(any(DbAuthAccount.class))).thenAnswer(invocation -> {
      DbAuthAccount account = invocation.getArgument(0);
      account.setId(77L);
      return account;
    });

    AuthResponse response = authService.signUp(request);

    assertThat(response.accountId()).isEqualTo(77L);
    assertThat(response.customerId()).isEqualTo(41L);
    assertThat(response.rewardPoints()).isEqualTo(500);
  }

  @Test
  void otpRequestDirectsUnknownGuestToSignup() {
    when(authAccountRepository.findByEmailIgnoreCase("new@example.com"))
            .thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.requestOtp(new OtpRequest("new@example.com")))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessageContaining("couldn’t find an Aaru’s Chalet membership")
            .hasMessageContaining("sign up first");
  }

  @Test
  void passwordSigninDoesNotRevealWhetherAccountExists() {
    when(authAccountRepository.findByEmailIgnoreCase("missing@example.com"))
            .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
            authService.signIn(new SignInRequest("missing@example.com", "wrong-password")))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage("Invalid email/phone or password.");
  }
}
