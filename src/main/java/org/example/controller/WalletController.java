package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.WalletRequest;
import org.example.dto.WalletResponse;
import org.example.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/wallet")
    public ResponseEntity<Void> operate(@RequestBody @Valid WalletRequest request) {
        walletService.processOperation(
                request.getWalletId(),
                request.getOperationType(),
                request.getAmount()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wallets/{id}")
    public ResponseEntity<WalletResponse> getBalance(@PathVariable UUID id) {
        long balance = walletService.getBalance(id);
        return ResponseEntity.ok(new WalletResponse(id, balance));
    }
}