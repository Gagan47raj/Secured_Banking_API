package com.algotutor.securebanking.service.impl;

import org.springframework.stereotype.Service;

import com.algotutor.securebanking.config.RateLimitConfig;
import com.algotutor.securebanking.service.RateLimitService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitServiceImpl implements RateLimitService{

	  
    private static final Logger logger = LoggerFactory.getLogger(RateLimitServiceImpl.class);
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    
    @Autowired
    private RateLimitConfig rateLimitConfig;
    
    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;
    
    @Override
    public boolean isAllowed(String key, String userRole, String endpoint) {
        try {
            Bucket bucket = getBucket(key, userRole, endpoint);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            
            if (probe.isConsumed()) {
                logger.debug("Rate limit check passed for key: {}, remaining: {}", key, probe.getRemainingTokens());
                return true;
            } else {
                logger.warn("Rate limit exceeded for key: {}, retry after: {} ms", 
                    key, probe.getNanosToWaitForRefill() / 1_000_000);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error in rate limiting check for key {}: {}", key, e.getMessage());
            // Fail open - allow request if rate limiting fails
            return true;
        }
    }
    
    @Override
    public long getAvailableTokens(String key, String userRole, String endpoint) {
        try {
            Bucket bucket = getBucket(key, userRole, endpoint);
            return bucket.getAvailableTokens();
        } catch (Exception e) {
            logger.error("Error getting available tokens for key {}: {}", key, e.getMessage());
            return 0;
        }
    }
    
    
    @Override
    public void resetBucket(String key) {
        try {
            bucketCache.remove(key);
            stringRedisTemplate.delete(RATE_LIMIT_PREFIX + key);
            logger.info("Rate limit bucket reset for key: {}", key);
        } catch (Exception e) {
            logger.error("Error resetting bucket for key {}: {}", key, e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> getRateLimitInfo(String key, String userRole, String endpoint) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            Bucket bucket = getBucket(key, userRole, endpoint);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(0); // Check without consuming
            
            info.put("availableTokens", bucket.getAvailableTokens());
            info.put("capacity", getBandwidthByRole(userRole, endpoint).getCapacity());
            info.put("refillRate", getBandwidthByRole(userRole, endpoint).getRefillTokens());
            info.put("isAllowed", probe.isConsumed());
            
            if (!probe.isConsumed()) {
                info.put("retryAfterMs", probe.getNanosToWaitForRefill() / 1_000_000);
            }
            
        } catch (Exception e) {
            logger.error("Error getting rate limit info for key {}: {}", key, e.getMessage());
            info.put("error", "Unable to retrieve rate limit information");
        }
        
        return info;
    }
    
    @Override
    public void cleanupExpiredBuckets() {
        logger.info("Starting rate limit bucket cleanup");
        
        try {
            // Remove expired entries from local cache
            bucketCache.entrySet().removeIf(entry -> {
                String key = entry.getKey();
                return !stringRedisTemplate.hasKey(RATE_LIMIT_PREFIX + key);
            });
            
            logger.info("Rate limit bucket cleanup completed");
        } catch (Exception e) {
            logger.error("Error during rate limit cleanup: {}", e.getMessage());
        }
    }
    
    private Bucket getBucket(String key, String userRole, String endpoint) {
        return bucketCache.computeIfAbsent(key, k -> createBucket(userRole, endpoint));
    }
    
    private Bucket createBucket(String userRole, String endpoint) {
        Bandwidth bandwidth = getBandwidthByRole(userRole, endpoint);
        
        return Bucket4j.builder()
            .addLimit(bandwidth)
            .build();
    }
    
    private Bandwidth getBandwidthByRole(String userRole, String endpoint) {
        // Special handling for different endpoint types
        if (endpoint != null) {
            if (endpoint.contains("/auth/")) {
                return rateLimitConfig.authBandwidth();
            }
            if (endpoint.contains("/deposit") || endpoint.contains("/withdraw") || endpoint.contains("/transfer")) {
                return rateLimitConfig.bankingBandwidth();
            }
        }
        
        // Role-based limits
        return switch (userRole.toUpperCase()) {
            case "ADMIN", "ROLE_ADMIN" -> rateLimitConfig.adminBandwidth();
            case "CUSTOMER", "ROLE_CUSTOMER" -> rateLimitConfig.customerBandwidth();
            default -> rateLimitConfig.guestBandwidth();
        };
    }
}
