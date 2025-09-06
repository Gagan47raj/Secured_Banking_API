package com.algotutor.securebanking.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.algotutor.securebanking.entity.User;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {

	private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

	@Value("${app.jwtSecret:mySecretKey}")
	private String jwtSecret;

	@Value("${app.jwtExpirationMs:86400000}") // 24 hours
	private int jwtExpirationMs;

	@Value("${app.jwtRefreshExpirationMs:604800000}") // 7 days
	private int jwtRefreshExpirationMs;

	public String generateJwtToken(Authentication authentication) {
		User userPrincipal = (User) authentication.getPrincipal();
		return generateTokenFromUsername(userPrincipal.getUsername(), jwtExpirationMs);
	}

	public String generateRefreshToken(String username) {
		return generateTokenFromUsername(username, jwtRefreshExpirationMs);
	}

	public String generateTokenFromUsername(String username, int expirationMs) {
		Date expiryDate = new Date((new Date()).getTime() + expirationMs);

		return Jwts.builder().subject(username).issuedAt(new Date()).expiration(expiryDate).signWith(getSignKey())
				.compact();
	}

	public String getUserNameFromJwtToken(String token) {
		return Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload().getSubject();
	}

	private SecretKey getSignKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public boolean validateJwtToken(String authToken) {
		try {
			Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(authToken);
			return true;
		} catch (SecurityException e) {
			logger.error("Invalid JWT signature: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			logger.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			logger.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			logger.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("JWT claims string is empty: {}", e.getMessage());
		}

		return false;
	}

	public Long getJwtExpirationMs() {
		return (long) jwtExpirationMs;
	}

}
