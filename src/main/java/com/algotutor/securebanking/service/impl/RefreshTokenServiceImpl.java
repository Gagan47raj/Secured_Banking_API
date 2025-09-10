package com.algotutor.securebanking.service.impl;

import org.springframework.stereotype.Service;

import com.algotutor.securebanking.entity.RefreshToken;
import com.algotutor.securebanking.entity.User;
import com.algotutor.securebanking.exception.TokenRefreshException;
import com.algotutor.securebanking.repository.UserRepository;
import com.algotutor.securebanking.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    
    // Redis key prefixes
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist_token:";
    
    @Value("${app.jwtRefreshExpirationMs:604800000}") // 7 days
    private Long refreshTokenDurationMs;
    
    @Value("${app.maxRefreshTokensPerUser:5}")
    private int maxTokensPerUser;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public RefreshToken createRefreshToken(String username, HttpServletRequest request) {
        logger.info("Creating refresh token for user: {}", username);
        
        // Find user
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Clean up expired tokens for this user first
        cleanupExpiredTokensForUser(username);
        
        // Limit number of refresh tokens per user
        limitTokensPerUser(username);
        
        // Generate new refresh token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000);
        
        // Get client info
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        RefreshToken refreshToken = new RefreshToken(
            token, username, user.getId(), expiryDate, ipAddress, userAgent);
        
        // Store in Redis
        storeRefreshToken(refreshToken);
        
        logger.info("Refresh token created successfully for user: {}", username);
        return refreshToken;
    }
    
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        try {
            String key = REFRESH_TOKEN_PREFIX + token;
            RefreshToken refreshToken = (RefreshToken) redisTemplate.opsForValue().get(key);
            
            if (refreshToken != null && refreshToken.isExpired()) {
                // Token expired, remove it
                deleteByToken(token);
                return Optional.empty();
            }
            
            return Optional.ofNullable(refreshToken);
        } catch (Exception e) {
            logger.error("Error finding refresh token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            deleteByToken(token.getToken());
            throw new TokenRefreshException("Refresh token has expired. Please log in again.");
        }
        
        // Check if token is blacklisted
        if (isTokenBlacklisted(token.getToken())) {
            throw new TokenRefreshException("Refresh token has been revoked. Please log in again.");
        }
        
        return token;
    }
    
    @Override
    public void deleteByUsername(String username) {
        logger.info("Deleting all refresh tokens for user: {}", username);
        
        List<RefreshToken> userTokens = getActiveTokensByUsername(username);
        
        for (RefreshToken token : userTokens) {
            deleteByToken(token.getToken());
        }
        
        // Clear user tokens set
        String userTokensKey = USER_TOKENS_PREFIX + username;
        redisTemplate.delete(userTokensKey);
    }
    
    @Override
    public void deleteByToken(String token) {
        try {
            RefreshToken refreshToken = (RefreshToken) redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + token);
            
            if (refreshToken != null) {
                // Remove from user's token set
                String userTokensKey = USER_TOKENS_PREFIX + refreshToken.getUsername();
                redisTemplate.opsForSet().remove(userTokensKey, token);
            }
            
            // Delete the token
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
            
            logger.info("Refresh token deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting refresh token: {}", e.getMessage());
        }
    }
    
    @Override
    public void deleteExpiredTokens() {
        logger.info("Starting cleanup of expired refresh tokens");
        
        try {
            Set<String> tokenKeys = redisTemplate.keys(REFRESH_TOKEN_PREFIX + "*");
            
            if (tokenKeys != null) {
                int expiredCount = 0;
                
                for (String key : tokenKeys) {
                    RefreshToken token = (RefreshToken) redisTemplate.opsForValue().get(key);
                    
                    if (token != null && token.isExpired()) {
                        String tokenValue = key.substring(REFRESH_TOKEN_PREFIX.length());
                        deleteByToken(tokenValue);
                        expiredCount++;
                    }
                }
                
                logger.info("Cleaned up {} expired refresh tokens", expiredCount);
            }
        } catch (Exception e) {
            logger.error("Error during expired token cleanup: {}", e.getMessage());
        }
    }
    
    @Override
    public List<RefreshToken> getActiveTokensByUsername(String username) {
        try {
            String userTokensKey = USER_TOKENS_PREFIX + username;
            Set<Object> tokenSet = redisTemplate.opsForSet().members(userTokensKey);
            
            List<RefreshToken> activeTokens = new ArrayList<>();
            
            if (tokenSet != null) {
                for (Object tokenObj : tokenSet) {
                    String token = (String) tokenObj;
                    Optional<RefreshToken> refreshToken = findByToken(token);
                    refreshToken.ifPresent(activeTokens::add);
                }
            }
            
            return activeTokens;
        } catch (Exception e) {
            logger.error("Error getting active tokens for user {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public void revokeAllUserTokens(String username) {
        logger.info("Revoking all tokens for user: {}", username);
        
        List<RefreshToken> userTokens = getActiveTokensByUsername(username);
        
        for (RefreshToken token : userTokens) {
            // Add to blacklist
            blacklistToken(token.getToken());
            
            // Delete from storage
            deleteByToken(token.getToken());
        }
    }
    
    @Override
    public boolean isTokenValid(String token) {
        Optional<RefreshToken> refreshToken = findByToken(token);
        return refreshToken.isPresent() && !refreshToken.get().isExpired() && !isTokenBlacklisted(token);
    }
    
    // Private helper methods
    
    private void storeRefreshToken(RefreshToken refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken.getToken();
        String userTokensKey = USER_TOKENS_PREFIX + refreshToken.getUsername();
        
        // Store token with expiration
        redisTemplate.opsForValue().set(tokenKey, refreshToken, 
            refreshTokenDurationMs, TimeUnit.MILLISECONDS);
        
        // Add token to user's token set
        redisTemplate.opsForSet().add(userTokensKey, refreshToken.getToken());
        redisTemplate.expire(userTokensKey, refreshTokenDurationMs, TimeUnit.MILLISECONDS);
    }
    
    private void cleanupExpiredTokensForUser(String username) {
        List<RefreshToken> userTokens = getActiveTokensByUsername(username);
        
        for (RefreshToken token : userTokens) {
            if (token.isExpired()) {
                deleteByToken(token.getToken());
            }
        }
    }
    
    private void limitTokensPerUser(String username) {
        List<RefreshToken> activeTokens = getActiveTokensByUsername(username);
        
        // If user has reached the limit, remove oldest tokens
        while (activeTokens.size() >= maxTokensPerUser) {
            RefreshToken oldestToken = activeTokens.stream()
                .min(Comparator.comparing(RefreshToken::getCreatedAt))
                .orElse(null);
            
            if (oldestToken != null) {
                deleteByToken(oldestToken.getToken());
                activeTokens.remove(oldestToken);
            } else {
                break;
            }
        }
    }
    
    private void blacklistToken(String token) {
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(blacklistKey, "revoked", 
            refreshTokenDurationMs, TimeUnit.MILLISECONDS);
    }
    
    private boolean isTokenBlacklisted(String token) {
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(blacklistKey);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
