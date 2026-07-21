package com.paasmart.backend.tryon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

// Real AI provider — Fashn.ai. Paid hai, jab account aur paise ready hon tab use karo.
@Component
public class FashnAiProvider implements TryOnProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fashn.ai.api-key:}")
    private String apiKey;

    private static final String BASE_URL = "https://api.fashn.ai/v1";

    @Override
    public String startJob(String userPhotoUrl, String clothImageUrl, String category) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model_image", userPhotoUrl,
                "garment_image", clothImageUrl,
                "category", category.toLowerCase()
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<?, ?> response = restTemplate.postForObject(BASE_URL + "/run", request, Map.class);
        if (response == null || response.get("id") == null) {
            throw new RuntimeException("Fashn.ai did not return a prediction id");
        }
        return response.get("id").toString();
    }

    @Override
    public Map<String, Object> checkJobStatus(String jobId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        Map<?, ?> response = restTemplate.exchange(
                BASE_URL + "/status/" + jobId,
                HttpMethod.GET,
                request,
                Map.class
        ).getBody();

        String state = String.valueOf(response.get("status"));
        String resultUrl = null;

        if ("completed".equalsIgnoreCase(state)) {
            List<?> output = (List<?>) response.get("output");
            resultUrl = (output != null && !output.isEmpty()) ? output.get(0).toString() : null;
        }

        return Map.of(
                "status", state,
                "resultUrl", resultUrl == null ? "" : resultUrl
        );
    }
}