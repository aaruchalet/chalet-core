package com.chalet.core.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class BookingRequest {
  @NotNull
  private Long customerId;

  @NotNull
  private Long roomTypeId;

  @NotNull
  @FutureOrPresent(message = "Check-in date must be today or in the future")
  private LocalDate checkInDate;

  @NotNull
  @FutureOrPresent(message = "Check-out date must be today or in the future")
  private LocalDate checkOutDate;

  @Min(value = 1, message = "At least one adult is required per room")
  @Max(value = 3, message = "A room can have at most 3 adults")
  private int adults = 2;

  @Min(value = 0, message = "Child age cannot be negative")
  @Max(value = 17, message = "Child age must be 17 or below")
  private Integer childAge;

  @AssertTrue(message = "Room occupancy is limited to 3 people: up to 3 adults, or 2 adults plus 1 child")
  public boolean isOccupancyValid() {
    if (childAge == null) {
      return adults >= 1 && adults <= 3;
    }
    int effectiveAdults = adults + (childAge >= 12 ? 1 : 0);
    int youngChildren = childAge < 12 ? 1 : 0;
    return effectiveAdults <= 3 && effectiveAdults + youngChildren <= 3 && (! (childAge < 12) || adults <= 2);
  }

  @AssertTrue(message = "Check-out date must be after check-in date")
  public boolean isDateRangeValid() {
    if (checkInDate == null || checkOutDate == null) {
      return true;
    }
    return checkOutDate.isAfter(checkInDate);
  }
}
