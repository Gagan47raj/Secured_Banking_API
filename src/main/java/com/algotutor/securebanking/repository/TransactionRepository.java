package com.algotutor.securebanking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.algotutor.securebanking.entity.Account;
import com.algotutor.securebanking.entity.Transaction;
import com.algotutor.securebanking.entity.TransactionType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	List<Transaction> findByAccountOrderByCreatedAtDesc(Account account);

	Page<Transaction> findByAccountOrderByCreatedAtDesc(Account account, Pageable pageable);

	List<Transaction> findByAccountAndCreatedAtBetweenOrderByCreatedAtDesc(Account account, LocalDateTime startDate,
			LocalDateTime endDate);

	@Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId ORDER BY t.createdAt DESC")
	List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

	@Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.createdAt >= :fromDate ORDER BY t.createdAt DESC")
	List<Transaction> findRecentTransactionsByAccountId(Long accountId, LocalDateTime fromDate);

	@Query("SELECT t FROM Transaction t WHERE t.account.user.username = :username ORDER BY t.createdAt DESC")
	List<Transaction> findByUserUsernameOrderByCreatedAtDesc(String username);

	@Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId AND t.type = :type")
	long countByAccountIdAndType(Long accountId, TransactionType type);
}
