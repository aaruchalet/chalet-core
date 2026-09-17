package com.chalet.core.mapper;

import com.chalet.core.dto.response.RoomResponse;
import com.chalet.core.entity.DbRoom;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {

  @Mapping(target = "roomTypeId", source = "roomType.id")
  RoomResponse toDto(DbRoom room);

  List<RoomResponse> toDto(List<DbRoom> rooms);
}
