package com.algotutor.securebanking.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.algotutor.securebanking.dto.auth.AuthResponse;
import com.algotutor.securebanking.dto.auth.LoginRequest;
import com.algotutor.securebanking.dto.auth.RegisterRequest;
import com.algotutor.securebanking.entity.AccountType;
import com.algotutor.securebanking.entity.RefreshToken;
import com.algotutor.securebanking.entity.Role;
import com.algotutor.securebanking.entity.User;
import com.algotutor.securebanking.exception.BadRequestException;
import com.algotutor.securebanking.exception.ResourceNotFoundException;
import com.algotutor.securebanking.metrics.BankingMetrics;
import com.algotutor.securebanking.repository.UserRepository;
import com.algotutor.securebanking.security.JwtUtils;
import com.algotutor.securebanking.service.AccountService;
import com.algotutor.securebanking.service.AuthService;
import com.algotutor.securebanking.service.RefreshTokenService;

import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

	private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtils jwtUtils;

	@Autowired
	private AccountService accountService;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private BankingMetrics bankingMetrics;

	@Override
	public AuthResponse registerUser(RegisterRequest registerRequest) {
		logger.info("Registering new user: {}", registerRequest.getUsername());

		// Check if username exists
		if (userRepository.existsByUsername(registerRequest.getUsername())) {
			throw new BadRequestException("Username is already taken!");
		}

		// Check if email exists
		if (userRepository.existsByEmail(registerRequest.getEmail())) {
			throw new BadRequestException("Email is already in use!");
		}

		// Create new user
		User user = new User(registerRequest.getUsername(), registerRequest.getEmail(), registerRequest.getFirstName(),
				registerRequest.getLastName(), passwordEncoder.encode(registerRequest.getPassword()), Role.CUSTOMER // Default
																													// role
		);

		user = userRepository.save(user);

		// Create default savings account for new user
		accountService.createAccount(user, AccountType.SAVINGS);

		// Authenticate the newly registered user
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(registerRequest.getUsername(), registerRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);

		// Update last login time
		user.setLastLogin(LocalDateTime.now());
		userRepository.save(user);

		// Generate tokens
		String accessToken = jwtUtils.generateJwtToken(authentication);
		RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername(),
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest());

		logger.info("User registered successfully: {}", user.getUsername());

		return new AuthResponse(accessToken, refreshToken.getToken(), jwtUtils.getJwtExpirationMs(),
				buildUserInfo(user));
	}

	@Override
	public AuthResponse authenticateUser(LoginRequest loginRequest) {
		Timer.Sample sample = bankingMetrics.startAuthTimer();
		bankingMetrics.incrementAuthAttempts();
		logger.info("Authenticating user: {}", loginRequest.getUsername());

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

			SecurityContextHolder.getContext().setAuthentication(authentication);

			// Update last login time
			User user = (User) authentication.getPrincipal();
			user.setLastLogin(LocalDateTime.now());
			userRepository.save(user);

			// Generate tokens
			String accessToken = jwtUtils.generateJwtToken(authentication);
			RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername(),
					((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest());

			logger.info("User authenticated successfully: {}", user.getUsername());

			bankingMetrics.incrementAuthSuccesses();
			logger.info("User authenticated successfully: {}", user.getUsername());

			return new AuthResponse(accessToken, refreshToken.getToken(), jwtUtils.getJwtExpirationMs(),
					buildUserInfo(user));

		} catch (AuthenticationException e) {
	        bankingMetrics.incrementAuthFailures();

			logger.error("Authentication failed for user: {}", loginRequest.getUsername());
			throw new BadRequestException("Invalid username or password");
		}
		finally {
	        bankingMetrics.stopAuthTimer(sample);
		}
	}

	@Override
	public AuthResponse refreshToken(String refreshTokenStr) {
		Optional<RefreshToken> refreshTokenOpt = refreshTokenService.findByToken(refreshTokenStr);

		if (refreshTokenOpt.isEmpty()) {
			throw new BadRequestException("Invalid refresh token");
		}

		RefreshToken refreshToken = refreshTokenService.verifyExpiration(refreshTokenOpt.get());

		User user = userRepository.findByUsername(refreshToken.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + refreshToken.getUsername()));

		// Generate new tokens
		String newAccessToken = jwtUtils.generateTokenFromUsername(refreshToken.getUsername(),
				jwtUtils.getJwtExpirationMs().intValue());

		RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(refreshToken.getUsername(),
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest());

		// Delete old refresh token
		refreshTokenService.deleteByToken(refreshTokenStr);

		return new AuthResponse(newAccessToken, newRefreshToken.getToken(), jwtUtils.getJwtExpirationMs(),
				buildUserInfo(user));
	}

	@Override
	public void logout(String username) {
		logger.info("User logged out: {}", username);
		refreshTokenService.deleteByUsername(username);
		SecurityContextHolder.clearContext();
	}

	private AuthResponse.UserInfo buildUserInfo(User user) {
		return new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
				user.getLastName(), user.getRole().name());
	}
}
