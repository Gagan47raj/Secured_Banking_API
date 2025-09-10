
package com.algotutor.securebanking.scheduler;

import com.algotutor.securebanking.service.RateLimitService;
import com.algotutor.securebanking.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for cleaning up expired tokens and rate limit buckets
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class CleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(CleanupScheduler.class);
    
    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * Clean up expired refresh tokens every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTokens() {
        logger.info("Starting scheduled cleanup of expired refresh tokens");
        try {
            refreshTokenService.deleteExpiredTokens();
            logger.info("Completed cleanup of expired refresh tokens");
        } catch (Exception e) {
            logger.error("Error during token cleanup: {}", e.getMessage());
        }
    }
    
    /**
     * Clean up expired rate limit buckets every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void cleanupRateLimitBuckets() {
        logger.info("Starting scheduled cleanup of rate limit buckets");
        try {
            rateLimitService.cleanupExpiredBuckets();
            logger.info("Completed cleanup of rate limit buckets");
        } catch (Exception e) {
            logger.error("Error during rate limit cleanup: {}", e.getMessage());
        }
    }
}
