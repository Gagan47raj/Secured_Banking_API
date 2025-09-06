package com.algotutor.securebanking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.algotutor.securebanking.entity.Account;
import com.algotutor.securebanking.entity.User;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

	List<Account> findByUserAndActiveTrue(User user);

	Optional<Account> findByAccountNumber(String accountNumber);

	Optional<Account> findByAccountNumberAndActiveTrue(String accountNumber);

	@Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.active = true")
	List<Account> findActiveAccountsByUserId(Long userId);

	@Query("SELECT a FROM Account a LEFT JOIN FETCH a.transactions WHERE a.accountNumber = :accountNumber")
	Optional<Account> findByAccountNumberWithTransactions(String accountNumber);

	Boolean existsByAccountNumber(String accountNumber);
}
