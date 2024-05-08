package com.springosc.product.kafkaConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springosc.product.productViewed.UpdateViewCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpdateProductViewCount {

    private final UpdateViewCount updateViewCount;
    private final ObjectMapper objectMapper;

    public UpdateProductViewCount(UpdateViewCount updateViewCount, ObjectMapper objectMapper) {
        this.updateViewCount = updateViewCount;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "product-view-count", groupId = "product-view-group")
    public void consumeProductId(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String productId = jsonNode.get("productId").asText();
            updateViewCount.updateMap(productId);
            log.info("Product ID {} updated in the map.", productId);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

}
