package am.asukiasyan.booking.controller;

import am.asukiasyan.booking.dto.AvailabilityResponse;
import am.asukiasyan.booking.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Slf4j
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/availability")
    @Operation(summary = "Get current available unit count")
    public AvailabilityResponse availability() {
        log.info("GET /stats/availability start");
        return new AvailabilityResponse(availabilityService.getAvailableUnits());
    }
}
