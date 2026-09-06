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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.sender-email:}")
    private String senderEmail;

    @Value("${brevo.sender-name:PaasMart}")
    private String senderName;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            String html = """
                    <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                        <h2>PaasMart Verification</h2>
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
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", "Your PaasMart verification code",
                    "htmlContent", html
            );

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey)
                    .header("Content-Type", "application/json")
                    .header("accept", "application/json")
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