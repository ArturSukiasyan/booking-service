package am.asukiasyan.booking.service;

import am.asukiasyan.booking.domain.Booking;
import am.asukiasyan.booking.domain.Payment;
import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.domain.User;
import am.asukiasyan.booking.dto.BookingRequest;
import am.asukiasyan.booking.dto.BookingResponse;
import am.asukiasyan.booking.exception.ConflictException;
import am.asukiasyan.booking.exception.NotFoundException;
import am.asukiasyan.booking.enums.BookingStatus;
import am.asukiasyan.booking.enums.PaymentStatus;
import am.asukiasyan.booking.enums.UnitEventType;
import am.asukiasyan.booking.repository.BookingRepository;
import am.asukiasyan.booking.repository.PaymentRepository;
import am.asukiasyan.booking.repository.UnitRepository;
import am.asukiasyan.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final Duration PAYMENT_WINDOW = Duration.ofMinutes(15);

    private final BookingRepository bookingRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final UnitService unitService;
    private final UnitEventService unitEventService;
    private final AvailabilityService availabilityService;
    private static final String TTL_CANCEL_DETAILS = "Cancelled by TTL";

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        log.info("Creating booking unitId={} userId={} start={} end={}",
                request.unitId(), request.userId(), request.startDate(), request.endDate());
        var unit = loadUnitForUpdate(request.unitId());
        ensureAvailable(unit, request.startDate(), request.endDate());

        var user = loadUser(request.userId());
        var booking = bookingRepository.save(buildBooking(unit, user, request.startDate(), request.endDate()));
        var payment = paymentRepository.save(buildPendingPayment(booking));

        unitEventService.recordEvent(unit, UnitEventType.BOOKED, "Booking created and pending payment");
        adjustAvailabilityForToday(booking.getStartDate(), booking.getEndDate(), availabilityService::decreaseIfPossible);

        log.info("Booking created id={} paymentId={}", booking.getId(), payment.getId());
        return toResponse(booking, payment);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        var booking = findBooking(bookingId);
        var payment = findPaymentOrThrow(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Cancel requested for already cancelled booking id={}", bookingId);
            return toResponse(booking, payment);
        }

        log.info("Cancelling booking id={}", bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(Instant.now());
        booking.setExpiresAt(null);
        var saved = bookingRepository.save(booking);

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        unitEventService.recordEvent(saved.getUnit(), UnitEventType.CANCELLED, "Booking cancelled");
        adjustAvailabilityForToday(booking.getStartDate(), booking.getEndDate(), availabilityService::increase);
        log.info("Booking cancelled id={} paymentId={}", bookingId, payment.getId());
        return toResponse(saved, payment);
    }

    @Transactional
    public int cancelExpiredBookings() {
        var expired = bookingRepository.findExpiredBookings(Instant.now());
        if (expired.isEmpty()) {
            return 0;
        }

        int todaysCancellations = 0;
        for (Booking booking : expired) {
            if (affectsToday(booking.getStartDate(), booking.getEndDate())) {
                todaysCancellations++;
            }
            cancelExpiredBooking(booking);
        }
        if (todaysCancellations > 0) {
            availabilityService.increaseBy(todaysCancellations);
        }
        log.info("Expired booking cancellation run completed; cancelled={}", expired.size());
        return expired.size();
    }

    @Transactional
    public BookingResponse confirmPayment(Long bookingId) {
        var booking = findBooking(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Cannot pay for cancelled booking");
        }
        assertNotExpired(booking);

        log.info("Confirming payment for booking id={}", bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(Instant.now());
        booking.setExpiresAt(null);
        var saved = bookingRepository.save(booking);

        var payment = findPaymentOrThrow(bookingId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        unitEventService.recordEvent(saved.getUnit(), UnitEventType.PAYMENT_CONFIRMED, "Payment received");
        log.info("Payment confirmed for booking id={} paymentId={}", bookingId, payment.getId());
        return toResponse(saved, payment);
    }

    private Unit loadUnitForUpdate(Long unitId) {
        return unitRepository.findByIdForUpdate(unitId)
                .orElseThrow(() -> new NotFoundException("Unit not found"));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void ensureAvailable(Unit unit, LocalDate start, LocalDate end) {
        if (bookingRepository.existsActiveBooking(unit.getId(), start, end)) {
            log.warn("Availability check failed for unitId={} start={} end={}", unit.getId(), start, end);
            throw new ConflictException("Unit is unavailable for the selected dates");
        }
    }

    private Booking buildBooking(Unit unit, User user, LocalDate start, LocalDate end) {
        return Booking.builder()
                .unit(unit)
                .user(user)
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.PENDING_PAYMENT)
                .totalCost(calculateTotalCost(unit.getBaseCost()))
                .expiresAt(Instant.now().plus(PAYMENT_WINDOW))
                .build();
    }

    private Payment buildPendingPayment(Booking booking) {
        return Payment.builder()
                .booking(booking)
                .status(PaymentStatus.PENDING)
                .build();
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private Payment findPaymentOrThrow(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NotFoundException("Payment record missing"));
    }

    private BookingResponse toResponse(Booking booking, Payment payment) {
        return new BookingResponse(
                booking.getId(),
                booking.getUnit().getId(),
                booking.getUser().getId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus(),
                payment.getStatus(),
                booking.getTotalCost()
        );
    }

    private BigDecimal calculateTotalCost(BigDecimal baseCost) {
        return unitService.addMarkup(baseCost);
    }

    private void adjustAvailabilityForToday(LocalDate startDate, LocalDate endDate, Runnable adjustment) {
        if (affectsToday(startDate, endDate)) {
            adjustment.run();
        }
    }

    private boolean affectsToday(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        var today = LocalDate.now();
        return !startDate.isAfter(today) && !endDate.isBefore(today);
    }

    private void assertNotExpired(Booking booking) {
        var expiresAt = booking.getExpiresAt();
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new ConflictException("Payment window has expired");
        }
    }

    private void cancelExpiredBooking(Booking booking) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(Instant.now());
        booking.setExpiresAt(null);
        bookingRepository.save(booking);

        paymentRepository.findByBookingId(booking.getId())
                .ifPresentOrElse(payment -> {
                    payment.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(payment);
                }, () -> log.warn("Payment record missing for expired booking id={}", booking.getId()));

        unitEventService.recordEvent(booking.getUnit(), UnitEventType.CANCELLED, TTL_CANCEL_DETAILS);
    }
}
