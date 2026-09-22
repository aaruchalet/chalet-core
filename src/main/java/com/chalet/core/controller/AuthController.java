package com.chalet.core.controller;

import com.chalet.core.dto.common.ApiResponse;
import com.chalet.core.dto.request.OtpRequest;
import com.chalet.core.dto.request.OtpVerifyRequest;
import com.chalet.core.dto.request.SignInRequest;
import com.chalet.core.dto.request.SignUpRequest;
import com.chalet.core.dto.response.AuthResponse;
import com.chalet.core.dto.response.OtpChallengeResponse;
import com.chalet.core.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  public static final String AUTH_SESSION_KEY = "AARUS_CHALET_AUTH_ACCOUNT_ID";

  private final AuthService authService;

  @Value("${spring.security.oauth2.client.registration.google.client-id:}")
  private String googleClientId;

  @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
  private String googleClientSecret;

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<AuthResponse>> signUp(
          @Valid @RequestBody SignUpRequest request,
          HttpSession session) {
    AuthResponse account = authService.signUp(request);
    authenticate(session, account);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Account created successfully.", account));
  }

  @PostMapping("/signin")
  public ResponseEntity<ApiResponse<AuthResponse>> signIn(
          @Valid @RequestBody SignInRequest request,
          HttpSession session) {
    AuthResponse account = authService.signIn(request);
    authenticate(session, account);
    return ResponseEntity.ok(ApiResponse.success("Signed in successfully.", account));
  }

  @PostMapping("/otp/request")
  public ResponseEntity<ApiResponse<OtpChallengeResponse>> requestOtp(
          @Valid @RequestBody OtpRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
            "OTP created. In local development the code is returned for testing.",
            authService.requestOtp(request)));
  }

  @PostMapping("/otp/verify")
  public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
          @Valid @RequestBody OtpVerifyRequest request,
          HttpSession session) {
    AuthResponse account = authService.verifyOtp(request);
    authenticate(session, account);
    return ResponseEntity.ok(ApiResponse.success("OTP verified. Signed in successfully.", account));
  }

  @PostMapping("/customer/{customerId}")
  public ResponseEntity<ApiResponse<AuthResponse>> linkCustomer(
          @PathVariable Long customerId,
          HttpSession session) {
    Object accountId = session.getAttribute(AUTH_SESSION_KEY);
    if (!(accountId instanceof Long id)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(new ApiResponse<>(false, null, null, "Sign in before linking a guest profile."));
    }
    AuthResponse account = authService.linkCustomer(id, customerId);
    return ResponseEntity.ok(ApiResponse.success("Guest profile linked.", account));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<AuthResponse>> me(HttpSession session) {
    Object accountId = session.getAttribute(AUTH_SESSION_KEY);
    if (!(accountId instanceof Long id)) {
      return ResponseEntity.ok(ApiResponse.success("Not signed in."));
    }
    return ResponseEntity.ok(ApiResponse.success(authService.findById(id)));
  }

  @GetMapping("/config")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> config() {
    boolean googleEnabled = !googleClientId.isBlank() && !googleClientSecret.isBlank();
    return ResponseEntity.ok(ApiResponse.success(Map.of("googleEnabled", googleEnabled)));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
    session.invalidate();
    return ResponseEntity.ok(ApiResponse.success("Signed out successfully."));
  }

  private void authenticate(HttpSession session, AuthResponse account) {
    session.setAttribute(AUTH_SESSION_KEY, account.accountId());
  }
}
