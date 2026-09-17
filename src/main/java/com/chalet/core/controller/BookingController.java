package com.chalet.core.controller;

import com.chalet.core.dto.request.BookingRequest;
import com.chalet.core.dto.response.BookingResponse;
import com.chalet.core.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  @GetMapping("/{id}")
  public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.getBookingById(id));
  }

  @GetMapping("/customer/{customerId}")
  public ResponseEntity<List<BookingResponse>> getBookingsByCustomer(
          @PathVariable Long customerId) {
    return ResponseEntity.ok(bookingService.findBookingsByCustomerId(customerId));
  }

  @GetMapping("/room/{roomId}")
  public ResponseEntity<List<BookingResponse>> getBookingsByRoom(@PathVariable Long roomId) {
    return ResponseEntity.ok(bookingService.findBookingsByRoomId(roomId));
  }

  @PostMapping
  public ResponseEntity<BookingResponse> createBooking(
          @Valid @RequestBody BookingRequest bookingRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(bookingService.createBooking(bookingRequest));
  }

  @PostMapping("/{id}/confirm")
  public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.confirmBooking(id));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
    bookingService.cancelBooking(id);
    return ResponseEntity.noContent().build();
  }
}
