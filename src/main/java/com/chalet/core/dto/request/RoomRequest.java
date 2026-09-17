package com.chalet.core.dto.request;

import com.chalet.core.enums.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(
        @NotBlank String roomNumber,
        @NotNull Long roomTypeId,
        @NotNull RoomStatus roomStatus
) {
}
