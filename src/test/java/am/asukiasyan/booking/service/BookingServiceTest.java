package am.asukiasyan.booking.service;

import am.asukiasyan.booking.domain.Booking;
import am.asukiasyan.booking.domain.Payment;
import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.domain.User;
import am.asukiasyan.booking.dto.BookingRequest;
import am.asukiasyan.booking.enums.BookingStatus;
import am.asukiasyan.booking.enums.PaymentStatus;
import am.asukiasyan.booking.enums.UnitEventType;
import am.asukiasyan.booking.exception.ConflictException;
import am.asukiasyan.booking.exception.NotFoundException;
import am.asukiasyan.booking.repository.BookingRepository;
import am.asukiasyan.booking.repository.PaymentRepository;
import am.asukiasyan.booking.repository.UnitRepository;
import am.asukiasyan.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UnitService unitService;

    @Mock
    private UnitEventService unitEventService;

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private BookingService bookingService;

    private final LocalDate startDate = LocalDate.now();
    private final LocalDate endDate = LocalDate.now().plusDays(1);
    private BookingRequest request;
    private Unit unit;
    private User user;
    private Payment payment;

    @BeforeEach
    void setUp() {
        request = new BookingRequest(1L, 2L, startDate, endDate);
        unit = new Unit();
        unit.setId(1L);
        unit.setBaseCost(new BigDecimal("100"));
        user = new User();
        user.setId(2L);
        payment = new Payment();
    }

    @Test
    void testCreateBookingSuccess() {
        when(unitRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unit));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(bookingRepository.existsActiveBooking(eq(1L), any(), any())).thenReturn(false);
        when(unitService.addMarkup(new BigDecimal("100"))).thenReturn(new BigDecimal("115.00"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            var booking = invocation.getArgument(0, Booking.class);
            booking.setId(10L);
            return booking;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            var pay = invocation.getArgument(0, Payment.class);
            pay.setId(20L);
            return pay;
        });

        var response = bookingService.createBooking(request);

        assertThat(response.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.totalCost()).isEqualByComparingTo("115.00");
        verify(unitEventService).recordEvent(unit, UnitEventType.BOOKED, "Booking created and pending payment");
        verify(availabilityService).decreaseIfPossible();
    }

    @Test
    void testCreateBookingFailUnitNotFound() {
        when(unitRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Unit not found");
    }

    @Test
    void testCreateBookingFailUserNotFound() {
        when(unitRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unit));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void testCreateBookingFailAvailabilityConflict() {
        when(unitRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unit));
        when(bookingRepository.existsActiveBooking(eq(1L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Unit is unavailable");
        verify(bookingRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testCreateBookingFutureDoesNotTouchAvailability() {
        var futureRequest = new BookingRequest(1L, 2L, LocalDate.now().plusDays(10), LocalDate.now().plusDays(11));
        when(unitRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unit));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(bookingRepository.existsActiveBooking(eq(1L), any(), any())).thenReturn(false);
        when(unitService.addMarkup(new BigDecimal("100"))).thenReturn(new BigDecimal("115.00"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            var booking = invocation.getArgument(0, Booking.class);
            booking.setId(10L);
            return booking;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            var pay = invocation.getArgument(0, Payment.class);
            pay.setId(20L);
            return pay;
        });

        bookingService.createBooking(futureRequest);

        verify(availabilityService, never()).decreaseIfPossible();
    }

    @Test
    void testCancelBookingSuccess() {
        var booking = bookingWithStatus(BookingStatus.PENDING_PAYMENT);
        stubFindBooking(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.cancelBooking(1L);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(paymentRepository).findByBookingId(1L);
        verify(availabilityService).increase();
        verify(unitEventService).recordEvent(booking.getUnit(), UnitEventType.CANCELLED, "Booking cancelled");
    }

    @Test
    void testCancelBookingFailNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void testCancelBookingFailPaymentMissing() {
        var booking = bookingWithStatus(BookingStatus.PENDING_PAYMENT);
        stubFindBooking(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Payment record missing");
    }

    @Test
    void testCancelBookingSuccessWhenAlreadyCancelled() {
        var booking = bookingWithStatus(BookingStatus.CANCELLED);
        stubFindBooking(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        var response = bookingService.cancelBooking(1L);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(paymentRepository).findByBookingId(1L);
        verify(availabilityService, never()).increase();
    }

    @Test
    void testConfirmPaymentSuccess() {
        var booking = bookingWithStatus(BookingStatus.PENDING_PAYMENT);
        stubFindBooking(booking);
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        var response = bookingService.confirmPayment(1L);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(unitEventService).recordEvent(booking.getUnit(), UnitEventType.PAYMENT_CONFIRMED, "Payment received");
    }

    @Test
    void testConfirmPaymentFailCancelledBooking() {
        var booking = bookingWithStatus(BookingStatus.CANCELLED);
        stubFindBooking(booking);

        assertThatThrownBy(() -> bookingService.confirmPayment(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot pay for cancelled booking");
    }

    @Test
    void testConfirmPaymentFailExpiredBooking() {
        var booking = bookingWithStatus(BookingStatus.PENDING_PAYMENT);
        booking.setExpiresAt(Instant.now().minusSeconds(10));
        stubFindBooking(booking);

        assertThatThrownBy(() -> bookingService.confirmPayment(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Payment window has expired");
        verify(paymentRepository, never()).findByBookingId(any());
    }

    @Test
    void testCancelExpiredBookingsIncreasesAvailabilityForTodayOnly() {
        var todayBooking = bookingWithStatus(BookingStatus.PENDING_PAYMENT);
        todayBooking.setExpiresAt(Instant.now().minusSeconds(5));
        var futureBooking = bookingWithStatus(BookingStatus.PENDING_PAYMENT);
        futureBooking.setStartDate(LocalDate.now().plusDays(3));
        futureBooking.setEndDate(LocalDate.now().plusDays(4));
        futureBooking.setExpiresAt(Instant.now().minusSeconds(5));

        when(bookingRepository.findExpiredBookings(any())).thenReturn(List.of(todayBooking, futureBooking));
        when(paymentRepository.findByBookingId(any())).thenReturn(Optional.of(payment));

        int cancelled = bookingService.cancelExpiredBookings();

        assertThat(cancelled).isEqualTo(2);
        assertThat(todayBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(futureBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(availabilityService).increaseBy(1);
        verify(unitEventService, atLeastOnce()).recordEvent(eq(unit), eq(UnitEventType.CANCELLED), any());
    }

    @Test
    void testCancelExpiredBookingsReturnsZeroWhenNothingFound() {
        when(bookingRepository.findExpiredBookings(any())).thenReturn(List.of());

        int cancelled = bookingService.cancelExpiredBookings();

        assertThat(cancelled).isZero();
        verify(availabilityService, never()).increaseBy(any(Integer.class));
    }

    @Test
    void testConfirmPaymentFailPaymentMissing() {
        var booking = bookingWithStatus(BookingStatus.PENDING_PAYMENT);
        stubFindBooking(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmPayment(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Payment record missing");
    }



    private void stubFindBooking(Booking booking) {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
    }

    private Booking bookingWithStatus(BookingStatus status) {
        var booking = new Booking();
        booking.setId(1L);
        booking.setUnit(unit);
        booking.setUser(user);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(status);
        return booking;
    }
}
