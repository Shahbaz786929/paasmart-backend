//package com.paasmart.backend.tryon;
//
//import com.paasmart.backend.common.CloudinaryService;
//import com.paasmart.backend.exception.BadRequestExceprion;
//import com.paasmart.backend.exception.ResourceNotFoundException;
//import com.paasmart.backend.exception.UnauthorizedException;
//import com.paasmart.backend.product.Product;
//import com.paasmart.backend.product.ProductRepository;
//import com.paasmart.backend.tryon.dto.TryOnResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class TryOnService {
//
//    @Autowired private FashnAiClient fashnAiClient;
//    @Autowired private ProductRepository productRepository;
//    @Autowired private TryOnHistoryRepository tryOnHistoryRepository;
//    @Autowired private CloudinaryService cloudinaryService;
//
//    public TryOnResponse startTryOn(Long customerId, Long productId, MultipartFile photo) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
//
//        if (!Boolean.TRUE.equals(product.getTryOnEnabled())) {
//            throw new BadRequestExceprion("Virtual try-on is not available for this product");
//        }
//        if (product.getImages() == null || product.getImages().isBlank()) {
//            throw new BadRequestExceprion("This product has no image to try on");
//        }
//        if (photo == null || photo.isEmpty()) {
//            throw new BadRequestExceprion("Please select a photo to try on this outfit");
//        }
//        if (photo.getSize() > 8 * 1024 * 1024) {   // 8MB limit
//            throw new BadRequestExceprion("Photo is too large. Please choose a photo under 8MB");
//        }
//
//        // 1) User ki photo backend khud Cloudinary pe upload karega
//        String userPhotoUrl = cloudinaryService.uploadImage(photo, "tryon_user_photos");
//
//        String clothImageUrl = product.getImages().split(",")[0].trim();
//        String category = mapCategory(product.getCategory());
//
//        TryOnHistory history = new TryOnHistory();
//        history.setCustomerId(customerId);
//        history.setProductId(product.getId());
//        history.setStatus(TryOnHistory.Status.PROCESSING);
//        history = tryOnHistoryRepository.save(history);
//
//        try {
//            String predictionId = fashnAiClient.startTryOn(userPhotoUrl, clothImageUrl, category);
//            history.setResultImageUrl(predictionId);
//            tryOnHistoryRepository.save(history);
//        } catch (Exception e) {
//            history.setStatus(TryOnHistory.Status.FAILED);
//            tryOnHistoryRepository.save(history);
//            throw new RuntimeException("Try-on failed to start: " + e.getMessage());
//        }
//
//        return new TryOnResponse(history.getId(), history.getStatus().name(), null);
//    }
//
//    public TryOnResponse checkStatus(Long customerId, Long tryOnId) {
//        TryOnHistory history = tryOnHistoryRepository.findById(tryOnId)
//                .orElseThrow(() -> new ResourceNotFoundException("Try-on request not found"));
//
//        if (!history.getCustomerId().equals(customerId)) {
//            throw new UnauthorizedException("This is not your try-on request");
//        }
//
//        if (history.getStatus() != TryOnHistory.Status.PROCESSING) {
//            return new TryOnResponse(history.getId(), history.getStatus().name(),
//                    history.getStatus() == TryOnHistory.Status.COMPLETED ? history.getResultImageUrl() : null);
//        }
//
//        String predictionId = history.getResultImageUrl();
//        Map<String, Object> status = fashnAiClient.checkStatus(predictionId);
//        String state = String.valueOf(status.get("status"));
//
//        if ("completed".equalsIgnoreCase(state)) {
//            List<?> output = (List<?>) status.get("output");
//            String resultUrl = (output != null && !output.isEmpty()) ? output.get(0).toString() : null;
//
//            history.setStatus(TryOnHistory.Status.COMPLETED);
//            history.setResultImageUrl(resultUrl);
//            tryOnHistoryRepository.save(history);
//
//            return new TryOnResponse(history.getId(), "COMPLETED", resultUrl);
//        }
//
//        if ("failed".equalsIgnoreCase(state)) {
//            history.setStatus(TryOnHistory.Status.FAILED);
//            tryOnHistoryRepository.save(history);
//            return new TryOnResponse(history.getId(), "FAILED", null);
//        }
//
//        return new TryOnResponse(history.getId(), "PROCESSING", null);
//    }
//
//    public List<TryOnHistory> getMyTryOns(Long customerId) {
//        return tryOnHistoryRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
//    }
//
//    private String mapCategory(String productCategory) {
//        if (productCategory == null) return "tops";
//        String c = productCategory.toLowerCase();
//        if (c.contains("pant") || c.contains("bottom") || c.contains("jean")) return "bottoms";
//        if (c.contains("dress") || c.contains("saree") || c.contains("gown")) return "one-pieces";
//        return "tops";
//    }
//}

