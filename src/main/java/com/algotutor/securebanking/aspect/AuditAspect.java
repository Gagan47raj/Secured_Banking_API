package com.algotutor.securebanking.aspect;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.algotutor.securebanking.annotation.Auditable;

/**
 * Simple Audit Aspect
 * Logs method executions marked with @Auditable
 */
@Aspect
@Component
public class AuditAspect {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    @AfterReturning("@annotation(auditable)")
    public void logSuccess(JoinPoint joinPoint, Auditable auditable) {
        logAudit(joinPoint, auditable, "SUCCESS", null);
    }
    
    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "exception")
    public void logFailure(JoinPoint joinPoint, Auditable auditable, Exception exception) {
        logAudit(joinPoint, auditable, "FAILURE", exception.getMessage());
    }
    
    private void logAudit(JoinPoint joinPoint, Auditable auditable, String status, String error) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
            
            auditLogger.info("AUDIT: User={}, Action={}, Resource={}, Method={}, Status={}, Error={}", 
                username, 
                auditable.action(), 
                auditable.resource(), 
                joinPoint.getSignature().getName(), 
                status, 
                error != null ? error : "N/A");
                
        } catch (Exception e) {
            // Don't let audit failures break the main functionality
            System.err.println("Audit logging failed: " + e.getMessage());
        }
    }
}