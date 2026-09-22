package com.chalet.core.entity;

import com.chalet.core.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "booking",
        indexes = {

                @Index(
                        name = "idx_booking_customer",
                        columnList = "customer_id"
                ),

                @Index(
                        name = "idx_booking_room",
                        columnList = "room_id"
                ),

                @Index(
                        name = "idx_booking_customer_checkin",
                        columnList = "customer_id, check_in_date"
                ),

                @Index(
                        name = "idx_booking_room_checkin_checkout",
                        columnList = "room_id, check_in_date, check_out_date"
                ),

                @Index(
                        name = "idx_booking_hold",
                        columnList = "booking_status, hold_expiry"
                )
        }
)
public class DbBooking {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private DbCustomer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private DbRoom room;

  @Column(name = "check_in_date", nullable = false)
  private LocalDate checkInDate;

  @Column(name = "check_out_date", nullable = false)
  private LocalDate checkOutDate;

  @Column(name = "adults", nullable = false)
  private int adults = 2;

  @Column(name = "child_age")
  private Integer childAge;

  @Enumerated(EnumType.STRING)
  @Column(name = "booking_status", nullable = false)
  private BookingStatus bookingStatus;

  @Column(name = "hold_expiry")
  private LocalDateTime holdExpiry;

  @Version
  @Column(nullable = false)
  private Long version;
}
