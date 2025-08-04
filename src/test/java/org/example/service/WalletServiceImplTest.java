package org.example.service;

import org.example.entity.Wallet;
import org.example.enums.OperationType;
import org.example.exception.InsufficientFundsException;
import org.example.exception.NotFoundException;
import org.example.repository.TransactionRepository;
import org.example.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        walletId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .balance(1000)
                .build();
    }

    @Test
    void getBalance_whenWalletExists_shouldReturnBalance() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        long balance = walletService.getBalance(walletId);
        assertEquals(1000, balance);
    }

    @Test
    void getBalance_whenWalletNotFound_shouldThrow() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> walletService.getBalance(walletId));
    }

    @Test
    void processOperation_deposit_shouldIncreaseBalance() {
        when(entityManager.find(Wallet.class, walletId, LockModeType.OPTIMISTIC)).thenReturn(wallet);
        doAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            assertEquals(1500, w.getBalance());
            return null;
        }).when(walletRepository).save(any());

        walletService.processOperation(walletId, OperationType.DEPOSIT, 500);

        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void processOperation_withdraw_insufficientFunds_shouldThrow() {
        when(entityManager.find(Wallet.class, walletId, LockModeType.OPTIMISTIC)).thenReturn(wallet);

        assertThrows(InsufficientFundsException.class,
                () -> walletService.processOperation(walletId, OperationType.WITHDRAW, 1500));
    }

    @Test
    void processOperation_optimisticLockException_shouldRetry() {
        when(entityManager.find(Wallet.class, walletId, LockModeType.OPTIMISTIC)).thenReturn(wallet);
        doThrow(new OptimisticLockException()).when(walletRepository).save(any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> walletService.processOperation(walletId, OperationType.DEPOSIT, 100));

        assertTrue(ex.getMessage().contains("Too many concurrent modifications"));
        verify(walletRepository, times(WalletServiceImpl.MAX_RETRIES)).save(any());
    }
}