package com.springosc.cache.kafkaConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springosc.cache.dto.PasswordResetDTO;
import com.springosc.cache.dto.UserDTO;
import com.springosc.cache.service.ResetPasswordCacheService;
import com.springosc.cache.service.UserDataCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumer {

    private final UserDataCacheService userDataCacheService;

    private final ResetPasswordCacheService resetPasswordCacheService;

    public KafkaConsumer(UserDataCacheService userDataCacheService, ResetPasswordCacheService resetPasswordCacheService) {
        this.userDataCacheService = userDataCacheService;
        this.resetPasswordCacheService = resetPasswordCacheService;
    }

    @KafkaListener(topics = "user-data", groupId = "user-data")
    public void consumeUserDTO(String userJson) {
        try {
            log.info("Received JSON from Kafka: " + userJson);

            ObjectMapper objectMapper = new ObjectMapper();
            UserDTO userDTO = objectMapper.readValue(userJson, UserDTO.class);

            log.info("User consumed from Kafka topic: " + userDTO.getName() + " (UserID: " + userDTO.getUserId() + ")");
            userDataCacheService.storeUserDataInCache(userDTO.getUserId(), userDTO);
        } catch (Exception e) {
            log.error("Error processing UserDTO from Kafka: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "user-password-reset", groupId = "password-reset")
    public void SaveUserOTP(String userJson){
        try{
            log.info("Received Details from Kafka: " + userJson);
            ObjectMapper objectMapper = new ObjectMapper();
            PasswordResetDTO passwordResetDTO = objectMapper.readValue(userJson, PasswordResetDTO.class);
            resetPasswordCacheService.storeOtpInCache(passwordResetDTO.getEmail(), passwordResetDTO.getOtp());
        }catch (Exception e){
            log.error("Error processing User Details from Kafka Topic: {}", e.getMessage());
        }
    }

}
