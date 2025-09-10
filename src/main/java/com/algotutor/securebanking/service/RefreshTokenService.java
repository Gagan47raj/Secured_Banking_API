package com.algotutor.securebanking.service;

import java.util.List;
import java.util.Optional;

import com.algotutor.securebanking.entity.RefreshToken;

import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenService {
	RefreshToken createRefreshToken(String username, HttpServletRequest request);

	Optional<RefreshToken> findByToken(String token);

	RefreshToken verifyExpiration(RefreshToken token);

	void deleteByUsername(String username);

	void deleteByToken(String token);

	void deleteExpiredTokens();

	List<RefreshToken> getActiveTokensByUsername(String username);

	void revokeAllUserTokens(String username);

	boolean isTokenValid(String token);
}
