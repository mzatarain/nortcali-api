package com.nortcali.api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class LoginRateLimiter {

    // 5 intentos por minuto por IP
    private static final long CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    // Entradas se evictan 2 minutos después del último acceso — evita memory leak con IPs únicas
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    public boolean tryConsume(String ip) {
        return buckets.get(ip, this::newBucket).tryConsume(1);
    }

    private Bucket newBucket(String key) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillIntervally(CAPACITY, REFILL_PERIOD)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
