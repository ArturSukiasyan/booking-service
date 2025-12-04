package am.asukiasyan.booking.schedule;

import am.asukiasyan.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingExpirySchedulerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingExpiryScheduler scheduler;

    @Test
    void cancelExpiredInvokesService() {
        when(bookingService.cancelExpiredBookings()).thenReturn(3);

        scheduler.cancelExpired();

        verify(bookingService, times(1)).cancelExpiredBookings();
    }
}
