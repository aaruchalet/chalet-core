package com.chalet.core.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Data;

@Data
public class BookingRequest {
  @NotNull
  private Long customerId;

  @NotNull
  private Long roomTypeId;

  @Pattern(regexp = "^[0-9]{10}$")
  private String phoneNumber;

  private RoomTypeRequest roomTypeRequest;

  @NotNull
  @FutureOrPresent(message = "Check-in date must be today or in the future")
  private LocalDate checkInDate;

  @NotNull
  @FutureOrPresent(message = "Check-out date must be today or in the future")
  private LocalDate checkOutDate;

  @AssertTrue(message = "Check-out date must be after check-in date")
  public boolean isDateRangeValid() {
    if (checkInDate == null || checkOutDate == null) {
      return true;
    }
    return checkOutDate.isAfter(checkInDate);
  }
}
