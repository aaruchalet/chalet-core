package com.chalet.core.dto.response;

import com.chalet.core.enums.BookingStatus;
import com.chalet.core.enums.MealPlan;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long customerId,
        Long roomId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int adults,
        Integer childAge,
        MealPlan mealPlan,
        int rewardPointsRedeemed,
        BookingStatus bookingStatus
) {
}
