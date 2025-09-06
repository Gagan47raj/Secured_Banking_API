package com.algotutor.securebanking.dto.banking;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.algotutor.securebanking.entity.AccountType;

import lombok.Data;

@Data
public class AccountDto {
    
    private Long id;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private boolean active;
    
    // Constructors
    public AccountDto() {}
    
    public AccountDto(Long id, String accountNumber, AccountType accountType, BigDecimal balance, 
                     LocalDateTime createdAt, boolean active) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.createdAt = createdAt;
        this.active = active;
    }
    
}