package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class WalletResponse {
    private UUID walletId;
    private long balance;
}