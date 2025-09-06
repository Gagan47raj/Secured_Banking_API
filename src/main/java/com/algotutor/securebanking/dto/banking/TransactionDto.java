package com.algotutor.securebanking.dto.banking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.algotutor.securebanking.entity.TransactionType;

import lombok.Data;

@Data
public class TransactionDto {
    
    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private LocalDateTime createdAt;
    private String accountNumber;
    private String targetAccountNumber;
    
    // Constructors
    public TransactionDto() {}
    
    public TransactionDto(Long id, BigDecimal amount, TransactionType type, String description, 
                         LocalDateTime createdAt, String accountNumber, String targetAccountNumber) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
        this.accountNumber = accountNumber;
        this.targetAccountNumber = targetAccountNumber;
    }
    
}