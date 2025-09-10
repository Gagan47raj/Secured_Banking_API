package com.algotutor.securebanking.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;

@Configuration
public class RateLimitConfig {

	// Admin users: 1000 requests per minute
    @Bean("adminBandwidth")
    public Bandwidth adminBandwidth() {
        return Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofMinutes(1)));
    }
    
    // Regular customers: 100 requests per minute
    @Bean("customerBandwidth")
    public Bandwidth customerBandwidth() {
        return Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
    }
    
 // Unauthenticated users: 20 requests per minute
    @Bean("guestBandwidth")
    public Bandwidth guestBandwidth() {
        return Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
    }
    
 // Strict limit for auth endpoints: 10 attempts per minute
    @Bean("authBandwidth")
    public Bandwidth authBandwidth() {
        return Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
    }
    
    // Banking operations: 30 requests per minute (more restrictive)
    @Bean("bankingBandwidth")
    public Bandwidth bankingBandwidth() {
        return Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
    }
}
