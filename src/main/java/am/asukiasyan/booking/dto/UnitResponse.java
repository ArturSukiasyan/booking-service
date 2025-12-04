package am.asukiasyan.booking.dto;

import am.asukiasyan.booking.enums.UnitType;

import java.math.BigDecimal;
import java.time.Instant;

public record UnitResponse(
        Long id,
        int rooms,
        UnitType type,
        int floor,
        String description,
        BigDecimal baseCost,
        BigDecimal finalCost,
        Instant createdAt
) {
}
