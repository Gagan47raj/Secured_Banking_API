package com.algotutor.securebanking.service;

import java.util.List;

import com.algotutor.securebanking.dto.banking.AccountDto;
import com.algotutor.securebanking.entity.Account;
import com.algotutor.securebanking.entity.AccountType;
import com.algotutor.securebanking.entity.User;

public interface AccountService {
	AccountDto createAccount(User user, AccountType accountType);

	List<AccountDto> getUserAccounts(String username);

	AccountDto getAccountByNumber(String accountNumber);

	Account findAccountEntityByNumber(String accountNumber);

	List<AccountDto> getAllAccounts(); // Admin only
}
