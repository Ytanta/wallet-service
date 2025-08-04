package org.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.example.enums.OperationType;

import java.util.UUID;

@Data
public class WalletRequest {

    @NotNull
    private UUID walletId;

    @NotNull
    private OperationType operationType;

    @Positive
    private long amount;
}