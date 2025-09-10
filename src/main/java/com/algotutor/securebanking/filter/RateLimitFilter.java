
package com.algotutor.securebanking.filter;

import com.algotutor.securebanking.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limiting Filter
 * Applies distributed rate limiting based on user role and endpoint type
 */
@Component
@Order(1) // Execute before other filters
public class RateLimitFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    @Autowired
    private RateLimitService rateLimitService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Skip rate limiting for certain paths
        if (shouldSkipRateLimit(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientId = getClientIdentifier(request);
        String userRole = getUserRole();
        String endpoint = requestPath;
        
        if (!rateLimitService.isAllowed(clientId, userRole, endpoint)) {
            handleRateLimitExceeded(request, response, clientId, userRole, endpoint);
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean shouldSkipRateLimit(String requestPath) {
        return requestPath.startsWith("/actuator/") ||
               requestPath.startsWith("/swagger-ui/") ||
               requestPath.startsWith("/v3/api-docs") ||
               requestPath.startsWith("/h2-console") ||
               requestPath.equals("/error");
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "user:" + auth.getName();
        }
        
        // Use IP address for unauthenticated users
        return "ip:" + getClientIpAddress(request);
    }
    
    private String getUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(role -> role.startsWith("ROLE_"))
                .map(role -> role.substring(5)) // Remove "ROLE_" prefix
                .findFirst()
                .orElse("GUEST");
        }
        
        return "GUEST";
    }
    
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                       String clientId, String userRole, String endpoint) throws IOException {
        
        logger.warn("Rate limit exceeded for client: {}, role: {}, endpoint: {}", clientId, userRole, endpoint);
        
        Map<String, Object> rateLimitInfo = rateLimitService.getRateLimitInfo(clientId, userRole, endpoint);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate Limit Exceeded");
        errorResponse.put("message", "Too many requests. Please try again later.");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("rateLimitInfo", rateLimitInfo);
        
        // Add retry-after header if available
        if (rateLimitInfo.containsKey("retryAfterMs")) {
            long retryAfterSeconds = ((Number) rateLimitInfo.get("retryAfterMs")).longValue() / 1000;
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfterSeconds)));
        }
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
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
