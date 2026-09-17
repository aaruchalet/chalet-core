package com.chalet.core.dto.response;

import com.chalet.core.enums.RoomStatus;

public record RoomResponse(
        Long id,
        String roomNumber,
        Long roomTypeId,
        RoomStatus roomStatus
) {
}
