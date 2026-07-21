package com.paasmart.backend.tryon;

import com.paasmart.backend.tryon.dto.TryOnResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tryon")
public class TryOnController {

    @Autowired private TryOnService tryOnService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // User apni photo camera/gallery se seedha yahan bhejega — multipart/form-data
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<TryOnResponse> startTryOn(
            @RequestParam("productId") Long productId,
            @RequestParam("photo") MultipartFile photo) {
        return ResponseEntity.ok(tryOnService.startTryOn(currentUserId(), productId, photo));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<TryOnResponse> checkStatus(@PathVariable Long id) {
        return ResponseEntity.ok(tryOnService.checkStatus(currentUserId(), id));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TryOnHistory>> myHistory() {
        return ResponseEntity.ok(tryOnService.getMyTryOns(currentUserId()));
    }
}