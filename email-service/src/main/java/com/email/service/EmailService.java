package com.email.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
//import com.email.dto.EmailDTO;
import java.io.IOException;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSenderImpl javaMailSenderImpl;
    private final String fromEmail = "shivamphotos35@gmail.com";

    public EmailService(JavaMailSenderImpl javaMailSenderImpl) {
        this.javaMailSenderImpl = javaMailSenderImpl;
    }

    @KafkaListener(topics = "user-data", groupId = "email-otp")
    public void sendOTPEmail(String userJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(userJson);
            String userId = jsonNode.get("userId").asText();
            String otp = jsonNode.get("otp").asText();
            String userEmail = jsonNode.get("email").asText();
            sendOTPByEmail(userId, otp, userEmail);
            log.info("OTP email sent for UserID: " + userId);
        } catch (Exception e) {
            log.error("Error processing User JSON Object from Kafka topic: ");
        }
    }


    private void sendOTPByEmail(String userId, String otp, String userEmail) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(userEmail);
            mailMessage.setSubject("OTP for Registration on OSC");
            mailMessage.setText("Your UserId is: " + userId + "\nYour OTP is: " + otp);
            javaMailSenderImpl.send(mailMessage);
            log.info("OTP email sent to: " + userEmail);
        } catch (Exception e) {
            log.error("Error sending OTP email: {}", e.getMessage());
        }
    }


    @KafkaListener(topics = "user-password-reset", groupId = "reset-password")
    public void passwordReset(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(message);
            String otp = jsonNode.get("otp").asText();
            String userEmail = jsonNode.get("email").asText();
            sendOTP(otp, userEmail);
        } catch (Exception e) {
            log.error("Error processing data from topic: {}", e.getMessage());
        }
    }


    private void sendOTP(String otp, String userEmail) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(userEmail);
            mailMessage.setSubject("OTP to Reset Password");
            mailMessage.setText("Your OTP is: " + otp);
            log.info("Sending OTP email to: " + userEmail);
            javaMailSenderImpl.send(mailMessage);
            log.info("OTP email sent to: " + userEmail);
        } catch (Exception e) {
            log.error("Error sending OTP email: {}", e.getMessage());
        }
    }

}

