package com.chalet.core.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chalet.core.config.BookingProperties;
import com.chalet.core.dto.request.BookingRequest;
import com.chalet.core.dto.response.BookingResponse;
import com.chalet.core.entity.DbAuthAccount;
import com.chalet.core.entity.DbBooking;
import com.chalet.core.entity.DbCustomer;
import com.chalet.core.entity.DbRoom;
import com.chalet.core.enums.BookingStatus;
import com.chalet.core.enums.MealPlan;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.exception.RoomAlreadyBookedException;
import com.chalet.core.mapper.BookingMapper;
import com.chalet.core.repository.AuthAccountRepository;
import com.chalet.core.repository.BookingRepository;
import com.chalet.core.repository.CustomerRepository;
import com.chalet.core.repository.RoomRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

  private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
  private static final LocalDateTime CURRENT_TIME = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

  @Mock
  private BookingRepository bookingRepository;

  @Mock
  private BookingMapper bookingMapper;

  @Mock
  private AuthAccountRepository authAccountRepository;

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private CustomerRepository customerRepository;

  private BookingServiceImpl bookingService;

  @BeforeEach
  void setUp() {
    BookingProperties bookingProperties = new BookingProperties();
    bookingProperties.setHoldDuration(Duration.ofMinutes(10));

    bookingService = new BookingServiceImpl(
            bookingRepository,
            bookingMapper,
            authAccountRepository,
            roomRepository,
            customerRepository,
            bookingProperties,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createBookingUsesSameClockTimeForAvailabilityAndHoldExpiry() {
    BookingRequest request = new BookingRequest();
    request.setCustomerId(10L);
    request.setRoomTypeId(20L);
    request.setCheckInDate(LocalDate.of(2026, 10, 1));
    request.setCheckOutDate(LocalDate.of(2026, 10, 2));

    DbCustomer customer = new DbCustomer();
    customer.setId(10L);

    DbRoom room = new DbRoom();
    room.setId(30L);

    DbBooking booking = new DbBooking();
    BookingResponse response = response(40L, BookingStatus.HELD);

    when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
    when(roomRepository.findAvailableRoomForBooking(
            20L,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            CURRENT_TIME)).thenReturn(Optional.of(room));
    when(bookingMapper.toEntity(request)).thenReturn(booking);
    when(bookingRepository.save(booking)).thenReturn(booking);
    when(bookingMapper.toDto(booking)).thenReturn(response);

    BookingResponse result = bookingService.createBooking(request, null);

    assertThat(result).isEqualTo(response);
    assertThat(booking.getCustomer()).isSameAs(customer);
    assertThat(booking.getRoom()).isSameAs(room);
    assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.HELD);
    assertThat(booking.getHoldExpiry()).isEqualTo(CURRENT_TIME.plusMinutes(10));
    verify(roomRepository).findAvailableRoomForBooking(
            20L,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            CURRENT_TIME);
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
            CURRENT_TIME.plusMinutes(5));
    BookingResponse response = response(2L, BookingStatus.CONFIRMED);

    when(bookingRepository.findById(2L)).thenReturn(Optional.of(booking));
    when(bookingMapper.toDto(booking)).thenReturn(response);

    BookingResponse result = bookingService.confirmBooking(2L, null);

    assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    assertThat(booking.getHoldExpiry()).isNull();
    assertThat(result).isEqualTo(response);
    verify(bookingMapper).toDto(booking);
  }

  @Test
  void confirmBookingRedeemsMemberPoints() {
    DbCustomer customer = new DbCustomer();
    customer.setId(10L);

    DbBooking booking = booking(
            7L,
            BookingStatus.HELD,
            CURRENT_TIME.plusMinutes(5));
    booking.setCustomer(customer);
    booking.setRewardPointsRedeemed(300);

    DbAuthAccount account = new DbAuthAccount();
    account.setId(77L);
    account.setCustomerId(10L);
    account.setEnabled(true);
    account.setRewardPoints(500);

    BookingResponse response = response(7L, BookingStatus.CONFIRMED, 300);

    when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
    when(authAccountRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(account));
    when(bookingMapper.toDto(booking)).thenReturn(response);

    BookingResponse result = bookingService.confirmBooking(7L, 77L);

    assertThat(account.getRewardPoints()).isEqualTo(200);
    assertThat(result.rewardPointsRedeemed()).isEqualTo(300);
    verify(authAccountRepository).save(account);
  }

  @Test
  void confirmBookingRejectsExpiredHoldAtDeterministicClockTime() {
    DbBooking booking = booking(
            3L,
            BookingStatus.HELD,
            CURRENT_TIME.minusSeconds(1));
    when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.confirmBooking(3L, null))
            .isInstanceOf(RoomAlreadyBookedException.class)
            .hasMessage("Booking hold expired.");
  }

  @Test
  void confirmBookingRejectsHoldExpiringExactlyNow() {
    DbBooking booking = booking(6L, BookingStatus.HELD, CURRENT_TIME);
    when(bookingRepository.findById(6L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.confirmBooking(6L, null))
            .isInstanceOf(RoomAlreadyBookedException.class)
            .hasMessage("Booking hold expired.");
  }

  @Test
  void confirmBookingRejectsNonHeldBooking() {
    DbBooking booking = booking(4L, BookingStatus.CONFIRMED, null);
    when(bookingRepository.findById(4L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.confirmBooking(4L, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Booking is not on hold.");
  }

  @Test
  void cancelBookingTransitionsBookingToCancelled() {
    DbBooking booking = booking(
            5L,
            BookingStatus.HELD,
            CURRENT_TIME.plusMinutes(5));
    when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

    bookingService.cancelBooking(5L);

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
    return response(id, status, 0);
  }

  private BookingResponse response(Long id, BookingStatus status, int rewardPointsRedeemed) {
    return new BookingResponse(
            id,
            10L,
            20L,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            2,
            null,
            MealPlan.ROOM_ONLY,
            rewardPointsRedeemed,
            status);
  }
}
