package com.algotutor.securebanking.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algotutor.securebanking.dto.auth.AuthResponse;
import com.algotutor.securebanking.dto.auth.LoginRequest;
import com.algotutor.securebanking.dto.auth.RegisterRequest;
import com.algotutor.securebanking.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestBody; 
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

	@Autowired
	private AuthService authService;

	@PostMapping("/register")
	@Operation(summary = "Register a new user", description = "Create a new user account with default savings account")
	public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
		AuthResponse response = authService.registerUser(registerRequest);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/login")
	@Operation(summary = "User login", description = "Authenticates user and returns JWT tokens")
	public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		AuthResponse response = authService.authenticateUser(loginRequest);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh JWT token", description = "Generates new access token using refresh token")
	public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
		String refreshToken = request.get("refreshToken");
		AuthResponse response = authService.refreshToken(refreshToken);
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logs out the current user")
    public ResponseEntity<Map<String, String>> logoutUser(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "User logged out successfully"));
    }
	
	@PostMapping("/debug")
	public ResponseEntity<Map<String, Object>> debugRequest(@RequestBody String rawJson) {
	    System.out.println("Raw JSON received: " + rawJson);
	    Map<String, Object> response = new HashMap<>();
	    response.put("rawJson", rawJson);
	    return ResponseEntity.ok(response);
	}

	@PostMapping("/debug-object")
	public ResponseEntity<Map<String, Object>> debugObject(@RequestBody RegisterRequest request) {
	    System.out.println("RegisterRequest object received:");
	    System.out.println("Username: " + request.getUsername());
	    System.out.println("Email: " + request.getEmail());
	    System.out.println("FirstName: " + request.getFirstName());
	    System.out.println("LastName: " + request.getLastName());
	    System.out.println("Password: " + (request.getPassword() != null ? "***PROVIDED***" : "NULL"));
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("username", request.getUsername());
	    response.put("email", request.getEmail());
	    response.put("firstName", request.getFirstName());
	    response.put("lastName", request.getLastName());
	    response.put("password", request.getPassword() != null ? "***PROVIDED***" : "NULL");
	    
	    return ResponseEntity.ok(response);
	}
}
