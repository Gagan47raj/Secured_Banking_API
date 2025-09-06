package com.algotutor.securebanking.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algotutor.securebanking.dto.banking.AccountDto;
import com.algotutor.securebanking.dto.banking.DepositRequest;
import com.algotutor.securebanking.dto.banking.TransactionDto;
import com.algotutor.securebanking.dto.banking.TransferRequest;
import com.algotutor.securebanking.dto.banking.WithdrawalRequest;
import com.algotutor.securebanking.service.AccountService;
import com.algotutor.securebanking.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer Operations", description = "Banking operations for customers")
public class CustomerController {

	@Autowired
	private AccountService accountService;

	@Autowired
	private TransactionService transactionService;

	@GetMapping("/accounts")
	@Operation(summary = "Get customer accounts", description = "Retrieves all accounts belonging to the authenticated customer")
	public ResponseEntity<List<AccountDto>> getMyAccounts(Authentication authentication) {
		List<AccountDto> accounts = accountService.getUserAccounts(authentication.getName());
		return ResponseEntity.ok(accounts);
	}

	@GetMapping("/accounts/{accountNumber}")
	@Operation(summary = "Get account details", description = "Retrieves details of a specific account (must belong to the customer)")
	public ResponseEntity<AccountDto> getAccountDetails(@PathVariable String accountNumber,
			Authentication authentication) {

		// Verify account belongs to the authenticated user
		List<AccountDto> userAccounts = accountService.getUserAccounts(authentication.getName());
		boolean accountBelongsToUser = userAccounts.stream()
				.anyMatch(account -> account.getAccountNumber().equals(accountNumber));

		if (!accountBelongsToUser) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		AccountDto account = accountService.getAccountByNumber(accountNumber);
		return ResponseEntity.ok(account);
	}

	@GetMapping("/accounts/{accountNumber}/transactions")
	@Operation(summary = "Get account transactions", description = "Retrieves transaction history for a specific account")
	public ResponseEntity<List<TransactionDto>> getAccountTransactions(@PathVariable String accountNumber,
			Authentication authentication) {

		// Verify account belongs to the authenticated user
		List<AccountDto> userAccounts = accountService.getUserAccounts(authentication.getName());
		boolean accountBelongsToUser = userAccounts.stream()
				.anyMatch(account -> account.getAccountNumber().equals(accountNumber));

		if (!accountBelongsToUser) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		List<TransactionDto> transactions = transactionService.getAccountTransactions(accountNumber);
		return ResponseEntity.ok(transactions);
	}

	@GetMapping("/transactions")
	@Operation(summary = "Get all customer transactions", description = "Retrieves all transactions for the authenticated customer")
	public ResponseEntity<List<TransactionDto>> getMyTransactions(Authentication authentication) {
		List<TransactionDto> transactions = transactionService.getUserTransactions(authentication.getName());
		return ResponseEntity.ok(transactions);
	}
	
	@PostMapping("/accounts/deposit")
	@Operation(summary = "Deposit money", description = "Deposits money into customer's account")
	public ResponseEntity<TransactionDto> deposit(
	        @Valid @RequestBody DepositRequest depositRequest,
	        Authentication authentication) {
	    
	    // Verify account belongs to the authenticated user
	    List<AccountDto> userAccounts = accountService.getUserAccounts(authentication.getName());
	    boolean accountBelongsToUser = userAccounts.stream()
	        .anyMatch(account -> account.getAccountNumber().equals(depositRequest.getAccountNumber()));
	    
	    if (!accountBelongsToUser) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	    }
	    
	    TransactionDto transaction = transactionService.deposit(depositRequest);
	    return ResponseEntity.ok(transaction);
	}
	
	@PostMapping("/accounts/withdraw")
	@Operation(summary = "Withdraw money", description = "Withdraws money from customer's account")
	public ResponseEntity<TransactionDto> withdraw(
	        @Valid @RequestBody WithdrawalRequest withdrawalRequest,
	        Authentication authentication) {
	    
	    // Verify account belongs to the authenticated user
	    List<AccountDto> userAccounts = accountService.getUserAccounts(authentication.getName());
	    boolean accountBelongsToUser = userAccounts.stream()
	        .anyMatch(account -> account.getAccountNumber().equals(withdrawalRequest.getAccountNumber()));
	    
	    if (!accountBelongsToUser) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	    }
	    
	    TransactionDto transaction = transactionService.withdraw(withdrawalRequest);
	    return ResponseEntity.ok(transaction);
	}
	
	@PostMapping("/accounts/transfer")
	@Operation(summary = "Transfer money", description = "Transfers money between accounts")
	public ResponseEntity<List<TransactionDto>> transfer(
	        @Valid @RequestBody TransferRequest transferRequest,
	        Authentication authentication) {
	    
	    // Verify from account belongs to the authenticated user
	    List<AccountDto> userAccounts = accountService.getUserAccounts(authentication.getName());
	    boolean fromAccountBelongsToUser = userAccounts.stream()
	        .anyMatch(account -> account.getAccountNumber().equals(transferRequest.getFromAccountNumber()));
	    
	    if (!fromAccountBelongsToUser) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	    }
	    
	    List<TransactionDto> transactions = transactionService.transfer(transferRequest);
	    return ResponseEntity.ok(transactions);
	}
	
	@GetMapping("/transactions/{transactionId}")
	@Operation(summary = "Get transaction details", description = "Retrieves details of a specific transaction")
	public ResponseEntity<TransactionDto> getTransaction(
	        @PathVariable Long transactionId,
	        Authentication authentication) {
	    
	    TransactionDto transaction = transactionService.getTransactionById(transactionId);
	    
	    // Verify transaction belongs to the authenticated user
	    List<AccountDto> userAccounts = accountService.getUserAccounts(authentication.getName());
	    boolean transactionBelongsToUser = userAccounts.stream()
	        .anyMatch(account -> account.getAccountNumber().equals(transaction.getAccountNumber()));
	    
	    if (!transactionBelongsToUser) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	    }
	    
	    return ResponseEntity.ok(transaction);
	}


}
