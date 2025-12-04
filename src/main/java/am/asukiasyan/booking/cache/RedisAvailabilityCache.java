package am.asukiasyan.booking.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public record RedisAvailabilityCache(StringRedisTemplate redisTemplate, ValueOperations<String, String> ops) {

    private static final String KEY = "availability:count";

    public RedisAvailabilityCache(StringRedisTemplate redisTemplate) {
        this(redisTemplate, redisTemplate.opsForValue());
    }

    public int get() {
        String value = ops.get(KEY);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            ops.set(KEY, "0");
            return 0;
        }
    }

    public void increment() {
        ops.increment(KEY);
    }

    public void decrement() {
        Long current = ops.decrement(KEY);
        if (current != null && current < 0) {
            ops.set(KEY, "0");
        }
    }

    public void update(int newValue) {
        ops.set(KEY, Integer.toString(newValue));
    }

    public boolean hasValue() {
        return redisTemplate.hasKey(KEY);
    }
}