package com.paasmart.backend.tryon;

import com.paasmart.backend.common.CloudinaryService;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.tryon.dto.TryOnResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class TryOnService {

    @Autowired private TryOnProvider tryOnProvider;   // Spring khud decide karega Mock ya Fashn use karna hai
    @Autowired private ProductRepository productRepository;
    @Autowired private TryOnHistoryRepository tryOnHistoryRepository;
    @Autowired private CloudinaryService cloudinaryService;

    public TryOnResponse startTryOn(Long customerId, Long productId, MultipartFile photo) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(product.getTryOnEnabled())) {
            throw new BadRequestExceprion("Virtual try-on is not available for this product");
        }
        if (product.getImages() == null || product.getImages().isBlank()) {
            throw new BadRequestExceprion("This product has no image to try on");
        }
        if (photo == null || photo.isEmpty()) {
            throw new BadRequestExceprion("Please select a photo to try on this outfit");
        }
        if (photo.getSize() > 8 * 1024 * 1024) {
            throw new BadRequestExceprion("Photo is too large. Please choose a photo under 8MB");
        }

        String userPhotoUrl = cloudinaryService.uploadImage(photo, "tryon_user_photos");
        String clothImageUrl = product.getImages().split(",")[0].trim();
        String category = mapCategory(product.getCategory());

        TryOnHistory history = new TryOnHistory();
        history.setCustomerId(customerId);
        history.setProductId(product.getId());
        history.setStatus(TryOnHistory.Status.PROCESSING);
        history = tryOnHistoryRepository.save(history);

        try {
            String jobId = tryOnProvider.startJob(userPhotoUrl, clothImageUrl, category);
            history.setResultImageUrl(jobId);   // jobId ko temporarily yahan store karte hain, poll hone tak
            tryOnHistoryRepository.save(history);
        } catch (Exception e) {
            history.setStatus(TryOnHistory.Status.FAILED);
            tryOnHistoryRepository.save(history);
            throw new RuntimeException("Try-on failed to start: " + e.getMessage());
        }

        return new TryOnResponse(history.getId(), history.getStatus().name(), null);
    }

    public TryOnResponse checkStatus(Long customerId, Long tryOnId) {
        TryOnHistory history = tryOnHistoryRepository.findById(tryOnId)
                .orElseThrow(() -> new ResourceNotFoundException("Try-on request not found"));

        if (!history.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("This is not your try-on request");
        }

        if (history.getStatus() != TryOnHistory.Status.PROCESSING) {
            return new TryOnResponse(history.getId(), history.getStatus().name(),
                    history.getStatus() == TryOnHistory.Status.COMPLETED ? history.getResultImageUrl() : null);
        }

        String jobId = history.getResultImageUrl();
        Map<String, Object> status = tryOnProvider.checkJobStatus(jobId);
        String state = String.valueOf(status.get("status"));

        if ("completed".equalsIgnoreCase(state)) {
            String resultUrl = String.valueOf(status.get("resultUrl"));

            history.setStatus(TryOnHistory.Status.COMPLETED);
            history.setResultImageUrl(resultUrl);
            tryOnHistoryRepository.save(history);

            return new TryOnResponse(history.getId(), "COMPLETED", resultUrl);
        }

        if ("failed".equalsIgnoreCase(state)) {
            history.setStatus(TryOnHistory.Status.FAILED);
            tryOnHistoryRepository.save(history);
            return new TryOnResponse(history.getId(), "FAILED", null);
        }

        return new TryOnResponse(history.getId(), "PROCESSING", null);
    }

    public List<TryOnHistory> getMyTryOns(Long customerId) {
        return tryOnHistoryRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    private String mapCategory(String productCategory) {
        if (productCategory == null) return "tops";
        String c = productCategory.toLowerCase();
        if (c.contains("pant") || c.contains("bottom") || c.contains("jean")) return "bottoms";
        if (c.contains("dress") || c.contains("saree") || c.contains("gown")) return "one-pieces";
        return "tops";
    }
}