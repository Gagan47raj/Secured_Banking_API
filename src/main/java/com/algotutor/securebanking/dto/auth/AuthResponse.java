package com.algotutor.securebanking.dto.auth;

import lombok.Data;

@Data
public class AuthResponse {

	private String accessToken;
	private String refreshToken;
	private String tokenType = "Bearer";
	private Long expiresIn;
	private UserInfo user;
	
public AuthResponse() {}
    
    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }
    
    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        
        // Constructors
        public UserInfo() {}
        
        public UserInfo(Long id, String username, String email, String firstName, String lastName, String role) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.role = role;
        }
    }
}
