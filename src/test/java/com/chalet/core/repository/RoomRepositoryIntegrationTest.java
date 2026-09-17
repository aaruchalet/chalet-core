package com.chalet.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chalet.core.entity.DbBooking;
import com.chalet.core.entity.DbCustomer;
import com.chalet.core.entity.DbRoom;
import com.chalet.core.entity.DbRoomType;
import com.chalet.core.enums.BookingStatus;
import com.chalet.core.enums.RoomStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoomRepositoryIntegrationTest {

  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("chalet_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private RoomTypeRepository roomTypeRepository;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private CustomerRepository customerRepository;

  private DbRoomType roomType;
  private DbRoom firstRoom;
  private DbRoom secondRoom;
  private DbCustomer customer;

  @BeforeEach
  void setUp() {
    bookingRepository.deleteAll();
    roomRepository.deleteAll();
    roomTypeRepository.deleteAll();
    customerRepository.deleteAll();

    roomType = new DbRoomType();
    roomType.setTypeName("DELUXE");
    roomType.setPricePerNight(new BigDecimal("2500.00"));
    roomType = roomTypeRepository.saveAndFlush(roomType);

    firstRoom = room("101");
    secondRoom = room("102");
    firstRoom = roomRepository.saveAndFlush(firstRoom);
    secondRoom = roomRepository.saveAndFlush(secondRoom);

    customer = new DbCustomer();
    customer.setName("Test Customer");
    customer.setEmail("guest@example.com");
    customer.setPhone("9999999999");
    customer.setAddress("Test Address");
    customer.setMember(false);
    customer = customerRepository.saveAndFlush(customer);
  }

  @Test
  void returnsFirstAvailableRoomWhenNoBookingOverlaps() {
    DbRoom available = roomRepository.findAvailableRoomForBooking(
                    roomType.getId(),
                    LocalDate.of(2026, 10, 10),
                    LocalDate.of(2026, 10, 12))
            .orElseThrow();

    assertThat(available.getId()).isEqualTo(firstRoom.getId());
  }

  @Test
  void skipsRoomWithOverlappingConfirmedBooking() {
    saveBooking(
            firstRoom,
            LocalDate.of(2026, 10, 10),
            LocalDate.of(2026, 10, 12),
            BookingStatus.CONFIRMED,
            null);

    DbRoom available = roomRepository.findAvailableRoomForBooking(
                    roomType.getId(),
                    LocalDate.of(2026, 10, 11),
                    LocalDate.of(2026, 10, 13))
            .orElseThrow();

    assertThat(available.getId()).isEqualTo(secondRoom.getId());
  }

  @Test
  void skipsRoomWithUnexpiredHeldBooking() {
    saveBooking(
            firstRoom,
            LocalDate.of(2026, 10, 10),
            LocalDate.of(2026, 10, 12),
            BookingStatus.HELD,
            LocalDateTime.now().plusMinutes(5));

    DbRoom available = roomRepository.findAvailableRoomForBooking(
                    roomType.getId(),
                    LocalDate.of(2026, 10, 10),
                    LocalDate.of(2026, 10, 12))
            .orElseThrow();

    assertThat(available.getId()).isEqualTo(secondRoom.getId());
  }

  @Test
  void expiredHoldDoesNotBlockRoom() {
    saveBooking(
            firstRoom,
            LocalDate.of(2026, 10, 10),
            LocalDate.of(2026, 10, 12),
            BookingStatus.HELD,
            LocalDateTime.now().minusMinutes(1));

    DbRoom available = roomRepository.findAvailableRoomForBooking(
                    roomType.getId(),
                    LocalDate.of(2026, 10, 10),
                    LocalDate.of(2026, 10, 12))
            .orElseThrow();

    assertThat(available.getId()).isEqualTo(firstRoom.getId());
  }

  @Test
  void adjacentBookingDoesNotCountAsOverlap() {
    saveBooking(
            firstRoom,
            LocalDate.of(2026, 10, 10),
            LocalDate.of(2026, 10, 12),
            BookingStatus.CONFIRMED,
            null);

    DbRoom available = roomRepository.findAvailableRoomForBooking(
                    roomType.getId(),
                    LocalDate.of(2026, 10, 12),
                    LocalDate.of(2026, 10, 14))
            .orElseThrow();

    assertThat(available.getId()).isEqualTo(firstRoom.getId());
  }

  private DbRoom room(String number) {
    DbRoom room = new DbRoom();
    room.setRoomNumber(number);
    room.setRoomType(roomType);
    room.setRoomStatus(RoomStatus.AVAILABLE);
    return room;
  }

  private void saveBooking(
          DbRoom room,
          LocalDate checkIn,
          LocalDate checkOut,
          BookingStatus status,
          LocalDateTime holdExpiry) {
    DbBooking booking = new DbBooking();
    booking.setCustomer(customer);
    booking.setRoom(room);
    booking.setCheckInDate(checkIn);
    booking.setCheckOutDate(checkOut);
    booking.setBookingStatus(status);
    booking.setHoldExpiry(holdExpiry);
    bookingRepository.saveAndFlush(booking);
  }
}
