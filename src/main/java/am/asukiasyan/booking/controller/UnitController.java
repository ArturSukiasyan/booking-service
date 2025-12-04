package am.asukiasyan.booking.controller;

import am.asukiasyan.booking.dto.PageResponse;
import am.asukiasyan.booking.dto.UnitRequest;
import am.asukiasyan.booking.dto.UnitResponse;
import am.asukiasyan.booking.dto.UnitSearchRequest;
import am.asukiasyan.booking.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/units")
@RequiredArgsConstructor
@Slf4j
public class UnitController {

    private final UnitService unitService;

    @PostMapping
    @Operation(summary = "Create a new unit")
    public UnitResponse createUnit(@RequestBody @Valid UnitRequest request) {
        log.info("POST /units start rooms={} type={} floor={}", request.rooms(), request.type(), request.floor());
        return unitService.create(request);
    }

    @GetMapping
    @Operation(summary = "Search units with filters and pagination")
    public PageResponse<UnitResponse> searchUnits(@ModelAttribute @Valid UnitSearchRequest request) {
        log.info("GET /units start rooms={} type={} floor={} page={} size={}",
                request.rooms(), request.type(), request.floor(), request.page(), request.size());
        return unitService.search(request);
    }
}
