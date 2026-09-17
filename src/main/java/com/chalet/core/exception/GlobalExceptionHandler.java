package com.chalet.core.exception;

import com.chalet.core.dto.common.ApiResponse;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<?> handleDuplicate(DuplicateResourceException exception) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(exception.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<?> handleResourceNotFond(ResourceNotFoundException exception) {
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(exception.getMessage());
  }

  @ExceptionHandler(RoomAlreadyBookedException.class)
  public ResponseEntity<?> handleRoomNotAvailable(RoomAlreadyBookedException exception) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(exception.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<?> handleIllegalState(IllegalStateException exception) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
          MethodArgumentNotValidException ex) {

    Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                    FieldError::getField,
                    error -> Optional.ofNullable(error.getDefaultMessage())
                            .orElse("Validation failed"),
                    (first, second) -> first
            ));

    return ResponseEntity.badRequest()
            .body(ApiResponse.failure(errors));
  }
}
