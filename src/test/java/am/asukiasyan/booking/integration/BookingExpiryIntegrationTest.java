package am.asukiasyan.booking.integration;

import am.asukiasyan.booking.domain.Booking;
import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.domain.User;
import am.asukiasyan.booking.enums.BookingStatus;
import am.asukiasyan.booking.repository.BookingRepository;
import am.asukiasyan.booking.repository.UnitEventRepository;
import am.asukiasyan.booking.repository.UnitRepository;
import am.asukiasyan.booking.repository.UserRepository;
import am.asukiasyan.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BookingExpiryIntegrationTest extends TestContainersConfig {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnitEventRepository unitEventRepository;

    @Autowired
    private BookingService bookingService;

    @Test
    void cancelsExpiredPendingBookingsViaDbCronFunction() {
        var unit = unitRepository.findById(1L).orElseThrow();
        var user = userRepository.findById(1L).orElseThrow();

        var saved = bookingRepository.save(expiredPendingBooking(unit, user));

        var cancelled = bookingService.cancelExpiredBookings();

        var cancelledBooking = bookingRepository.findById(saved.getId()).orElseThrow();
        assertThat(cancelled).isGreaterThanOrEqualTo(1);
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelledBooking.getExpiresAt()).isNull();
        assertThat(unitEventRepository.findAll())
                .anyMatch(event -> event.getUnit().getId().equals(cancelledBooking.getUnit().getId())
                        && "Cancelled by TTL".equals(event.getDetails()));
    }

    private Booking expiredPendingBooking(Unit unit, User user) {
        return Booking.builder()
                .unit(unit)
                .user(user)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .status(BookingStatus.PENDING_PAYMENT)
                .totalCost(new BigDecimal("100.00"))
                .expiresAt(Instant.now().minusSeconds(10))
                .build();
    }
}
