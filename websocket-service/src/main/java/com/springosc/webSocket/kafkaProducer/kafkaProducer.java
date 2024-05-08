package com.springosc.webSocket.kafkaProducer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springosc.webSocket.dto.LogoutDTO;
import com.springosc.webSocket.dto.ProductViewedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class kafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public kafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void publishRecentlyViewedProduct(ProductViewedDTO productViewedDTO) {
        try {
            String productJson = new ObjectMapper().writeValueAsString(productViewedDTO);
            kafkaTemplate.send("product-viewed", productJson);
            log.info("Published userId: {}, productId: {} and CategoryId: {} to Kafka topic product-viewed", productViewedDTO.getUserId(), productViewedDTO.getProductId(), productViewedDTO.getCategoryId());
        } catch (Exception e) {
            log.error("An error occurred while publishing to Kafka: " + e.getMessage());
        }
    }

    public void publishFilterRequest(String cateId, String filter){
        try{
            String message = cateId + "," + filter;
            kafkaTemplate.send("filter-product", message);
            log.info("Filter Product Published in Kafka: {}", message);
        }catch(Exception e){
            log.error("Some Error occurred: {}", e.getMessage());
        }
    }

    public void publishCartDataToTopic(String mtPing, String userId, String productId){
        try{
            String message = mtPing + "," + userId + "," + productId;
            kafkaTemplate.send("cart-data", message);
            log.info("Product is Published in Kafka Topic: {}", message);
        }catch(Exception e){
            log.error("Error Occurred: {}", e.getMessage());
        }
    }

    private void webSocketLogout(LogoutDTO logoutDTO) {
        try {
            String userJson = new ObjectMapper().writeValueAsString(logoutDTO);

            kafkaTemplate.send("logout-user", userJson);
            log.info("Logout information sent to Kafka topic: logout-user {}", userJson);
        } catch (JsonProcessingException exception) {
            log.error("Error converting LogoutDto to JSON" + exception.getMessage());
        }
    }

}
