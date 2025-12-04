package am.asukiasyan.booking.service;

import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.domain.UnitEvent;
import am.asukiasyan.booking.enums.UnitEventType;
import am.asukiasyan.booking.repository.UnitEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitEventService {

    private final UnitEventRepository unitEventRepository;

    @Transactional
    public void recordEvent(Unit unit, UnitEventType type, String details) {
        unitEventRepository.save(UnitEvent.builder()
                .unit(unit)
                .eventType(type)
                .details(details)
                .build());
        log.info("Recorded unit event unitId={} type={} details={}", unit.getId(), type, details);
    }
}
