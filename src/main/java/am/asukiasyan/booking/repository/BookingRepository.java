package am.asukiasyan.booking.repository;

import am.asukiasyan.booking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.unit.id = :unitId
              and b.status <> 'CANCELLED'
              and b.endDate >= :startDate
              and b.startDate <= :endDate
            """)
    boolean existsActiveBooking(
            @Param("unitId") Long unitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from Booking b
            where b.status = 'PENDING_PAYMENT'
              and b.expiresAt is not null
              and b.expiresAt <= :now
            """)
    List<Booking> findExpiredBookings(@Param("now") Instant now);
}
