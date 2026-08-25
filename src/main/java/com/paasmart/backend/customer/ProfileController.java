package com.paasmart.backend.customer;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.common.CloudinaryService;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {

    @Autowired private UserRepository userRepository;
    @Autowired private CloudinaryService cloudinaryService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile() {
        User user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(new ProfileResponse(user));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newEmail = request.getEmail() == null || request.getEmail().isBlank()
                ? null
                : request.getEmail().trim().toLowerCase();

        boolean emailChanged = newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail());
        if (emailChanged && userRepository.existsByEmail(newEmail)) {
            throw new BadRequestExceprion("This email is already in use by another account.");
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);

        User saved = userRepository.save(user);
        return ResponseEntity.ok(new ProfileResponse(saved));
    }

    // Profile photo — alag endpoint isliye kyunki multipart/form-data hai (JSON nahi)
    @PutMapping(value = "/me/photo", consumes = "multipart/form-data")
    public ResponseEntity<ProfileResponse> updateProfilePhoto(@RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestExceprion("Please select an image.");
        }

        Long userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String imageUrl = cloudinaryService.uploadImage(image, "profile_images");
        user.setProfileImage(imageUrl);

        User saved = userRepository.save(user);
        return ResponseEntity.ok(new ProfileResponse(saved));
    }
}