package com.algotutor.securebanking.service;

import java.util.List;

import com.algotutor.securebanking.dto.banking.DepositRequest;
import com.algotutor.securebanking.dto.banking.TransactionDto;
import com.algotutor.securebanking.dto.banking.TransferRequest;
import com.algotutor.securebanking.dto.banking.WithdrawalRequest;

public interface TransactionService {
	TransactionDto deposit(DepositRequest depositRequest);

	TransactionDto withdraw(WithdrawalRequest withdrawalRequest);

	List<TransactionDto> transfer(TransferRequest transferRequest);

	List<TransactionDto> getAccountTransactions(String accountNumber);

	List<TransactionDto> getUserTransactions(String username);

	TransactionDto getTransactionById(Long transactionId);
}
