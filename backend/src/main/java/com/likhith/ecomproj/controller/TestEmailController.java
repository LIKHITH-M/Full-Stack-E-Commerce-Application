package com.likhith.ecomproj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin
public class TestEmailController {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    /**
     * Test endpoint: GET /api/test/send-email?to=someone@gmail.com
     * This sends a test email to verify SMTP configuration works.
     */
    @GetMapping("/send-email")
    public String sendTestEmail(@RequestParam String to) {
        System.out.println("========================================");
        System.out.println("TEST EMAIL ENDPOINT HIT");
        System.out.println("Sender: " + senderEmail);
        System.out.println("Recipient: " + to);
        System.out.println("========================================");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (senderEmail != null && !senderEmail.trim().isEmpty()) {
                message.setFrom(senderEmail.trim());
            }
            message.setTo(to.trim());
            message.setSubject("Test Email from E-Commerce App");
            message.setText("Hello!\n\nThis is a test email from your E-Commerce application.\nIf you received this, your SMTP configuration is working correctly!\n\nTimestamp: " + new java.util.Date());

            mailSender.send(message);
            System.out.println("✅ TEST EMAIL SENT SUCCESSFULLY to: " + to);
            return "✅ Email sent successfully to: " + to;
        } catch (Exception e) {
            System.err.println("❌ TEST EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
            return "❌ Email FAILED: " + e.getMessage();
        }
    }
}
