package com.chalet.core.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chalet.core.dto.response.BookingResponse;
import com.chalet.core.entity.DbBooking;
import com.chalet.core.enums.BookingStatus;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.mapper.BookingMapper;
import com.chalet.core.repository.BookingRepository;
import com.chalet.core.repository.CustomerRepository;
import com.chalet.core.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

  @Mock
  private BookingRepository bookingRepository;

  @Mock
  private BookingMapper bookingMapper;

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private CustomerRepository customerRepository;

  private BookingServiceImpl bookingService;

  @BeforeEach
  void setUp() {
    bookingService = new BookingServiceImpl(
            bookingRepository,
            bookingMapper,
            roomRepository,
            customerRepository);
  }

  @Test
  void getBookingByIdReturnsMappedBooking() {
    DbBooking booking = booking(1L, BookingStatus.CONFIRMED, null);
    BookingResponse response = response(1L, BookingStatus.CONFIRMED);

    when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
    when(bookingMapper.toDto(booking)).thenReturn(response);

    assertThat(bookingService.getBookingById(1L)).isEqualTo(response);
  }

  @Test
  void getBookingByIdThrowsWhenMissing() {
    when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.getBookingById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Booking with id 99 not found.");
  }

  @Test
  void confirmBookingTransitionsHeldBookingToConfirmed() {
    DbBooking booking = booking(
            2L,
            BookingStatus.HELD,
            LocalDateTime.now().plusMinutes(5));
    BookingResponse response = response(2L, BookingStatus.CONFIRMED);

    when(bookingRepository.findById(2L)).thenReturn(Optional.of(booking));
    when(bookingMapper.toDto(booking)).thenReturn(response);

    BookingResponse result = bookingService.confirmBooking(2L);

    assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    assertThat(booking.getHoldExpiry()).isNull();
    assertThat(result).isEqualTo(response);
    verify(bookingMapper).toDto(booking);
  }

  @Test
  void confirmBookingRejectsNonHeldBooking() {
    DbBooking booking = booking(3L, BookingStatus.CONFIRMED, null);
    when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.confirmBooking(3L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Booking is not on hold.");
  }

  @Test
  void cancelBookingTransitionsBookingToCancelled() {
    DbBooking booking = booking(
            4L,
            BookingStatus.HELD,
            LocalDateTime.now().plusMinutes(5));
    when(bookingRepository.findById(4L)).thenReturn(Optional.of(booking));

    bookingService.cancelBooking(4L);

    assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
    assertThat(booking.getHoldExpiry()).isNull();
  }

  private DbBooking booking(Long id, BookingStatus status, LocalDateTime holdExpiry) {
    DbBooking booking = new DbBooking();
    booking.setId(id);
    booking.setBookingStatus(status);
    booking.setHoldExpiry(holdExpiry);
    booking.setCheckInDate(LocalDate.of(2026, 10, 1));
    booking.setCheckOutDate(LocalDate.of(2026, 10, 2));
    return booking;
  }

  private BookingResponse response(Long id, BookingStatus status) {
    return new BookingResponse(
            id,
            10L,
            20L,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            status);
  }
}
