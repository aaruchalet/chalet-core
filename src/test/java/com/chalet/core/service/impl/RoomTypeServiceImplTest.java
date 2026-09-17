package com.chalet.core.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chalet.core.dto.request.RoomTypeRequest;
import com.chalet.core.dto.response.RoomTypeResponse;
import com.chalet.core.entity.DbRoomType;
import com.chalet.core.exception.DuplicateResourceException;
import com.chalet.core.mapper.RoomTypeMapper;
import com.chalet.core.repository.RoomTypeRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceImplTest {

  @Mock
  private RoomTypeRepository roomTypeRepository;

  @Mock
  private RoomTypeMapper roomTypeMapper;

  private RoomTypeServiceImpl roomTypeService;

  @BeforeEach
  void setUp() {
    roomTypeService = new RoomTypeServiceImpl(roomTypeRepository, roomTypeMapper);
  }

  @Test
  void updateAllowsKeepingSameTypeName() {
    DbRoomType roomType = roomType(1L, "DELUXE", "2000.00");
    RoomTypeRequest request = new RoomTypeRequest(1L, "DELUXE", new BigDecimal("2500.00"));
    RoomTypeResponse response = new RoomTypeResponse(1L, "DELUXE", new BigDecimal("2500.00"));

    when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
    when(roomTypeRepository.findByTypeName("DELUXE")).thenReturn(Optional.of(roomType));
    when(roomTypeRepository.save(roomType)).thenReturn(roomType);
    when(roomTypeMapper.toDto(roomType)).thenReturn(response);

    RoomTypeResponse actual = roomTypeService.updateRoomType(1L, request);

    assertThat(actual).isEqualTo(response);
    assertThat(roomType.getPricePerNight()).isEqualByComparingTo("2500.00");
    verify(roomTypeRepository).save(roomType);
  }

  @Test
  void updateRejectsNameOwnedByAnotherRoomType() {
    DbRoomType roomType = roomType(1L, "DELUXE", "2000.00");
    DbRoomType existingNameOwner = roomType(2L, "SUITE", "3500.00");
    RoomTypeRequest request = new RoomTypeRequest(1L, "SUITE", new BigDecimal("2500.00"));

    when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
    when(roomTypeRepository.findByTypeName("SUITE")).thenReturn(Optional.of(existingNameOwner));

    assertThatThrownBy(() -> roomTypeService.updateRoomType(1L, request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessage("Room type already exists.");
  }

  private DbRoomType roomType(Long id, String name, String price) {
    DbRoomType roomType = new DbRoomType();
    roomType.setId(id);
    roomType.setTypeName(name);
    roomType.setPricePerNight(new BigDecimal(price));
    return roomType;
  }
}
