package org.example.service;
import org.example.enums.OperationType;

import java.util.UUID;

public interface WalletService {
    void processOperation(UUID walletId, OperationType type, long amount);
    long getBalance(UUID walletId);
}