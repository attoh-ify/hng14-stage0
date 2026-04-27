package hng14.stage0.nameclassifier.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        long now = Instant.now().getEpochSecond();

        RateLimitBucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || now >= existing.windowStart + windowSeconds) {
                return new RateLimitBucket(now, 1);
            }

            existing.requestCount++;
            return existing;
        });

        return bucket.requestCount <= maxRequests;
    }

    private static class RateLimitBucket {
        private final long windowStart;
        private int requestCount;

        private RateLimitBucket(long windowStart, int requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}