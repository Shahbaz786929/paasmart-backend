package com.paasmart.backend.notification;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PushNotificationService pushNotificationService;
    @Autowired private WebPushService webPushService;

    // Backend me kahin se bhi kisi bhi role ke user ko notify karna ho — isi ek method se hoga.
    // DB me save (in-app list ke liye) + push (agar pushToken hai) — dono yahin se.
    public void notify(Long userId, Notification.Type type, String title, String message, Long referenceId) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setReferenceId(referenceId);
            notificationRepository.save(notification);

            Map<String, String> data = new HashMap<>();
            data.put("type", type.name());
            if (referenceId != null) data.put("referenceId", String.valueOf(referenceId));

            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getPushToken() != null) {
                pushNotificationService.send(user.getPushToken(), title, message);
            }
            webPushService.sendToUser(userId, title, message, data);
        } catch (Exception e) {
            log.warn("Failed to create notification for user {}", userId, e);
        }
    }

    public List<Notification> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public Notification markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedException("This notification doesn't belong to you.");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }
}