package com.paasmart.backend.notification;

import com.paasmart.backend.notification.dto.WebPushSubscribeRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/web-push")
public class WebPushController {

    @Autowired private WebPushService webPushService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Login se pehle bhi frontend ko chahiye ho sakta hai — public rakha hai
    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> vapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", webPushService.getPublicKey()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody WebPushSubscribeRequest req) {
        webPushService.saveSubscription(currentUserId(), req.getEndpoint(), req.getP256dh(), req.getAuth());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody Map<String, String> body) {
        webPushService.removeSubscription(body.get("endpoint"));
        return ResponseEntity.ok().build();
    }
}