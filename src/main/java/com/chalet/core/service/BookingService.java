package com.chalet.core.service;

import com.chalet.core.dto.request.BookingRequest;
import com.chalet.core.dto.response.BookingResponse;
import java.util.List;

public interface BookingService {

  BookingResponse createBooking(BookingRequest request);

  BookingResponse confirmBooking(Long bookingId);

  void cancelBooking(Long bookingId);

  BookingResponse getBookingById(Long id);

  List<BookingResponse> findBookingsByCustomerId(Long customerId);

  List<BookingResponse> findBookingsByRoomId(Long roomId);
}
