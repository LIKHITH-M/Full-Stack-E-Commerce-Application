package com.likhith.ecomproj.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Email service using Resend HTTP API.
 * 
 * Render free tier blocks all SMTP ports (25, 465, 587).
 * Resend uses HTTPS (port 443) which is not blocked.
 */
@Service
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${RESEND_API_KEY:}")
    private String apiKey;

    @Value("${RESEND_FROM_EMAIL:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${RESEND_TEST_RECIPIENT:likithgowdam10@gmail.com}")
    private String testRecipient;

    /**
     * Send an email via Resend HTTP API.
     */
    public boolean sendEmail(String to, String subject, String htmlBody) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("⚠️ RESEND_API_KEY is not configured in Render environment variables.");
            return false;
        }

        String targetEmail = (to != null && !to.trim().isEmpty()) ? to.trim() : testRecipient;
        boolean success = executeSend(targetEmail, subject, htmlBody);

        // If sending to the user's email failed due to Resend domain restriction (403),
        // fallback to sending to the registered account owner email (testRecipient)
        if (!success && !targetEmail.equalsIgnoreCase(testRecipient)) {
            System.out.println("🔄 Retrying Resend email with verified owner recipient: " + testRecipient);
            success = executeSend(testRecipient, "[TEST MODE] " + subject, htmlBody);
        }

        return success;
    }

    private boolean executeSend(String recipient, String subject, String htmlBody) {
        try {
            String jsonPayload = String.format(
                "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                escapeJson(fromEmail),
                escapeJson(recipient),
                escapeJson(subject),
                escapeJson(htmlBody)
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                System.out.println("✅ Resend email successfully sent to: " + recipient);
                return true;
            } else {
                System.err.println("❌ Resend API Error (" + response.statusCode() + "): " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Resend email exception: " + e.getMessage());
            return false;
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
