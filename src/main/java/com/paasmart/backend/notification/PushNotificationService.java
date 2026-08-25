package com.paasmart.backend.notification;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public void send(String pushToken, String title, String body) {
        if (pushToken == null || pushToken.isBlank()) return;

        try {
            Map<String, Object> payload = Map.of(
                    "to", pushToken,
                    "title", title,
                    "body", body,
                    "sound", "default"
            );
            restTemplate.postForObject("https://exp.host/--/api/v2/push/send", payload, String.class);
        } catch (Exception e) {
            log.warn("Push notification failed", e);
        }
    }
}
