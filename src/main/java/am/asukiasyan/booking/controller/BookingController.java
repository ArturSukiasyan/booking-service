package am.asukiasyan.booking.controller;

import am.asukiasyan.booking.dto.BookingRequest;
import am.asukiasyan.booking.dto.BookingResponse;
import am.asukiasyan.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a booking")
    public BookingResponse createBooking(@RequestBody @Valid BookingRequest request) {
        log.info("POST /bookings start userId={} unitId={} startDate={} endDate={}",
                request.userId(), request.unitId(), request.startDate(), request.endDate());
        return bookingService.createBooking(request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public BookingResponse cancelBooking(@PathVariable Long id) {
        log.info("POST /bookings/{}/cancel start", id);
        return bookingService.cancelBooking(id);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Confirm payment for a booking")
    public BookingResponse pay(@PathVariable Long id) {
        log.info("POST /bookings/{}/pay start", id);
        return bookingService.confirmPayment(id);
    }
}
