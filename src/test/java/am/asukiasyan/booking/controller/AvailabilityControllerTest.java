package am.asukiasyan.booking.controller;

import am.asukiasyan.booking.service.AvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static am.asukiasyan.booking.TestDataHelper.AVAILABILITY_PATH;
import static am.asukiasyan.booking.TestDataHelper.SERVLET_PATH;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvailabilityController.class)
class AvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvailabilityService availabilityService;

    @Test
    void returnsAvailabilitySuccess() throws Exception {
        when(availabilityService.getAvailableUnits()).thenReturn(7);

        mockMvc.perform(get(AVAILABILITY_PATH).servletPath(SERVLET_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableUnits").value(7));
    }

    @Test
    void returnsServerErrorWhenServiceFails() throws Exception {
        when(availabilityService.getAvailableUnits()).thenThrow(new RuntimeException("cache down"));

        mockMvc.perform(get(AVAILABILITY_PATH).servletPath(SERVLET_PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}
