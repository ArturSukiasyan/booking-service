package am.asukiasyan.booking.integration;

import am.asukiasyan.booking.domain.Booking;
import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.enums.BookingStatus;
import am.asukiasyan.booking.enums.UnitType;
import am.asukiasyan.booking.dto.UnitResponse;
import am.asukiasyan.booking.dto.UnitSearchRequest;
import am.asukiasyan.booking.repository.BookingRepository;
import am.asukiasyan.booking.repository.UnitEventRepository;
import am.asukiasyan.booking.repository.UnitRepository;
import am.asukiasyan.booking.repository.UserRepository;
import am.asukiasyan.booking.service.UnitService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UnitSearchIntegrationTest extends TestContainersConfig {

    @Autowired
    private UnitService unitService;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UnitEventRepository unitEventRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<Unit> seededUnits = new ArrayList<>();

    @BeforeAll
    void setupData() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
        cleanupCustomData();
        var user = userRepository.findById(1L).orElseThrow();

        seededUnits.add(saveUnit(4, UnitType.HOME, 600, "Search test lux home", new BigDecimal("900.00")));
        seededUnits.add(saveUnit(1, UnitType.FLAT, 601, "Search test available flat", new BigDecimal("120.00")));
        seededUnits.add(saveUnit(1, UnitType.FLAT, 601, "Search test booked flat", new BigDecimal("150.00")));
        seededUnits.add(saveUnit(3, UnitType.APARTMENTS, 602, "Search test high floor apt", new BigDecimal("400.00")));
        seededUnits.add(saveUnit(5, UnitType.HOME, 603, "Search test family home", new BigDecimal("500.00")));

        bookingRepository.save(Booking.builder()
                .unit(findByDescription("Search test booked flat"))
                .user(user)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .status(BookingStatus.CONFIRMED)
                .totalCost(new BigDecimal("150.00"))
                .build());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unitSearchCases")
    void filtersUnitsBySearchCriteria(SearchCase searchCase) {
        var results = unitService.search(searchCase.request());
        var resultIds = results.content().stream().map(UnitResponse::id).toList();

        if (searchCase.exactOrder()) {
            assertThat(resultIds).containsExactlyElementsOf(searchCase.expectedUnitIds());
        } else {
            assertThat(resultIds).containsExactlyInAnyOrderElementsOf(searchCase.expectedUnitIds());
        }
    }

    Stream<SearchCase> unitSearchCases() {
        var today = LocalDate.now();
        var luxHome = findByDescription("Search test lux home");
        var availableFlat = findByDescription("Search test available flat");
        var bookedFlat = findByDescription("Search test booked flat");
        var highFloorApt = findByDescription("Search test high floor apt");
        var familyHome = findByDescription("Search test family home");

        return Stream.of(
                new SearchCase(
                        "filters by type + floor + high cost",
                        request(null, UnitType.HOME, 600, new BigDecimal("900.00"), new BigDecimal("1200.00"), null, null, "id", Sort.Direction.ASC),
                        List.of(luxHome.getId()),
                        false
                ),
                new SearchCase(
                        "excludes booked units when availability overlaps",
                        request(1, UnitType.FLAT, 601, new BigDecimal("0.00"), new BigDecimal("500.00"), today, today.plusDays(1), "id", Sort.Direction.ASC),
                        List.of(availableFlat.getId()),
                        false
                ),
                new SearchCase(
                        "includes booked unit when availability does not overlap",
                        request(1, UnitType.FLAT, 601, new BigDecimal("0.00"), new BigDecimal("500.00"), today.plusDays(10), today.plusDays(12), "id", Sort.Direction.ASC),
                        List.of(availableFlat.getId(), bookedFlat.getId()),
                        false
                ),
                new SearchCase(
                        "filters by rooms and floor only",
                        request(5, null, 603, null, null, null, null, "id", Sort.Direction.ASC),
                        List.of(familyHome.getId()),
                        false
                ),
                new SearchCase(
                        "filters by cost range irrespective of type",
                        request(null, null, null, new BigDecimal("450.00"), new BigDecimal("950.00"), null, null, "id", Sort.Direction.ASC),
                        List.of(familyHome.getId(), highFloorApt.getId()),
                        false
                ),
                new SearchCase(
                        "sorts by baseCost descending when no availability filter applied",
                        request(1, UnitType.FLAT, 601, null, null, null, null, "baseCost", Sort.Direction.DESC),
                        List.of(bookedFlat.getId(), availableFlat.getId()),
                        true
                ),
                new SearchCase(
                        "filters apartments on floor and returns single match",
                        request(3, UnitType.APARTMENTS, 602, null, null, null, null, "id", Sort.Direction.ASC),
                        List.of(highFloorApt.getId()),
                        false
                )
        );
    }

    private UnitSearchRequest request(Integer rooms,
                                      UnitType type,
                                      Integer floor,
                                      BigDecimal minFinalCost,
                                      BigDecimal maxFinalCost,
                                      LocalDate start,
                                      LocalDate end,
                                      String sortBy,
                                      Sort.Direction direction) {
        return new UnitSearchRequest(
                rooms,
                type,
                floor,
                minFinalCost,
                maxFinalCost,
                start,
                end,
                0,
                10,
                sortBy,
                direction
        );
    }

    private void cleanupCustomData() {
        Set<Integer> customFloors = Set.of(600, 601, 602, 603);
        var existingCustomUnits = unitRepository.findAll().stream()
                .filter(unit -> customFloors.contains(unit.getFloor()))
                .toList();
        if (existingCustomUnits.isEmpty()) {
            return;
        }
        var unitIds = existingCustomUnits.stream().map(Unit::getId).toList();
        var bookingsToRemove = bookingRepository.findAll().stream()
                .filter(booking -> unitIds.contains(booking.getUnit().getId()))
                .toList();
        bookingRepository.deleteAll(bookingsToRemove);
        var unitEventsToRemove = unitEventRepository.findAll().stream()
                .filter(event -> unitIds.contains(event.getUnit().getId()))
                .toList();
        unitEventRepository.deleteAll(unitEventsToRemove);
        unitRepository.deleteAll(existingCustomUnits);
    }

    private Unit findByDescription(String description) {
        return seededUnits.stream()
                .filter(unit -> unit.getDescription().equals(description))
                .findFirst()
                .orElseThrow();
    }

    private Unit saveUnit(int rooms, UnitType type, int floor, String description, BigDecimal baseCost) {
        var unit = Unit.builder()
                .rooms(rooms)
                .type(type)
                .floor(floor)
                .description(description)
                .baseCost(baseCost)
                .build();
        return unitRepository.save(unit);
    }

    private record SearchCase(String name, UnitSearchRequest request, List<Long> expectedUnitIds, boolean exactOrder) {
        @Override
        public String toString() {
            return name;
        }
    }
}
