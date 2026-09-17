package com.chalet.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.chalet.core.dto.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void duplicateResourceUsesConflictEnvelope() {
    ResponseEntity<ApiResponse<Void>> response =
            handler.handleDuplicate(new DuplicateResourceException("duplicate"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertFailure(response.getBody(), "duplicate");
  }

  @Test
  void missingResourceUsesNotFoundEnvelope() {
    ResponseEntity<ApiResponse<Void>> response =
            handler.handleResourceNotFound(new ResourceNotFoundException("missing"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertFailure(response.getBody(), "missing");
  }

  @Test
  void unavailableRoomUsesConflictEnvelope() {
    ResponseEntity<ApiResponse<Void>> response =
            handler.handleRoomNotAvailable(new RoomAlreadyBookedException("unavailable"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertFailure(response.getBody(), "unavailable");
  }

  @Test
  void invalidBookingStateUsesConflictEnvelope() {
    ResponseEntity<ApiResponse<Void>> response =
            handler.handleIllegalState(new IllegalStateException("invalid state"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertFailure(response.getBody(), "invalid state");
  }

  @Test
  void optimisticLockFailureUsesConflictEnvelope() {
    ResponseEntity<ApiResponse<Void>> response = handler.handleOptimisticLock(
            new ObjectOptimisticLockingFailureException(Object.class, 1L));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertFailure(response.getBody(),
            "Booking was modified by another request. Refresh and retry.");
  }

  private void assertFailure(ApiResponse<Void> body, String error) {
    assertThat(body).isNotNull();
    assertThat(body.success()).isFalse();
    assertThat(body.data()).isNull();
    assertThat(body.error()).isEqualTo(error);
  }
}
