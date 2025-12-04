package am.asukiasyan.booking.integration;

import am.asukiasyan.booking.cache.RedisAvailabilityCache;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;

import static am.asukiasyan.booking.TestDataHelper.REDIS_PORT;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisAvailabilityCacheIntegrationTest extends TestContainersConfig {

    private RedisAvailabilityCache createCache() {
        var config = new RedisStandaloneConfiguration(
                REDIS.getHost(),
                REDIS.getMappedPort(REDIS_PORT)
        );
        var connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        var template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        Objects.requireNonNull(template.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushAll();
        return new RedisAvailabilityCache(template);
    }

    @Test
    void incrementsAndDecrementsBoundedAtZero() {
        RedisAvailabilityCache cache = createCache();
        cache.update(3);

        cache.increment();
        cache.increment();
        for (int i = 0; i < 7; i++) {
            cache.decrement();
        }

        assertThat(cache.get()).isEqualTo(0);
        cache.update(5);
        assertThat(cache.get()).isEqualTo(5);
    }
}
