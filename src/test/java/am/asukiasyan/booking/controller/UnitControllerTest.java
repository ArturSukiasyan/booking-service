package am.asukiasyan.booking.controller;

import am.asukiasyan.booking.dto.PageResponse;
import am.asukiasyan.booking.dto.UnitRequest;
import am.asukiasyan.booking.dto.UnitResponse;
import am.asukiasyan.booking.dto.UnitSearchRequest;
import am.asukiasyan.booking.enums.UnitType;
import am.asukiasyan.booking.service.UnitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static am.asukiasyan.booking.TestDataHelper.UNIT_PATH;
import static am.asukiasyan.booking.TestDataHelper.SERVLET_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UnitController.class)
class UnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnitService unitService;

    @Test
    void createsUnitSuccess() throws Exception {
        var response = new UnitResponse(1L, 2, UnitType.HOME, 1, "desc",
                BigDecimal.valueOf(50), BigDecimal.valueOf(57.5), Instant.now());
        when(unitService.create(any(UnitRequest.class))).thenReturn(response);

        mockMvc.perform(post(UNIT_PATH).servletPath(SERVLET_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rooms":2,"type":"HOME","floor":1,"description":"desc","baseCost":50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void searchesUnitsSuccess() throws Exception {
        var response = new UnitResponse(2L, 1, UnitType.FLAT, 2, "flat",
                BigDecimal.valueOf(80), BigDecimal.valueOf(92), Instant.now());
        when(unitService.search(any(UnitSearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(response), 0, 10, 1));

        mockMvc.perform(get(UNIT_PATH).servletPath(SERVLET_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"rooms\":0,\"type\":\"HOME\",\"floor\":-1,\"description\":\"\",\"baseCost\":-5}",
            "{\"rooms\":2,\"type\":null,\"floor\":1,\"description\":\"desc\",\"baseCost\":50}",
            "{\"rooms\":2,\"type\":\"HOME\",\"floor\":1,\"description\":null,\"baseCost\":50}",
            "{\"rooms\":2,\"type\":\"HOME\",\"floor\":1,\"description\":\"desc\",\"baseCost\":null}"
    })
    void createUnitValidationFailure(String payload) throws Exception {
        mockMvc.perform(post(UNIT_PATH).servletPath(SERVLET_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
