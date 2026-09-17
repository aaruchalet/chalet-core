package com.chalet.core.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseConstraintIntegrationTest {

  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("chalet_constraints_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void rejectsBookingWhenCheckoutIsNotAfterCheckin() {
    jdbcTemplate.update(
            "INSERT INTO customer (name, email, phone, member) VALUES (?, ?, ?, ?)",
            "Constraint Guest",
            "constraint-guest@example.com",
            "9000000001",
            false);

    Long customerId = jdbcTemplate.queryForObject(
            "SELECT id FROM customer WHERE email = ?",
            Long.class,
            "constraint-guest@example.com");
    Long roomId = jdbcTemplate.queryForObject(
            "SELECT id FROM room ORDER BY id LIMIT 1",
            Long.class);

    LocalDate date = LocalDate.of(2026, 10, 10);

    assertThatThrownBy(() -> jdbcTemplate.update(
            """
            INSERT INTO booking
              (customer_id, room_id, check_in_date, check_out_date, booking_status, version)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            customerId,
            roomId,
            date,
            date,
            "CONFIRMED",
            0L))
            .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsRoomTypeWithNonPositivePrice() {
    assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO room_type (type_name, price_per_night) VALUES (?, ?)",
            "FREE",
            0))
            .isInstanceOf(DataIntegrityViolationException.class);
  }
}
