//package com.paasmart.backend.tryon;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Map;
//
//@Component
//public class FashnAiClient {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @Value("${fashn.ai.api-key:}")
//    private String apiKey;
//
//    private static final String BASE_URL = "https://api.fashn.ai/v1";
//
//    // Step 1: try-on job start karo, prediction_id return hoga
//    public String startTryOn(String userPhotoUrl, String clothImageUrl, String category) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + apiKey);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        Map<String, Object> body = Map.of(
//                "model_image", userPhotoUrl,
//                "garment_image", clothImageUrl,
//                "category", category.toLowerCase()   // "tops", "bottoms", "one-pieces"
//        );
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
//
//        Map<?, ?> response = restTemplate.postForObject(BASE_URL + "/run", request, Map.class);
//        if (response == null || response.get("id") == null) {
//            throw new RuntimeException("Fashn.ai did not return a prediction id");
//        }
//        return response.get("id").toString();
//    }
//
//    // Step 2: status poll karo (frontend ise har 2-3 second me call karega)
//    public Map<String, Object> checkStatus(String predictionId) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + apiKey);
//
//        HttpEntity<Void> request = new HttpEntity<>(headers);
//
//        return restTemplate.exchange(
//                BASE_URL + "/status/" + predictionId,
//                HttpMethod.GET,
//                request,
//                Map.class
//        ).getBody();
//    }
//}