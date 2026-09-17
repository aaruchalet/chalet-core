package com.chalet.core.service.impl;

import static com.chalet.core.util.Constants.BOOKING_HOLD_EXPIRED;
import static com.chalet.core.util.Constants.BOOKING_NOT_FOUND;
import static com.chalet.core.util.Constants.BOOKING_NOT_ON_HOLD;
import static com.chalet.core.util.Constants.CUSTOMER_NOT_FOUND;

import com.chalet.core.dto.request.BookingRequest;
import com.chalet.core.dto.response.BookingResponse;
import com.chalet.core.entity.DbBooking;
import com.chalet.core.entity.DbCustomer;
import com.chalet.core.entity.DbRoom;
import com.chalet.core.enums.BookingStatus;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.exception.RoomAlreadyBookedException;
import com.chalet.core.mapper.BookingMapper;
import com.chalet.core.repository.BookingRepository;
import com.chalet.core.repository.CustomerRepository;
import com.chalet.core.repository.RoomRepository;
import com.chalet.core.service.BookingService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

  private final BookingRepository bookingRepository;
  private final BookingMapper bookingMapper;
  private final RoomRepository roomRepository;
  private final CustomerRepository customerRepository;

  @Override
  public List<BookingResponse> findBookingsByCustomerId(Long customerId) {
    return bookingMapper.toDto(bookingRepository.findByCustomerId(customerId));
  }

  @Override
  public List<BookingResponse> findBookingsByRoomId(Long roomId) {
    return bookingMapper.toDto(bookingRepository.findByRoomId(roomId));
  }

  @Override
  @Transactional
  public BookingResponse createBooking(BookingRequest request) {
    DbCustomer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    CUSTOMER_NOT_FOUND.formatted(request.getCustomerId())));

    DbRoom room = roomRepository.findAvailableRoomForBooking(
                    request.getRoomTypeId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate())
            .orElseThrow(() -> new RoomAlreadyBookedException("No room available"));

    DbBooking booking = bookingMapper.toEntity(request);
    booking.setCustomer(customer);
    booking.setRoom(room);
    booking.setBookingStatus(BookingStatus.HELD);
    booking.setHoldExpiry(LocalDateTime.now().plusMinutes(10));

    return bookingMapper.toDto(bookingRepository.save(booking));
  }

  @Override
  @Transactional
  public BookingResponse confirmBooking(Long bookingId) {
    DbBooking booking = findBooking(bookingId);

    if (booking.getBookingStatus() != BookingStatus.HELD) {
      throw new IllegalStateException(BOOKING_NOT_ON_HOLD);
    }

    if (booking.getHoldExpiry() == null || booking.getHoldExpiry().isBefore(LocalDateTime.now())) {
      throw new RoomAlreadyBookedException(BOOKING_HOLD_EXPIRED);
    }

    booking.setBookingStatus(BookingStatus.CONFIRMED);
    booking.setHoldExpiry(null);

    return bookingMapper.toDto(booking);
  }

  @Override
  @Transactional
  public void cancelBooking(Long bookingId) {
    DbBooking booking = findBooking(bookingId);
    booking.setBookingStatus(BookingStatus.CANCELLED);
    booking.setHoldExpiry(null);
  }

  @Override
  @Transactional(readOnly = true)
  public BookingResponse getBookingById(Long id) {
    return bookingMapper.toDto(findBooking(id));
  }

  private DbBooking findBooking(Long bookingId) {
    return bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    BOOKING_NOT_FOUND.formatted(bookingId)));
  }
}
