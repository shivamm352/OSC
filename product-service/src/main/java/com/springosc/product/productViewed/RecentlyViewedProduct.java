package com.springosc.product.productViewed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RecentlyViewedProduct {

    private final IMap<String, List<String>> recentlyViewedMap;

    public RecentlyViewedProduct(IMap<String, List<String>> recentlyViewedMap) {
        this.recentlyViewedMap = recentlyViewedMap;
    }

    @KafkaListener(topics = "product-viewed", groupId = "product-Viewed")
    public void listen(String message) {
        try {
            log.info("Received message from Kafka: {}", message);
            JsonNode json = new ObjectMapper().readTree(message);
            String[] parts = message.split(",");
            if (parts.length != 3) {
                log.error("Invalid message format, Expected format: <userId>,<productId>,<categoryId>. Received: {}", message);
                return;
            }
            String userId = json.get("userId").asText();
            String productId = json.get("productId").asText();
            String categoryId = json.get("categoryId").asText();
            updateRecentlyViewed(userId, productId, categoryId);
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", e.getMessage());
        }
    }

    private void updateRecentlyViewed(String userId, String productId, String categoryId) {
        List<String> recentlyViewed = recentlyViewedMap.getOrDefault(userId, new ArrayList<>());

        String productWithCategory = productId + ":" + categoryId;

        if (recentlyViewed.contains(productWithCategory)) {
            log.info("Product {} with category {} already exists in recently viewed list for user {}", productId, categoryId, userId);
            return;
        }

        recentlyViewed.add(productWithCategory);

        if (recentlyViewed.size() > 6) {
            String removedProduct = recentlyViewed.remove(0);
            log.info("Removed product {} from user {}'s recently viewed list", removedProduct, userId);
        }

        recentlyViewedMap.put(userId, recentlyViewed);
        log.info("UserId added is :" + userId);
        log.info("Updated recently viewed list for user {}: {}", userId, recentlyViewed);
    }

}
