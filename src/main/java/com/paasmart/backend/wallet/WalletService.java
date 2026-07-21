package com.paasmart.backend.wallet;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;

    public BigDecimal getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getWalletBalance();
    }

    public List<WalletTransaction> getHistory(Long userId) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Wallet me paise credit karna (referral bonus, welcome bonus, refund)
    public void credit(Long userId, BigDecimal amount, WalletTransaction.Type type, String description, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setWalletBalance(user.getWalletBalance().add(amount));
        userRepository.save(user);

        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setAmount(amount);
        txn.setType(type);
        txn.setDescription(description);
        txn.setOrderId(orderId);
        walletTransactionRepository.save(txn);
    }

    // Checkout ke waqt wallet se paise katna
    public void debit(Long userId, BigDecimal amount, String description, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new BadRequestExceprion("Insufficient wallet balance");
        }

        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        userRepository.save(user);

        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setAmount(amount.negate());
        txn.setType(WalletTransaction.Type.ORDER_PAYMENT);
        txn.setDescription(description);
        txn.setOrderId(orderId);
        walletTransactionRepository.save(txn);
    }
}