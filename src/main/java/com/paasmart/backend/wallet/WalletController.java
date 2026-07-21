package com.paasmart.backend.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired private WalletService walletService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> balance() {
        return ResponseEntity.ok(Map.of("balance", walletService.getBalance(currentUserId())));
    }

    @GetMapping("/history")
    public ResponseEntity<List<WalletTransaction>> history() {
        return ResponseEntity.ok(walletService.getHistory(currentUserId()));
    }
}