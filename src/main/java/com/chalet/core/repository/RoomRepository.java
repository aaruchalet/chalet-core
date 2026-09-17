package com.chalet.core.repository;

import com.chalet.core.entity.DbRoom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<DbRoom, Long> {

  boolean existsByRoomNumber(String roomNumber);

  @Query(value = """
           SELECT r.*
             FROM room r
               LEFT JOIN booking b ON b.room_id = r.id\s
                   AND b.check_in_date < :checkOut
                   AND b.check_out_date > :checkIn
                   AND (b.booking_status = 'CONFIRMED'
                        OR (b.booking_status = 'HELD' AND b.hold_expiry > :currentTime))
               WHERE r.room_type_id = :roomTypeId
                   AND r.room_status = 'AVAILABLE'
                   AND b.id IS NULL
               ORDER BY r.id
               LIMIT 1
               FOR UPDATE
          \s""", nativeQuery = true)
  Optional<DbRoom> findAvailableRoomForBooking(
          @Param("roomTypeId") Long roomTypeId,
          @Param("checkIn") LocalDate checkIn,
          @Param("checkOut") LocalDate checkOut,
          @Param("currentTime") LocalDateTime currentTime);
}
