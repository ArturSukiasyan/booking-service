package am.asukiasyan.booking.service;

import am.asukiasyan.booking.cache.RedisAvailabilityCache;
import am.asukiasyan.booking.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static am.asukiasyan.booking.TestDataHelper.DB_DOWN_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private RedisAvailabilityCache cache;

    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void testInitializeSuccessWhenCacheMissing() {
        stubCacheMissing(3L);

        availabilityService.initialize();

        verify(cache).hasValue();
        verify(unitRepository).countAvailableToday(any(LocalDate.class));
        verify(cache).update(3);
    }

    @Test
    void testInitializeSuccessWhenCachePresent() {
        stubCachePresent();

        availabilityService.initialize();

        verify(cache).hasValue();
        verify(cache, never()).update(any(Integer.class));
        verify(unitRepository, never()).countAvailableToday(any(LocalDate.class));
    }

    @Test
    void testIncreaseSuccess() {
        stubCachePresent();

        availabilityService.increase();
        verify(cache).increment();
    }

    @Test
    void testDecreaseIfPossibleSuccess() {
        stubCachePresent();

        availabilityService.decreaseIfPossible();
        verify(cache).decrement();
    }

    @Test
    void testRefreshFromDatabaseSuccess() {
        when(unitRepository.countAvailableToday(any(LocalDate.class))).thenReturn(5L);

        availabilityService.refreshFromDatabase();

        verify(unitRepository).countAvailableToday(any(LocalDate.class));
        verify(cache).update(5);
    }

    @Test
    void testGetAvailableUnitsSuccess() {
        stubCachePresent();
        when(cache.get()).thenReturn(7);

        var result = availabilityService.getAvailableUnits();

        verify(cache).hasValue();
        verify(cache).get();
        assertThat(result).isEqualTo(7);
    }

    @Test
    void testInitializeFailWhenCacheMissing() {
        when(cache.hasValue()).thenReturn(false);
        when(unitRepository.countAvailableToday(any(LocalDate.class))).thenThrow(new RuntimeException(DB_DOWN_MESSAGE));

        assertThatThrownBy(() -> availabilityService.initialize())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(DB_DOWN_MESSAGE);
    }

    @Test
    void testRefreshFromDatabaseFail() {
        when(unitRepository.countAvailableToday(any(LocalDate.class))).thenThrow(new RuntimeException(DB_DOWN_MESSAGE));

        assertThatThrownBy(() -> availabilityService.refreshFromDatabase())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(DB_DOWN_MESSAGE);
    }

    @Test
    void testGetAvailableUnitsFailWhenCacheThrows() {
        stubCachePresent();
        when(cache.get()).thenThrow(new RuntimeException("cache down"));

        assertThatThrownBy(() -> availabilityService.getAvailableUnits())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cache down");
    }

    private void stubCachePresent() {
        when(cache.hasValue()).thenReturn(true);
    }

    private void stubCacheMissing(long availableCount) {
        when(cache.hasValue()).thenReturn(false);
        when(unitRepository.countAvailableToday(any(LocalDate.class))).thenReturn(availableCount);
    }
}
