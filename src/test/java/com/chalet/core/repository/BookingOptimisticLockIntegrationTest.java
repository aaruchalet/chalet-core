package com.chalet.core.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chalet.core.entity.DbBooking;
import com.chalet.core.entity.DbCustomer;
import com.chalet.core.entity.DbRoom;
import com.chalet.core.entity.DbRoomType;
import com.chalet.core.enums.BookingStatus;
import com.chalet.core.enums.RoomStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingOptimisticLockIntegrationTest {

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
  private BookingRepository bookingRepository;

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private RoomTypeRepository roomTypeRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void staleBookingUpdateFailsWithOptimisticLockConflict() {
    DbCustomer customer = new DbCustomer();
    customer.setName("Concurrent Guest");
    customer.setEmail("concurrent@example.com");
    customer.setPhone("8888888888");
    customer.setMember(false);
    customer = customerRepository.saveAndFlush(customer);

    DbRoomType roomType = new DbRoomType();
    roomType.setTypeName("LOCK_TEST");
    roomType.setPricePerNight(new BigDecimal("1000.00"));
    roomType = roomTypeRepository.saveAndFlush(roomType);

    DbRoom room = new DbRoom();
    room.setRoomNumber("LOCK-101");
    room.setRoomType(roomType);
    room.setRoomStatus(RoomStatus.AVAILABLE);
    room = roomRepository.saveAndFlush(room);

    DbBooking booking = new DbBooking();
    booking.setCustomer(customer);
    booking.setRoom(room);
    booking.setCheckInDate(LocalDate.of(2026, 11, 1));
    booking.setCheckOutDate(LocalDate.of(2026, 11, 3));
    booking.setBookingStatus(BookingStatus.HELD);
    booking = bookingRepository.saveAndFlush(booking);

    Long bookingId = booking.getId();
    entityManager.clear();

    DbBooking staleBooking = bookingRepository.findById(bookingId).orElseThrow();

    jdbcTemplate.update(
            "UPDATE booking SET version = version + 1 WHERE id = ?",
            bookingId);

    staleBooking.setBookingStatus(BookingStatus.CANCELLED);

    assertThatThrownBy(bookingRepository::flush)
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
  }
}
