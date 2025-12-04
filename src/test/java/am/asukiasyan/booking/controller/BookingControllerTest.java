package am.asukiasyan.booking.controller;

import am.asukiasyan.booking.dto.BookingRequest;
import am.asukiasyan.booking.dto.BookingResponse;
import am.asukiasyan.booking.enums.BookingStatus;
import am.asukiasyan.booking.enums.PaymentStatus;
import am.asukiasyan.booking.exception.ConflictException;
import am.asukiasyan.booking.exception.NotFoundException;
import am.asukiasyan.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static am.asukiasyan.booking.TestDataHelper.*;
import static am.asukiasyan.booking.enums.BookingStatus.CANCELLED;
import static am.asukiasyan.booking.enums.PaymentStatus.PAID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Test
    void createBookingSuccess() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post(BOOKING_PATH).servletPath(SERVLET_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitId":2,"userId":3,"startDate":"2025-01-01","endDate":"2025-01-02"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"unitId\":null,\"userId\":3,\"startDate\":\"2025-01-01\",\"endDate\":\"2025-01-02\"}",
            "{\"unitId\":2,\"userId\":null,\"startDate\":\"2025-01-01\",\"endDate\":\"2025-01-02\"}",
            "{\"unitId\":2,\"userId\":3,\"startDate\":null,\"endDate\":\"2025-01-02\"}",
            "{\"unitId\":2,\"userId\":3,\"startDate\":\"2025-01-02\",\"endDate\":null}",
            "{\"unitId\":2,\"userId\":3,\"startDate\":\"2025-01-02\",\"endDate\":\"2025-01-01\"}"
    })
    void createBookingFailValidation(String payload) throws Exception {
        mockMvc.perform(post(BOOKING_PATH).servletPath(SERVLET_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelBookingSuccess() throws Exception {
        var response = sampleResponseWith(CANCELLED, PaymentStatus.CANCELLED);
        when(bookingService.cancelBooking(1L)).thenReturn(response);

        mockMvc.perform(post(BOOKING_CANCEL_PATH).servletPath(SERVLET_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CANCELLED.name()));
    }

    @Test
    void paySuccess() throws Exception {
        var response = sampleResponseWith(BookingStatus.CONFIRMED, PAID);
        when(bookingService.confirmPayment(1L)).thenReturn(response);

        mockMvc.perform(post(BOOKING_PAY_PATH).servletPath(SERVLET_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value(PAID.name()));
    }

    @Test
    void cancelBookingFailNotFound() throws Exception {
        when(bookingService.cancelBooking(1L))
                .thenThrow(new NotFoundException("Booking not found"));

        mockMvc.perform(post(BOOKING_CANCEL_PATH).servletPath(SERVLET_PATH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found"));
    }

    @Test
    void payFailConflict() throws Exception {
        when(bookingService.confirmPayment(1L))
                .thenThrow(new ConflictException("Cannot pay for cancelled booking"));

        mockMvc.perform(post(BOOKING_PAY_PATH).servletPath(SERVLET_PATH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot pay for cancelled booking"));
    }

    @Test
    void createBookingFailServerError() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new RuntimeException(DB_DOWN_MESSAGE));

        mockMvc.perform(post(BOOKING_PATH).servletPath(SERVLET_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitId":2,"userId":3,"startDate":"2025-01-01","endDate":"2025-01-02"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }

    private BookingResponse sampleResponse() {
        return sampleResponseWith(BookingStatus.PENDING_PAYMENT, PaymentStatus.PENDING);
    }

    private BookingResponse sampleResponseWith(BookingStatus bookingStatus, PaymentStatus paymentStatus) {
        return new BookingResponse(1L, 2L, 3L, LocalDate.now(), LocalDate.now().plusDays(1),
                bookingStatus, paymentStatus, BigDecimal.TEN);
    }
}
