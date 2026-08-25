package com.paasmart.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.from-address}")
    private String fromAddress;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Your PaasMart Seller verification code");
        message.setText(
                "Your verification code is: " + otp + "\n\n" +
                        "This code will expire in 10 minutes.\n\n" +
                        "If you did not request this, you can safely ignore this email."
        );
        mailSender.send(message);
    }
}