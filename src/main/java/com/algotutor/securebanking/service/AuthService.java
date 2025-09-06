package com.algotutor.securebanking.service;

import com.algotutor.securebanking.dto.auth.AuthResponse;
import com.algotutor.securebanking.dto.auth.LoginRequest;
import com.algotutor.securebanking.dto.auth.RegisterRequest;

public interface AuthService {
	AuthResponse registerUser(RegisterRequest registerRequest);

	AuthResponse authenticateUser(LoginRequest loginRequest);

	AuthResponse refreshToken(String refreshToken);

	void logout(String username);
}
