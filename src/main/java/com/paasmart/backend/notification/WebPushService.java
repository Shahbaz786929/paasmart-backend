package com.paasmart.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${vapid.public-key}") private String vapidPublicKey;
    @Value("${vapid.private-key}") private String vapidPrivateKey;
    @Value("${vapid.subject}") private String vapidSubject;

    @Autowired private WebPushSubscriptionRepository subscriptionRepository;

    private PushService pushService;

    @PostConstruct
    public void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        if (vapidPublicKey != null && !vapidPublicKey.isBlank()) {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        } else {
            log.warn("VAPID keys not configured — web push notifications are disabled.");
        }
    }

    public String getPublicKey() {
        return vapidPublicKey;
    }

    public void saveSubscription(Long userId, String endpoint, String p256dh, String auth) {
        WebPushSubscription sub = subscriptionRepository.findByEndpoint(endpoint).orElseGet(WebPushSubscription::new);
        sub.setUserId(userId);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        subscriptionRepository.save(sub);
    }

    public void removeSubscription(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
    }

    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        if (pushService == null) return;

        List<WebPushSubscription> subs = subscriptionRepository.findByUserId(userId);
        if (subs.isEmpty()) return;

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("body", body);
            if (data != null) payload.put("data", data);
            String payloadJson = objectMapper.writeValueAsString(payload);

            for (WebPushSubscription sub : subs) {
                try {
                    Subscription subscription = new Subscription(
                            sub.getEndpoint(),
                            new Subscription.Keys(sub.getP256dh(), sub.getAuth())
                    );
                    nl.martijndwars.webpush.Notification notification =
                            new nl.martijndwars.webpush.Notification(subscription, payloadJson);

                    var response = pushService.send(notification);
                    int status = response.getStatusLine().getStatusCode();
                    if (status == 404 || status == 410) {
                        // Subscription expire ho chuki hai (user ne notification permission hataayi ya browser data clear kiya)
                        subscriptionRepository.delete(sub);
                    }
                } catch (Exception e) {
                    log.warn("Web push failed for subscription {}", sub.getId(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Web push payload build failed for user {}", userId, e);
        }
    }
}