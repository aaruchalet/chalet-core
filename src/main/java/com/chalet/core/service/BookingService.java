package com.chalet.core.service;

import com.chalet.core.dto.request.BookingRequest;
import com.chalet.core.dto.response.BookingResponse;
import java.util.List;

public interface BookingService {

  BookingResponse createBooking(BookingRequest request, Long authenticatedAccountId);

  BookingResponse confirmBooking(Long bookingId, Long authenticatedAccountId);

  void cancelBooking(Long bookingId);

  BookingResponse getBookingById(Long id);

  List<BookingResponse> findBookingsByCustomerId(Long customerId);

  List<BookingResponse> findBookingsByRoomId(Long roomId);
}
