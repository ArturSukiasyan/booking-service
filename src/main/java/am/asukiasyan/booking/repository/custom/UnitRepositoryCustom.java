package am.asukiasyan.booking.repository.custom;

import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.enums.UnitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface UnitRepositoryCustom {

    Page<Unit> search(UnitType type,
                      Integer rooms,
                      Integer floor,
                      BigDecimal minBaseCost,
                      BigDecimal maxBaseCost,
                      LocalDate startDate,
                      LocalDate endDate,
                      boolean applyAvailability,
                      Pageable pageable);
}
