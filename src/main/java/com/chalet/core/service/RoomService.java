package com.chalet.core.service;

import com.chalet.core.dto.request.RoomRequest;
import com.chalet.core.dto.response.RoomAvailabilityResponse;
import com.chalet.core.dto.response.RoomResponse;
import java.time.LocalDate;
import java.util.List;

public interface RoomService {

  List<RoomResponse> findAll();

  List<RoomAvailabilityResponse> findAvailability(LocalDate checkIn, LocalDate checkOut);

  RoomResponse findById(Long id);

  RoomResponse create(RoomRequest request);

  RoomResponse update(Long id, RoomRequest request);

  void delete(Long id);
}
