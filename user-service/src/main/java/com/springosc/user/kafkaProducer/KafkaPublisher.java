package com.springosc.user.kafkaProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springosc.user.dto.ResetPasswordDTO;
import com.springosc.user.dto.SessionEventDTO;
import com.springosc.user.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserDataInTopic(UserDTO userDTO) {
        try {
            String userJson = new ObjectMapper().writeValueAsString(userDTO);
            kafkaTemplate.send("user-data", userJson);
            log.info("User published to Kafka topic: " + userDTO.getName() + " (UserID: " + userDTO.getUserId() + ")");
        } catch (Exception e) {
            log.error("Error Publishing in Topic: {}", e.getMessage());
        }
    }

    public void publishUserSession(SessionEventDTO sessionEventDTO, boolean isLoggedIn) {
        try {
            String key = sessionEventDTO.getUserId() + "_" + sessionEventDTO.getDevice();
            String value = String.valueOf(isLoggedIn);
            kafkaTemplate.send("session-stream", key, value);
            log.info("Data Published in topic: " + key + ", Value: " + value);
        } catch (Exception e) {
            log.error("Error occurred during publishing in topic: {}", e.getMessage());
        }
    }

    public void publishOTPToResetPassword(ResetPasswordDTO resetPasswordDTO) {
        try {
            String userJson = new ObjectMapper().writeValueAsString(resetPasswordDTO);
            kafkaTemplate.send("user-password-reset", userJson);
            log.info("Details published to Kafka topic: {} (OTP: {})", resetPasswordDTO.getEmail(), resetPasswordDTO.getOtp());
        } catch (Exception e) {
            log.error("Error converting ResetPasswordDTO to JSON: {}", e.getMessage());
        }
    }

}
