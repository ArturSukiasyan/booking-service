package am.asukiasyan.booking.service;

import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.dto.PageResponse;
import am.asukiasyan.booking.dto.UnitRequest;
import am.asukiasyan.booking.dto.UnitResponse;
import am.asukiasyan.booking.dto.UnitSearchRequest;
import am.asukiasyan.booking.enums.UnitType;
import am.asukiasyan.booking.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static am.asukiasyan.booking.enums.UnitEventType.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UnitEventService unitEventService;

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private UnitService unitService;

    @Test
    void testAddMarkupSuccess() {
        assertThat(unitService.addMarkup(new BigDecimal("100"))).isEqualByComparingTo("115.00");
    }

    @Test
    void testCreateSuccess() {
        when(unitRepository.save(any(Unit.class)))
                .thenAnswer(invocation -> {
                    var unit = invocation.getArgument(0, Unit.class);
                    unit.setId(1L);
                    return unit;
                });

        var response = unitService.create(sampleRequest());
        var captor = ArgumentCaptor.forClass(Unit.class);
        verify(unitRepository).save(captor.capture());

        var saved = captor.getValue();

        assertThat(saved.getRooms()).isEqualTo(2);
        assertThat(saved.getType()).isEqualTo(UnitType.FLAT);
        assertThat(saved.getBaseCost()).isEqualByComparingTo("50");
        verify(unitEventService)
                .recordEvent(saved, CREATED, "Unit created");
        verify(availabilityService).increase();
        assertThat(response.finalCost()).isEqualByComparingTo("57.50");
    }

    @Test
    void testSearchSuccess() {
        var unit = buildUnit();

        when(unitRepository.search(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(unit), PageRequest.of(0, 10), 1));

        PageResponse<UnitResponse> response = unitService.search(sampleSearchRequest());

        var minCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        var maxCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(unitRepository)
                .search(any(), any(), any(), minCaptor.capture(), maxCaptor.capture(), any(), any(), anyBoolean(), any());

        assertThat(minCaptor.getValue()).isEqualByComparingTo("100.00");
        assertThat(maxCaptor.getValue()).isEqualByComparingTo("200.00");
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().finalCost()).isEqualByComparingTo("115.00");
    }

    private UnitRequest sampleRequest() {
        return new UnitRequest(2, UnitType.FLAT, 3, "desc", new BigDecimal("50"));
    }

    private UnitSearchRequest sampleSearchRequest() {
        return new UnitSearchRequest(
                null,
                UnitType.APARTMENTS,
                null,
                new BigDecimal("115.00"),
                new BigDecimal("230.00"),
                null,
                null,
                0,
                10,
                "id",
                Sort.Direction.ASC
        );
    }

    private Unit buildUnit() {
        return Unit.builder()
                .baseCost(new BigDecimal("100.00"))
                .rooms(2)
                .type(UnitType.APARTMENTS)
                .floor(3)
                .description("desc")
                .build();
    }
}
