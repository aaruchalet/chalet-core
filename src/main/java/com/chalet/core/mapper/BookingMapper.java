package com.chalet.core.mapper;

import com.chalet.core.dto.request.BookingRequest;
import com.chalet.core.dto.response.BookingResponse;
import com.chalet.core.entity.DbBooking;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

  @Mapping(target = "customerId", source = "customer.id")
  @Mapping(target = "roomId", source = "room.id")
  BookingResponse toDto(DbBooking booking);

  List<BookingResponse> toDto(List<DbBooking> bookings);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "customer", ignore = true)
  @Mapping(target = "room", ignore = true)
  @Mapping(target = "bookingStatus", ignore = true)
  @Mapping(target = "holdExpiry", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "rewardPointsRedeemed", source = "rewardPointsToRedeem")
  DbBooking toEntity(BookingRequest request);

  List<DbBooking> toEntity(List<BookingRequest> bookings);
}
