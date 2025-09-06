package com.algotutor.securebanking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.algotutor.securebanking.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	Boolean existsByUsername(String username);

	Boolean existsByEmail(String email);

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.accounts WHERE u.username = :username")
	Optional<User> findByUsernameWithAccounts(String username);
}
