package am.asukiasyan.booking.schedule;

import am.asukiasyan.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    /**
     *  Runs each minute to clear expired bookings
     */
    @Scheduled(cron = "0 * * * * *")
    public void cancelExpired() {
        int cancelled = bookingService.cancelExpiredBookings();
        if (cancelled > 0) {
            log.info("Scheduler cancelled {} expired bookings", cancelled);
        }
    }

    private final BookingService bookingService;
}
