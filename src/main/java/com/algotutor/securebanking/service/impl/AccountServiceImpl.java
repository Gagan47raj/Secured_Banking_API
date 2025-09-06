package com.algotutor.securebanking.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algotutor.securebanking.dto.banking.AccountDto;
import com.algotutor.securebanking.entity.Account;
import com.algotutor.securebanking.entity.AccountType;
import com.algotutor.securebanking.entity.User;
import com.algotutor.securebanking.exception.ResourceNotFoundException;
import com.algotutor.securebanking.repository.AccountRepository;
import com.algotutor.securebanking.repository.UserRepository;
import com.algotutor.securebanking.service.AccountService;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {
	
	private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

	@Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public AccountDto createAccount(User user, AccountType accountType) {
        logger.info("Creating new {} account for user: {}", accountType, user.getUsername());
        
        // Generate unique account number
        String accountNumber = generateAccountNumber();
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = generateAccountNumber();
        }
        
        Account account = new Account(accountNumber, accountType, user);
        account = accountRepository.save(account);
        
        logger.info("Account created successfully: {}", accountNumber);
        
        return convertToDto(account);
    }
    
    
    @Override
    @Transactional(readOnly = true)
    public List<AccountDto> getUserAccounts(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        List<Account> accounts = accountRepository.findByUserAndActiveTrue(user);
        
        return accounts.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccountByNumber(String accountNumber) {
        Account account = findAccountEntityByNumber(accountNumber);
        return convertToDto(account);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Account findAccountEntityByNumber(String accountNumber) {
        return accountRepository.findByAccountNumberAndActiveTrue(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        
        return accounts.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder();
        
        // Generate 10-digit number starting with non-zero
        accountNumber.append(random.nextInt(9) + 1); // First digit: 1-9
        for (int i = 1; i < 10; i++) {
            accountNumber.append(random.nextInt(10)); // Remaining digits: 0-9
        }
        
        return accountNumber.toString();
    }
    
    private AccountDto convertToDto(Account account) {
        return new AccountDto(
            account.getId(),
            account.getAccountNumber(),
            account.getAccountType(),
            account.getBalance(),
            account.getCreatedAt(),
            account.isActive()
        );
    }
    
    
}
