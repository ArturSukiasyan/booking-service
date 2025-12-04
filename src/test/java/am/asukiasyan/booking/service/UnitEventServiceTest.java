package am.asukiasyan.booking.service;

import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.domain.UnitEvent;
import am.asukiasyan.booking.enums.UnitEventType;
import am.asukiasyan.booking.repository.UnitEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnitEventServiceTest {

    @Mock
    private UnitEventRepository unitEventRepository;

    @InjectMocks
    private UnitEventService unitEventService;

    @Test
    void testRecordEventSuccess() {
        var unit = new Unit();
        unitEventService.recordEvent(unit, UnitEventType.CREATED, "details");

        var captor = ArgumentCaptor.forClass(UnitEvent.class);
        verify(unitEventRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getUnit()).isEqualTo(unit);
        assertThat(saved.getEventType()).isEqualTo(UnitEventType.CREATED);
        assertThat(saved.getDetails()).isEqualTo("details");
    }
}
