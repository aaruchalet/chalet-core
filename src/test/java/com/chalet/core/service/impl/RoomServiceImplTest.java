package com.chalet.core.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chalet.core.dto.request.RoomRequest;
import com.chalet.core.dto.response.RoomAvailabilityResponse;
import com.chalet.core.dto.response.RoomResponse;
import com.chalet.core.entity.DbRoom;
import com.chalet.core.entity.DbRoomType;
import com.chalet.core.enums.RoomStatus;
import com.chalet.core.exception.DuplicateResourceException;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.mapper.RoomMapper;
import com.chalet.core.repository.RoomRepository;
import com.chalet.core.repository.RoomTypeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private RoomTypeRepository roomTypeRepository;

  @Mock
  private RoomMapper roomMapper;

  private final Clock clock = Clock.fixed(
          Instant.parse("2026-09-23T06:00:00Z"),
          ZoneOffset.UTC);

  private RoomServiceImpl roomService;

  @BeforeEach
  void setUp() {
    roomService = new RoomServiceImpl(roomRepository, roomTypeRepository, roomMapper, clock);
  }

  @Test
  void createPersistsRoomWithRequestedTypeAndStatus() {
    RoomRequest request = new RoomRequest("101", 7L, RoomStatus.AVAILABLE);
    DbRoomType roomType = new DbRoomType();
    roomType.setId(7L);
    RoomResponse response = new RoomResponse(1L, "101", 7L, RoomStatus.AVAILABLE);

    when(roomRepository.existsByRoomNumber("101")).thenReturn(false);
    when(roomTypeRepository.findById(7L)).thenReturn(Optional.of(roomType));
    when(roomRepository.save(any(DbRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(roomMapper.toDto(any(DbRoom.class))).thenReturn(response);

    RoomResponse actual = roomService.create(request);

    assertThat(actual).isEqualTo(response);
    verify(roomRepository).save(any(DbRoom.class));
  }

  @Test
  void createRejectsDuplicateRoomNumber() {
    RoomRequest request = new RoomRequest("101", 7L, RoomStatus.AVAILABLE);
    when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

    assertThatThrownBy(() -> roomService.create(request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessage("Room number already exists.");
  }

  @Test
  void createRejectsUnknownRoomType() {
    RoomRequest request = new RoomRequest("101", 7L, RoomStatus.AVAILABLE);
    when(roomRepository.existsByRoomNumber("101")).thenReturn(false);
    when(roomTypeRepository.findById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Room Type with id 7 not found.");
  }

  @Test
  void updateAllowsKeepingExistingRoomNumber() {
    DbRoom existing = new DbRoom();
    existing.setId(1L);
    existing.setRoomNumber("101");
    existing.setRoomStatus(RoomStatus.CLEANING);

    DbRoomType roomType = new DbRoomType();
    roomType.setId(7L);

    RoomRequest request = new RoomRequest("101", 7L, RoomStatus.AVAILABLE);
    RoomResponse response = new RoomResponse(1L, "101", 7L, RoomStatus.AVAILABLE);

    when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(roomTypeRepository.findById(7L)).thenReturn(Optional.of(roomType));
    when(roomRepository.save(existing)).thenReturn(existing);
    when(roomMapper.toDto(existing)).thenReturn(response);

    RoomResponse actual = roomService.update(1L, request);

    assertThat(actual).isEqualTo(response);
    assertThat(existing.getRoomStatus()).isEqualTo(RoomStatus.AVAILABLE);
    assertThat(existing.getRoomType()).isEqualTo(roomType);
  }

  @Test
  void findAvailabilityReturnsAvailableCountForEachRoomType() {
    DbRoomType pine = new DbRoomType();
    pine.setId(1L);
    pine.setTypeName("Pine Haven");
    pine.setPricePerNight(new BigDecimal("2000.00"));

    DbRoomType cedar = new DbRoomType();
    cedar.setId(2L);
    cedar.setTypeName("Cedar Retreat");
    cedar.setPricePerNight(new BigDecimal("2500.00"));

    LocalDate checkIn = LocalDate.of(2026, 10, 10);
    LocalDate checkOut = LocalDate.of(2026, 10, 12);

    when(roomTypeRepository.findAll()).thenReturn(List.of(pine, cedar));
    when(roomRepository.countAvailableRoomsForBooking(
            any(Long.class), any(LocalDate.class), any(LocalDate.class), any()))
            .thenReturn(2L, 0L);

    List<RoomAvailabilityResponse> availability =
            roomService.findAvailability(checkIn, checkOut);

    assertThat(availability).containsExactly(
            new RoomAvailabilityResponse(1L, 2L),
            new RoomAvailabilityResponse(2L, 0L));
  }

  @Test
  void findByIdRejectsUnknownRoom() {
    when(roomRepository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.findById(404L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Room with id 404 not found.");
  }
}
