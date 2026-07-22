package com.paasmart.backend.returns;

import com.paasmart.backend.returns.dto.ResolveReturnDto;
import com.paasmart.backend.returns.dto.ReturnRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ReturnRequestController {

    @Autowired private ReturnRequestService returnRequestService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Customer return request banata hai (photos optional)
    @PostMapping(value = "/api/returns", consumes = "multipart/form-data")
    public ResponseEntity<ReturnRequest> createReturnRequest(
            @RequestParam Long orderItemId,
            @RequestParam String reason,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<MultipartFile> photos) {

        ReturnRequestDto dto = new ReturnRequestDto();
        dto.setOrderItemId(orderItemId);
        dto.setReason(reason);
        dto.setDescription(description);

        return ResponseEntity.ok(returnRequestService.createReturnRequest(currentUserId(), dto, photos));
    }

    @GetMapping("/api/returns/my")
    public ResponseEntity<List<ReturnRequest>> myReturnRequests() {
        return ResponseEntity.ok(returnRequestService.getMyReturnRequests(currentUserId()));
    }

    @GetMapping("/api/returns/order/{orderId}")
    public ResponseEntity<List<ReturnRequest>> returnsForOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(returnRequestService.getReturnRequestsForOrder(currentUserId(), orderId));
    }

    // Seller approve/reject karta hai
    @PutMapping("/api/v1/seller/returns/{returnRequestId}/resolve")
    public ResponseEntity<ReturnRequest> resolveReturn(
            @PathVariable Long returnRequestId,
            @RequestBody ResolveReturnDto dto) {
        return ResponseEntity.ok(returnRequestService.resolveReturnRequest(currentUserId(), returnRequestId, dto));
    }
}