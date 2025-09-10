package com.algotutor.securebanking.service;

import java.util.Map;

public interface RateLimitService {

	boolean isAllowed(String key, String userRole, String endpoint);

	long getAvailableTokens(String key, String userRole, String endpoint);

	void resetBucket(String key);

	Map<String, Object> getRateLimitInfo(String key, String userRole, String endpoint);

	void cleanupExpiredBuckets();
}
