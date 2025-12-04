package am.asukiasyan.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

public record BookingRequest(
        @NotNull @Positive Long unitId,
        @NotNull @Positive Long userId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
    @AssertTrue(message = "endDate must be on or after startDate")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
