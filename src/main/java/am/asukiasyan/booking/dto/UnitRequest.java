package am.asukiasyan.booking.dto;

import am.asukiasyan.booking.enums.UnitType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UnitRequest(
        @Positive int rooms,
        @NotNull UnitType type,
        @PositiveOrZero int floor,
        @NotBlank String description,
        @NotNull @Positive BigDecimal baseCost
) {
}
