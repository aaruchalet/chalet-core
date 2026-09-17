package com.chalet.core.service.impl;

import static com.chalet.core.util.Constants.ROOM_NOT_FOUND;
import static com.chalet.core.util.Constants.ROOM_NUMBER_ALREADY_EXISTS;
import static com.chalet.core.util.Constants.ROOM_TYPE_NOT_FOUND;

import com.chalet.core.dto.request.RoomRequest;
import com.chalet.core.dto.response.RoomResponse;
import com.chalet.core.entity.DbRoom;
import com.chalet.core.entity.DbRoomType;
import com.chalet.core.exception.DuplicateResourceException;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.mapper.RoomMapper;
import com.chalet.core.repository.RoomRepository;
import com.chalet.core.repository.RoomTypeRepository;
import com.chalet.core.service.RoomService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

  private final RoomRepository roomRepository;
  private final RoomTypeRepository roomTypeRepository;
  private final RoomMapper roomMapper;

  @Override
  @Transactional(readOnly = true)
  public List<RoomResponse> findAll() {
    return roomMapper.toDto(roomRepository.findAll());
  }

  @Override
  @Transactional(readOnly = true)
  public RoomResponse findById(Long id) {
    return roomMapper.toDto(findRoom(id));
  }

  @Override
  @Transactional
  public RoomResponse create(RoomRequest request) {
    if (roomRepository.existsByRoomNumber(request.roomNumber())) {
      throw new DuplicateResourceException(ROOM_NUMBER_ALREADY_EXISTS);
    }

    DbRoom room = new DbRoom();
    applyRequest(room, request);
    return roomMapper.toDto(roomRepository.save(room));
  }

  @Override
  @Transactional
  public RoomResponse update(Long id, RoomRequest request) {
    DbRoom room = findRoom(id);

    if (!room.getRoomNumber().equals(request.roomNumber())
            && roomRepository.existsByRoomNumber(request.roomNumber())) {
      throw new DuplicateResourceException(ROOM_NUMBER_ALREADY_EXISTS);
    }

    applyRequest(room, request);
    return roomMapper.toDto(roomRepository.save(room));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    roomRepository.delete(findRoom(id));
  }

  private DbRoom findRoom(Long id) {
    return roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ROOM_NOT_FOUND.formatted(id)));
  }

  private void applyRequest(DbRoom room, RoomRequest request) {
    DbRoomType roomType = roomTypeRepository.findById(request.roomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    ROOM_TYPE_NOT_FOUND.formatted(request.roomTypeId())));

    room.setRoomNumber(request.roomNumber());
    room.setRoomType(roomType);
    room.setRoomStatus(request.roomStatus());
  }
}
