package com.chalet.core.service.impl;

import static com.chalet.core.util.Constants.ROOM_TYPE_ALREADY_EXISTS;
import static com.chalet.core.util.Constants.ROOM_TYPE_NOT_FOUND;

import com.chalet.core.dto.request.RoomTypeRequest;
import com.chalet.core.dto.response.RoomTypeResponse;
import com.chalet.core.entity.DbRoomType;
import com.chalet.core.exception.DuplicateResourceException;
import com.chalet.core.exception.ResourceNotFoundException;
import com.chalet.core.mapper.RoomTypeMapper;
import com.chalet.core.repository.RoomTypeRepository;
import com.chalet.core.service.RoomTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

  private final RoomTypeRepository roomTypeRepository;
  private final RoomTypeMapper roomTypeMapper;

  @Override
  @Transactional(readOnly = true)
  public List<RoomTypeResponse> getAllRoomTypes() {
    return roomTypeMapper.toDto(roomTypeRepository.findAll());
  }

  @Override
  @Transactional
  public RoomTypeResponse updateRoomType(Long id, RoomTypeRequest roomTypeRequest) {
    DbRoomType existingRoomType = findRoomType(id);

    roomTypeRepository.findByTypeName(roomTypeRequest.getTypeName())
            .filter(roomType -> !roomType.getId().equals(id))
            .ifPresent(roomType -> {
              throw new DuplicateResourceException(ROOM_TYPE_ALREADY_EXISTS);
            });

    existingRoomType.setTypeName(roomTypeRequest.getTypeName());
    existingRoomType.setPricePerNight(roomTypeRequest.getPricePerNight());
    return roomTypeMapper.toDto(roomTypeRepository.save(existingRoomType));
  }

  @Override
  @Transactional(readOnly = true)
  public RoomTypeResponse getSingleRoomType(Long id) {
    return roomTypeMapper.toDto(findRoomType(id));
  }

  @Override
  @Transactional
  public RoomTypeResponse createRoomType(RoomTypeRequest roomTypeRequest) {
    if (roomTypeRepository.findByTypeName(roomTypeRequest.getTypeName()).isPresent()) {
      throw new DuplicateResourceException(ROOM_TYPE_ALREADY_EXISTS);
    }
    return roomTypeMapper.toDto(roomTypeRepository.save(roomTypeMapper.toEntity(roomTypeRequest)));
  }

  @Override
  @Transactional
  public void deleteRoomType(Long id) {
    roomTypeRepository.delete(findRoomType(id));
  }

  private DbRoomType findRoomType(Long id) {
    return roomTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ROOM_TYPE_NOT_FOUND.formatted(id)));
  }
}
