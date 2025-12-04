package am.asukiasyan.booking.config;

import am.asukiasyan.booking.cache.RedisAvailabilityCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisAvailabilityCache availabilityCache(StringRedisTemplate redisTemplate) {
        return new RedisAvailabilityCache(redisTemplate);
    }
}
