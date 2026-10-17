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
 * 
 * Free tier: 100 emails/day, 3000/month.
 * Sign up at https://resend.com and get an API key.
 */
@Service
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${RESEND_API_KEY:}")
    private String apiKey;

    @Value("${RESEND_FROM_EMAIL:onboarding@resend.dev}")
    private String fromEmail;

    /**
     * Send an email via Resend HTTP API.
     * Returns true if sent successfully, false otherwise.
     */
    public boolean sendEmail(String to, String subject, String htmlBody) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("⚠️ RESEND_API_KEY not set, skipping email.");
            return false;
        }

        try {
            // Build JSON payload
            String jsonPayload = String.format(
                "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                escapeJson(fromEmail),
                escapeJson(to),
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
                System.out.println("✅ Resend email sent to: " + to);
                return true;
            } else {
                System.err.println("❌ Resend API error (" + response.statusCode() + "): " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Resend email FAILED: " + e.getMessage());
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
