package com.algotutor.securebanking.service.impl;

import org.springframework.stereotype.Service;

import com.algotutor.securebanking.annotation.Auditable;
import com.algotutor.securebanking.dto.banking.AccountDto;
import com.algotutor.securebanking.dto.banking.DepositRequest;
import com.algotutor.securebanking.dto.banking.TransactionDto;
import com.algotutor.securebanking.dto.banking.TransferRequest;
import com.algotutor.securebanking.dto.banking.WithdrawalRequest;
import com.algotutor.securebanking.entity.Account;
import com.algotutor.securebanking.entity.Transaction;
import com.algotutor.securebanking.entity.TransactionType;
import com.algotutor.securebanking.exception.BadRequestException;
import com.algotutor.securebanking.exception.InsufficientFundsException;
import com.algotutor.securebanking.exception.ResourceNotFoundException;
import com.algotutor.securebanking.repository.TransactionRepository;
import com.algotutor.securebanking.service.AccountService;
import com.algotutor.securebanking.service.TransactionService;

import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountService accountService;
    
    @Override
    @Auditable(action = "DEPOSIT", resource = "ACCOUNT")
    public TransactionDto deposit(DepositRequest depositRequest) {
        logger.info("Processing deposit of {} to account {}", 
            depositRequest.getAmount(), depositRequest.getAccountNumber());
        
        // Validate deposit amount
        if (depositRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Deposit amount must be greater than zero");
        }
        
        // Find account
        Account account = accountService.findAccountEntityByNumber(depositRequest.getAccountNumber());
        
        // Create and save transaction
        Transaction transaction = new Transaction(
            depositRequest.getAmount(),
            TransactionType.DEPOSIT,
            depositRequest.getDescription() != null ? depositRequest.getDescription() : "Deposit",
            account
        );
        
        // Update account balance
        account.credit(depositRequest.getAmount());
        
        // Save transaction
        transaction = transactionRepository.save(transaction);
        
        logger.info("Deposit completed successfully. Transaction ID: {}", transaction.getId());
        
        return convertToDto(transaction);
    }
    
    @Override
    @Auditable(action = "WITHDRAWAL", resource = "ACCOUNT")
    public TransactionDto withdraw(WithdrawalRequest withdrawalRequest) {
        logger.info("Processing withdrawal of {} from account {}", 
            withdrawalRequest.getAmount(), withdrawalRequest.getAccountNumber());
        
        // Validate withdrawal amount
        if (withdrawalRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Withdrawal amount must be greater than zero");
        }
        
        // Find account
        Account account = accountService.findAccountEntityByNumber(withdrawalRequest.getAccountNumber());
        
        // Check sufficient balance
        if (account.getBalance().compareTo(withdrawalRequest.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance for withdrawal. Available: " + account.getBalance());
        }
        
        // Create and save transaction
        Transaction transaction = new Transaction(
            withdrawalRequest.getAmount(),
            TransactionType.WITHDRAWAL,
            withdrawalRequest.getDescription() != null ? withdrawalRequest.getDescription() : "Withdrawal",
            account
        );
        
        // Update account balance
        account.debit(withdrawalRequest.getAmount());
        
        // Save transaction
        transaction = transactionRepository.save(transaction);
        
        logger.info("Withdrawal completed successfully. Transaction ID: {}", transaction.getId());
        
        return convertToDto(transaction);
    }
    
    @Override
    @Auditable(action = "TRANSFER", resource = "ACCOUNT")
    public List<TransactionDto> transfer(TransferRequest transferRequest) {
        logger.info("Processing transfer of {} from account {} to account {}", 
            transferRequest.getAmount(), transferRequest.getFromAccountNumber(), transferRequest.getToAccountNumber());
        
        // Validate transfer amount
        if (transferRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transfer amount must be greater than zero");
        }
        
        // Validate different accounts
        if (transferRequest.getFromAccountNumber().equals(transferRequest.getToAccountNumber())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }
        
        // Find accounts
        Account fromAccount = accountService.findAccountEntityByNumber(transferRequest.getFromAccountNumber());
        Account toAccount = accountService.findAccountEntityByNumber(transferRequest.getToAccountNumber());
        
        // Check sufficient balance
        if (fromAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance for transfer. Available: " + fromAccount.getBalance());
        }
        
        // Create transfer out transaction
        Transaction transferOut = new Transaction(
            transferRequest.getAmount(),
            TransactionType.TRANSFER_OUT,
            transferRequest.getDescription() != null ? 
                transferRequest.getDescription() : "Transfer to " + transferRequest.getToAccountNumber(),
            fromAccount
        );
        transferOut.setTargetAccount(toAccount);
        
        // Create transfer in transaction
        Transaction transferIn = new Transaction(
            transferRequest.getAmount(),
            TransactionType.TRANSFER_IN,
            transferRequest.getDescription() != null ? 
                transferRequest.getDescription() : "Transfer from " + transferRequest.getFromAccountNumber(),
            toAccount
        );
        transferIn.setTargetAccount(fromAccount);
        
        // Update balances
        fromAccount.debit(transferRequest.getAmount());
        toAccount.credit(transferRequest.getAmount());
        
        // Save transactions
        transferOut = transactionRepository.save(transferOut);
        transferIn = transactionRepository.save(transferIn);
        
        logger.info("Transfer completed successfully. Transfer Out ID: {}, Transfer In ID: {}", 
            transferOut.getId(), transferIn.getId());
        
        return Arrays.asList(convertToDto(transferOut), convertToDto(transferIn));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getAccountTransactions(String accountNumber) {
        Account account = accountService.findAccountEntityByNumber(accountNumber);
        List<Transaction> transactions = transactionRepository.findByAccountOrderByCreatedAtDesc(account);
        
        return transactions.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getUserTransactions(String username) {
        // Get all user accounts first
        List<AccountDto> userAccounts = accountService.getUserAccounts(username);
        
        return userAccounts.stream()
            .flatMap(account -> getAccountTransactions(account.getAccountNumber()).stream())
            .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt())) // Sort by date descending
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        return convertToDto(transaction);
    }
    
    /**
     * Convert Transaction entity to DTO
     */
    private TransactionDto convertToDto(Transaction transaction) {
        return new TransactionDto(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getType(),
            transaction.getDescription(),
            transaction.getCreatedAt(),
            transaction.getAccount().getAccountNumber(),
            transaction.getTargetAccount() != null ? transaction.getTargetAccount().getAccountNumber() : null
        );
    }
}
