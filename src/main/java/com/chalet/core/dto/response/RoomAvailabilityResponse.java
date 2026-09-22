package com.chalet.core.dto.response;

public record RoomAvailabilityResponse(
        Long roomTypeId,
        long availableRooms
) {
}
