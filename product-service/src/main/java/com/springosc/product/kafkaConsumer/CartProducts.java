package com.springosc.product.kafkaConsumer;

import com.hazelcast.map.IMap;
import com.springosc.product.dto.CartDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CartProducts {

    private final IMap<String, Map<String, Integer>> cartData;

    public CartProducts(IMap<String, Map<String, Integer>> cartData) {
        this.cartData = cartData;
    }

    @KafkaListener(topics = "cart-data", groupId = "product-data")
    public void consumeCartData(String productJson){
        try{
                log.info("Cart Data Received From User: {}", productJson);

            String[] parts = productJson.split(",");
            if (parts.length != 3) {
                log.error("Invalid format for cart data: {}", productJson);
                return;
            }
            String mtPing  = parts[0];
            String userId = parts[1];
            String productId = parts[2];

            log.info("Cart Data Received From User: {}", productJson);

            CartDTO cartDTO = new CartDTO();
            cartDTO.setUserId(userId);
            cartDTO.setProductId(productId);

            updateCartData(cartDTO, mtPing);

            log.info("Product Received: {}", productId);
        } catch (Exception e){
            log.error("Some Error Occurred {}", e.getMessage());
        }
    }

    private void updateCartData(CartDTO cartDTO, String mtPing) {
        String userId = cartDTO.getUserId();
        String productId = cartDTO.getProductId();
        log.info("Updating cart data for userId: {} and productId: {} with mtPing: {}", userId, productId, mtPing);
        Map<String, Integer> userCart = cartData.computeIfAbsent(userId, k ->
                new HashMap<>());

        switch (mtPing) {
            case "9":
                int count = userCart.getOrDefault(productId, 0) + 1;
                log.info("Incrementing count for productId {} in user's cart. New count: {}", productId, count);
                userCart.put(productId, count);
                break;
            case "8":
                if (userCart.containsKey(productId)) {
                    int currentCount = userCart.get(productId);
                    if (currentCount > 1) {
                        int newCount = currentCount - 1;
                        log.info("Decrementing count for productId {} in user's cart. New count: {}", productId, newCount);
                        userCart.put(productId, newCount);
                    } else {
                        log.info("Count for productId {} in user's cart is already 1, cannot decrement further", productId);
                    }
                } else {
                    log.warn("Product {} not found in user's cart", productId);
                }
                break;

            case "10":
                log.info("Setting count as 0 for productId {} in user's cart", productId);
                userCart.put(productId, 0);
                break;
            default:
                log.warn("Unknown mtPing value: {}", mtPing);
                break;
        }

        cartData.put(userId, userCart);
        log.info("Updated cart data for userId: {} and productId: {} with mtPing: {}", userId, productId, mtPing);
    }

}

