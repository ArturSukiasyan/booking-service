package am.asukiasyan.booking.dto;

import am.asukiasyan.booking.enums.BookingStatus;
import am.asukiasyan.booking.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long unitId,
        Long userId,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalCost
) {
}
