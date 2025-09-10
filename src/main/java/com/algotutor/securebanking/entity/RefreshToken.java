package com.algotutor.securebanking.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefreshToken {
	private String token;
	private String username;
	private Long userId;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime expiryDate;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime createdAt;

	private String ipAddress;
	private String userAgent;

	public RefreshToken(String token, String username, Long userId, LocalDateTime expiryDate, String ipAddress,
			String userAgent) {
		this.token = token;
		this.username = username;
		this.userId = userId;
		this.expiryDate = expiryDate;
		this.createdAt = LocalDateTime.now();
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
	}

// Helper method to check if token is expired
	@JsonIgnore
	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiryDate);
	}

}
