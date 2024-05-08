package com.springosc.cache.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.springosc.cache.dto.ProductsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CacheProductService {

    private final HazelcastInstance hazelcastInstance;

    private static final String HAZELCAST_MAP_NAME = "productMap";

    public CacheProductService(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @KafkaListener(topics = "product-data", groupId = "product")
    public void consumeProductDTO(String productJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<ProductsDTO> productDTOList = objectMapper.readValue(productJson, new TypeReference<List<ProductsDTO>>() {});

            for (ProductsDTO productDTO : productDTOList) {
                String categoryId = productDTO.getCategoryId();
                String productId = productDTO.getProductId();

                IMap<String, Map<String, ProductsDTO>> categoryMap = hazelcastInstance.getMap(HAZELCAST_MAP_NAME);

                Map<String, ProductsDTO> productMap = categoryMap.get(categoryId);
                if (productMap == null) {
                    productMap = new HashMap<>();
                }

                productMap.put(productId, productDTO);
                categoryMap.put(categoryId, productMap);

                log.info("Received and processed ProductDTO: categoryId={}, productId={}, productDTO={}", categoryId, productId, productDTO);
            }
        } catch (Exception e) {
            log.error("error in Processing data from topic: {}", e.getMessage());
        }
    }


}
