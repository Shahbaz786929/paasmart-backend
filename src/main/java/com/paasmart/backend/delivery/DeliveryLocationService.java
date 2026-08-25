package com.paasmart.backend.delivery;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DeliveryLocationService {

    @Autowired private UserRepository userRepository;

    // Delivery boy app har 8-10 second me ye call karega jab tak koi active delivery ho
    public void updateLocation(Long deliveryBoyId, Double lat, Double lng) {
        User user = userRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setCurrentLat(lat);
        user.setCurrentLng(lng);
        user.setLocationUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}