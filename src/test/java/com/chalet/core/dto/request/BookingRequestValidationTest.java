package com.chalet.core.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BookingRequestValidationTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidatorFactory() {
    validatorFactory.close();
  }

  @Test
  void acceptsFutureDateRange() {
    BookingRequest request = validRequest();

    Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  void rejectsCheckoutOnSameDayAsCheckin() {
    BookingRequest request = validRequest();
    request.setCheckOutDate(request.getCheckInDate());

    Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

    assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Check-out date must be after check-in date");
  }

  @Test
  void rejectsCheckoutBeforeCheckin() {
    BookingRequest request = validRequest();
    request.setCheckOutDate(request.getCheckInDate().minusDays(1));

    Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

    assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Check-out date must be after check-in date");
  }

  @Test
  void rejectsPastCheckinDate() {
    BookingRequest request = validRequest();
    request.setCheckInDate(LocalDate.now().minusDays(1));

    Set<ConstraintViolation<BookingRequest>> violations = validator.validate(request);

    assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Check-in date must be today or in the future");
  }

  private BookingRequest validRequest() {
    BookingRequest request = new BookingRequest();
    request.setCustomerId(1L);
    request.setRoomTypeId(1L);
    request.setCheckInDate(LocalDate.now().plusDays(1));
    request.setCheckOutDate(LocalDate.now().plusDays(2));
    return request;
  }
}
