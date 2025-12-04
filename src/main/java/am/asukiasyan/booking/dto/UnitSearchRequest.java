package am.asukiasyan.booking.dto;

import am.asukiasyan.booking.enums.UnitType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UnitSearchRequest(
        @Positive Integer rooms,
        UnitType type,
        Integer floor,
        @Positive BigDecimal minCost,
        @Positive BigDecimal maxCost,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @PositiveOrZero Integer page,
        @Positive Integer size,
        String sortBy,
        Sort.Direction direction
) {
    public UnitSearchRequest {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 10;
        }
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "id";
        }
        if (direction == null) {
            direction = Sort.Direction.ASC;
        }
    }

    @AssertTrue(message = "maxCost must be greater than or equal to minCost")
    public boolean isCostRangeValid() {
        if (minCost == null || maxCost == null) {
            return true;
        }
        return maxCost.compareTo(minCost) >= 0;
    }

    @AssertTrue(message = "endDate must be on or after startDate")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
