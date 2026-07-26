package com.paasmart.backend.flashsale;

import com.paasmart.backend.flashsale.dto.ActiveDealResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/deals")
public class DealsController {

    @Autowired private FlashSaleService flashSaleService;

    // Public — koi bhi live deals dekh sakta hai, login zaroori nahi
    @GetMapping("/live")
    public ResponseEntity<List<ActiveDealResponse>> liveDeals() {
        return ResponseEntity.ok(flashSaleService.getAllActiveDeals());
    }
}