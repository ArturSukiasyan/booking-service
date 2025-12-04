package am.asukiasyan.booking.service;

import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.dto.PageResponse;
import am.asukiasyan.booking.dto.UnitRequest;
import am.asukiasyan.booking.dto.UnitResponse;
import am.asukiasyan.booking.dto.UnitSearchRequest;
import am.asukiasyan.booking.enums.UnitEventType;
import am.asukiasyan.booking.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitService {

    private static final BigDecimal MARKUP_MULTIPLIER = BigDecimal.valueOf(1.15);
    private static final int COST_SCALE = 2;
    private static final RoundingMode COST_ROUNDING = RoundingMode.HALF_UP;

    private final UnitRepository unitRepository;
    private final UnitEventService unitEventService;
    private final AvailabilityService availabilityService;

    @Transactional
    public UnitResponse create(UnitRequest request) {
        log.info("Creating unit rooms={} type={} floor={}", request.rooms(), request.type(), request.floor());
        var saved = unitRepository.save(buildUnit(request));
        unitEventService.recordEvent(saved, UnitEventType.CREATED, "Unit created");
        availabilityService.increase();
        log.info("Unit created id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UnitResponse> search(UnitSearchRequest request) {

        log.info("Searching by {}", request);
        var minBase = adjustToBase(request.minCost());
        var maxBase = adjustToBase(request.maxCost());
        var applyAvailability = request.startDate() != null && request.endDate() != null;
        var startDate = applyAvailability ? request.startDate() : LocalDate.now();
        var endDate = applyAvailability ? request.endDate() : LocalDate.now();
        var pageable = PageRequest.of(request.page(), request.size(), Sort.by(request.direction(), request.sortBy()));
        var units = unitRepository.search(
                request.type(),
                request.rooms(),
                request.floor(),
                minBase,
                maxBase,
                startDate,
                endDate,
                applyAvailability,
                pageable);

        var responses = units.stream().map(this::toResponse).toList();
        log.info("Search completed total={} page={} size={}", units.getTotalElements(), units.getNumber(), units.getSize());
        return new PageResponse<>(responses, units.getNumber(), units.getSize(), units.getTotalElements());
    }

    public BigDecimal addMarkup(BigDecimal baseCost) {
        return baseCost.multiply(MARKUP_MULTIPLIER).setScale(COST_SCALE, COST_ROUNDING);
    }

    private BigDecimal adjustToBase(BigDecimal finalCost) {
        if (finalCost == null) {
            return null;
        }
        return finalCost.divide(MARKUP_MULTIPLIER, COST_SCALE, COST_ROUNDING);
    }

    private Unit buildUnit(UnitRequest request) {
        return Unit.builder()
                .rooms(request.rooms())
                .type(request.type())
                .floor(request.floor())
                .description(request.description())
                .baseCost(request.baseCost())
                .build();
    }

    private UnitResponse toResponse(Unit unit) {
        return new UnitResponse(
                unit.getId(),
                unit.getRooms(),
                unit.getType(),
                unit.getFloor(),
                unit.getDescription(),
                unit.getBaseCost(),
                addMarkup(unit.getBaseCost()),
                unit.getCreatedAt()
        );
    }
}
