package com.chalet.core.service.impl;

import static com.chalet.core.util.Constants.BOOKING_HOLD_EXPIRED;
import static com.chalet.core.util.Constants.BOOKING_NOT_FOUND;
import static com.chalet.core.util.Constants.BOOKING_NOT_ON_HOLD;
import static com.chalet.core.util.Constants.CUSTOMER_NOT_FOUND;

import com.chalet.core.config.BookingProperties;
import com.chalet.core.dto.request.BookingRequest;
import com.chalet.core.dto.response.BookingResponse;
import com.chalet.core.entity.DbAuthAccount;
import com.chalet.core.entity.DbBooking;
import com.chalet.core.entity.DbCustomer;
import com.chalet.core.entity.DbRoom;
import com.chalet.core.enums.BookingStatus;
import com.chalet.core.exception.AuthenticationFailedException;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.exception.RoomAlreadyBookedException;
import com.chalet.core.mapper.BookingMapper;
import com.chalet.core.repository.AuthAccountRepository;
import com.chalet.core.repository.BookingRepository;
import com.chalet.core.repository.CustomerRepository;
import com.chalet.core.repository.RoomRepository;
import com.chalet.core.service.BookingService;
import java.time.Clock;
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
  private final AuthAccountRepository authAccountRepository;
  private final RoomRepository roomRepository;
  private final CustomerRepository customerRepository;
  private final BookingProperties bookingProperties;
  private final Clock clock;

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
  public BookingResponse createBooking(BookingRequest request, Long authenticatedAccountId) {
    DbCustomer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    CUSTOMER_NOT_FOUND.formatted(request.getCustomerId())));

    validateRewardPointsRequest(request, authenticatedAccountId);

    LocalDateTime currentTime = now();
    DbRoom room = roomRepository.findAvailableRoomForBooking(
                    request.getRoomTypeId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate(),
                    currentTime)
            .orElseThrow(() -> new RoomAlreadyBookedException("No room available"));

    DbBooking booking = bookingMapper.toEntity(request);
    booking.setCustomer(customer);
    booking.setRoom(room);
    booking.setBookingStatus(BookingStatus.HELD);
    booking.setHoldExpiry(currentTime.plus(bookingProperties.getHoldDuration()));

    return bookingMapper.toDto(bookingRepository.save(booking));
  }

  @Override
  @Transactional
  public BookingResponse confirmBooking(Long bookingId, Long authenticatedAccountId) {
    DbBooking booking = findBooking(bookingId);

    if (booking.getBookingStatus() != BookingStatus.HELD) {
      throw new IllegalStateException(BOOKING_NOT_ON_HOLD);
    }

    LocalDateTime currentTime = now();
    if (booking.getHoldExpiry() == null || !booking.getHoldExpiry().isAfter(currentTime)) {
      throw new RoomAlreadyBookedException(BOOKING_HOLD_EXPIRED);
    }

    redeemRewardPoints(booking, authenticatedAccountId);

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

  private void validateRewardPointsRequest(
          BookingRequest request,
          Long authenticatedAccountId) {
    int requestedPoints = request.getRewardPointsToRedeem();
    if (requestedPoints <= 0) {
      return;
    }

    DbAuthAccount account = authenticatedAccount(authenticatedAccountId, false);
    if (account.getCustomerId() == null
            || !account.getCustomerId().equals(request.getCustomerId())) {
      throw new AuthenticationFailedException(
              "Member points can only be used for the signed-in member’s booking.");
    }
    if (account.getRewardPoints() < requestedPoints) {
      throw new IllegalStateException("Not enough member points are available.");
    }
  }

  private void redeemRewardPoints(DbBooking booking, Long authenticatedAccountId) {
    int points = booking.getRewardPointsRedeemed();
    if (points <= 0) {
      return;
    }

    DbAuthAccount account = authenticatedAccount(authenticatedAccountId, true);
    if (account.getCustomerId() == null
            || booking.getCustomer() == null
            || !account.getCustomerId().equals(booking.getCustomer().getId())) {
      throw new AuthenticationFailedException(
              "Sign in with the member account used for this booking to redeem points.");
    }
    if (account.getRewardPoints() < points) {
      throw new IllegalStateException("Not enough member points are available.");
    }

    account.setRewardPoints(account.getRewardPoints() - points);
    authAccountRepository.save(account);
  }

  private DbAuthAccount authenticatedAccount(Long accountId, boolean lockForUpdate) {
    if (accountId == null) {
      throw new AuthenticationFailedException("Sign in to use member points.");
    }
    return (lockForUpdate
            ? authAccountRepository.findByIdForUpdate(accountId)
            : authAccountRepository.findById(accountId))
            .filter(DbAuthAccount::isEnabled)
            .orElseThrow(() ->
                    new AuthenticationFailedException("Session is no longer valid."));
  }

  private DbBooking findBooking(Long bookingId) {
    return bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    BOOKING_NOT_FOUND.formatted(bookingId)));
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock);
  }
}
