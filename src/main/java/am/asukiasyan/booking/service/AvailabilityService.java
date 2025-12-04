package am.asukiasyan.booking.service;

import am.asukiasyan.booking.cache.RedisAvailabilityCache;
import am.asukiasyan.booking.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvailabilityService {

    private final RedisAvailabilityCache cache;
    private final UnitRepository unitRepository;

    public void initialize() {
        if (!cache.hasValue()) {
            log.info("Initializing availability cache");
            refreshFromDatabase();
        }
    }

    public void increase() {
        initialize();
        cache.increment();
        log.info("Availability increased to {}", cache.get());
    }

    public void decreaseIfPossible() {
        initialize();
        cache.decrement();
        log.info("Availability decreased to {}", cache.get());
    }

    public void refreshFromDatabase() {
        var count = (int) unitRepository.countAvailableToday(LocalDate.now());
        cache.update(count);
        log.info("Availability refreshed from DB value={}", count);
    }

    public int getAvailableUnits() {
        initialize();
        return cache.get();
    }

    public void increaseBy(int count) {
        if (count <= 0) {
            return;
        }
        initialize();
        IntStream.range(0, count).forEach(i -> cache.increment());
        log.info("Availability increased by {} to {}", count, cache.get());
    }
}
