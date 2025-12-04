package am.asukiasyan.booking.repository.custom;

import am.asukiasyan.booking.domain.Booking;
import am.asukiasyan.booking.domain.Unit;
import am.asukiasyan.booking.enums.BookingStatus;
import am.asukiasyan.booking.enums.UnitType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UnitRepositoryImpl implements UnitRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Unit> search(UnitType type,
                             Integer rooms,
                             Integer floor,
                             BigDecimal minBaseCost,
                             BigDecimal maxBaseCost,
                             LocalDate startDate,
                             LocalDate endDate,
                             boolean applyAvailability,
                             Pageable pageable) {

        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(Unit.class);
        var unit = cq.from(Unit.class);

        var predicates = buildPredicates(cb, cq, unit, type, rooms, floor, minBaseCost, maxBaseCost, startDate, endDate, applyAvailability);

        cq.select(unit).where(predicates.toArray(new Predicate[0]));
        cq.orderBy(toOrders(cb, unit, pageable.getSort()));

        var query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        var content = query.getResultList();

        long total = count(cb, type, rooms, floor, minBaseCost, maxBaseCost, startDate, endDate, applyAvailability);

        return new PageImpl<>(content, pageable, total);
    }

    private long count(CriteriaBuilder cb,
                       UnitType type,
                       Integer rooms,
                       Integer floor,
                       BigDecimal minBaseCost,
                       BigDecimal maxBaseCost,
                       LocalDate startDate,
                       LocalDate endDate,
                       boolean applyAvailability) {

        var countQuery = cb.createQuery(Long.class);
        var unitRoot = countQuery.from(Unit.class);

        var predicates = buildPredicates(cb, countQuery, unitRoot, type, rooms, floor, minBaseCost, maxBaseCost, startDate, endDate, applyAvailability);
        countQuery.select(cb.count(unitRoot)).where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            CriteriaQuery<?> query,
                                            Root<Unit> unit,
                                            UnitType type,
                                            Integer rooms,
                                            Integer floor,
                                            BigDecimal minBaseCost,
                                            BigDecimal maxBaseCost,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            boolean applyAvailability) {
        List<Predicate> predicates = new ArrayList<>();

        if (type != null) {
            predicates.add(cb.equal(unit.get("type"), type));
        }
        if (rooms != null) {
            predicates.add(cb.equal(unit.get("rooms"), rooms));
        }
        if (floor != null) {
            predicates.add(cb.equal(unit.get("floor"), floor));
        }
        if (minBaseCost != null) {
            predicates.add(cb.greaterThanOrEqualTo(unit.get("baseCost"), minBaseCost));
        }
        if (maxBaseCost != null) {
            predicates.add(cb.lessThanOrEqualTo(unit.get("baseCost"), maxBaseCost));
        }

        if (applyAvailability && startDate != null && endDate != null) {
            var bookingSubquery = query.subquery(Long.class);
            var booking = bookingSubquery.from(Booking.class);
            bookingSubquery.select(cb.literal(1L));

            List<Predicate> bookingPredicates = new ArrayList<>();
            bookingPredicates.add(cb.equal(booking.get("unit"), unit));
            bookingPredicates.add(cb.notEqual(booking.get("status"), BookingStatus.CANCELLED));
            bookingPredicates.add(cb.greaterThanOrEqualTo(booking.get("endDate"), startDate));
            bookingPredicates.add(cb.lessThanOrEqualTo(booking.get("startDate"), endDate));

            bookingSubquery.where(bookingPredicates.toArray(new Predicate[0]));
            predicates.add(cb.not(cb.exists(bookingSubquery)));
        }

        return predicates;
    }

    private List<Order> toOrders(CriteriaBuilder cb, Root<Unit> unit, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return List.of(cb.asc(unit.get("id")));
        }
        List<Order> orders = new ArrayList<>();
        for (Sort.Order s : sort) {
            Expression<?> expression = unit.get(s.getProperty());
            orders.add(s.isAscending() ? cb.asc(expression) : cb.desc(expression));
        }
        return orders;
    }
}
