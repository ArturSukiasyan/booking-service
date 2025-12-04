package am.asukiasyan.booking.repository;

import am.asukiasyan.booking.domain.UnitEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitEventRepository extends JpaRepository<UnitEvent, Long> {
}
