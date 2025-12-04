package am.asukiasyan.booking.repository;

import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.enums.UnitType;
import am.asukiasyan.booking.repository.custom.UnitRepositoryCustom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long>, UnitRepositoryCustom {

    Page<Unit> search(@Param("type") UnitType type,
                      @Param("rooms") Integer rooms,
                      @Param("floor") Integer floor,
                      @Param("minBaseCost") BigDecimal minBaseCost,
                      @Param("maxBaseCost") BigDecimal maxBaseCost,
                      @Param("startDate") LocalDate startDate,
                      @Param("endDate") LocalDate endDate,
                      @Param("applyAvailability") boolean applyAvailability,
                      Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Unit u where u.id = :id")
    Optional<Unit> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(u) from Unit u
            where not exists(
                select b from Booking b
                where b.unit = u
                  and b.status <> 'CANCELLED'
                  and b.startDate <= :today
                  and b.endDate >= :today
            )
            """)
    long countAvailableToday(@Param("today") LocalDate today);
}
