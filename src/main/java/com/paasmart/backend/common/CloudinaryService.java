package com.paasmart.backend.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.paasmart.backend.exception.BadRequestExceprion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Service
public class CloudinaryService {

    // Only these content types are accepted for ANY image upload in the app
    // (product photos, profile pictures, review photos, return photos, try-on
    // photos, and the generic /uploads/image endpoint) — enforced here once so
    // every caller gets it automatically instead of duplicating the check.
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary getCloudinary() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
        }
        return cloudinary;
    }

    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestExceprion("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestExceprion("Only JPEG, PNG, WEBP or GIF images are allowed");
        }
        try {
            Map<?, ?> result = getCloudinary().uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", folder)
            );
            return result.get("secure_url").toString();
        } catch (Exception e) {
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
    }
}