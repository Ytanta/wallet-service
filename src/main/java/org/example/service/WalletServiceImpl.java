package org.example.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.enums.OperationType;
import org.example.entity.Transaction;
import org.example.entity.Wallet;
import org.example.exception.InsufficientFundsException;
import org.example.exception.NotFoundException;
import org.example.repository.TransactionRepository;
import org.example.repository.WalletRepository;
import org.example.service.WalletService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final EntityManager entityManager;

    public static final int MAX_RETRIES = 3;

    @Override
    public void processOperation(UUID walletId, OperationType type, long amount) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                updateBalance(walletId, type, amount);
                return;
            } catch (OptimisticLockException e) {
                log.warn("Optimistic lock failed on attempt {} for wallet {}", attempt, walletId);
                if (attempt == MAX_RETRIES) {
                    throw new RuntimeException("Too many concurrent modifications, please try again later.");
                }
            }
        }
    }

    @Transactional
    protected void updateBalance(UUID walletId, OperationType type, long amount) {
        Wallet wallet = entityManager.find(Wallet.class, walletId, LockModeType.OPTIMISTIC);
        if (wallet == null) {
            throw new NotFoundException("Wallet not found: " + walletId);
        }

        if (type == OperationType.WITHDRAW && wallet.getBalance() < amount) {
            throw new InsufficientFundsException("Not enough balance");
        }

        long updatedBalance = (type == OperationType.DEPOSIT)
                ? wallet.getBalance() + amount
                : wallet.getBalance() - amount;

        wallet.setBalance(updatedBalance);
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .operationType(type)
                .amount(amount)
                .build();

        transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public long getBalance(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"))
                .getBalance();
    }
}