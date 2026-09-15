package com.paasmart.backend.wallet;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.notification.Notification;
import com.paasmart.backend.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private NotificationService notificationService;

    public BigDecimal getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getWalletBalance();
    }

    public List<WalletTransaction> getHistory(Long userId) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Credit money to wallet (referral bonus, welcome bonus, refund).
    // @Transactional + row-lock so two simultaneous credits/debits on the same
    // user can't overwrite each other's balance update (lost-update race fix).
    @Transactional
    public void credit(Long userId, BigDecimal amount, WalletTransaction.Type type, String description, Long orderId) {
        User user = userRepository.findByIdForUpdate(userId)
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
        notificationService.notify(userId, Notification.Type.WALLET, "Wallet Credited", description, orderId);
    }

    // Debit money from wallet at checkout time.
    @Transactional
    public void debit(Long userId, BigDecimal amount, String description, Long orderId) {
        User user = userRepository.findByIdForUpdate(userId)
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