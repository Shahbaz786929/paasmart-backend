package com.paasmart.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    public EmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            String html = """
                    <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                        <h2>PaaSmart Seller Verification</h2>
                        <p>Your verification code is:</p>

                        <p style="
                            font-size: 28px;
                            font-weight: bold;
                            letter-spacing: 6px;
                        ">
                            %s
                        </p>

                        <p>This code will expire in 10 minutes.</p>

                        <p>
                            If you did not request this, you can safely ignore this email.
                        </p>
                    </div>
                    """.formatted(otp);

            Map<String, Object> payload = Map.of(
                    "from", fromEmail,
                    "to", List.of(toEmail),
                    "subject", "Your PaaSmart Seller verification code",
                    "html", html
            );

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Email provider returned " +
                                response.statusCode() +
                                ": " +
                                response.body()
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to send verification email",
                    e
            );
        }
    }
}