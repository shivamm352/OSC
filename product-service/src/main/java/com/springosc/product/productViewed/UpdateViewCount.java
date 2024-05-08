package com.springosc.product.productViewed;

import com.hazelcast.map.IMap;
import com.springosc.product.dao.ProductRepository;
import com.springosc.product.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UpdateViewCount {

    private final IMap<String, Integer> viewedProductMap;
    private final ProductRepository productRepository;

    public UpdateViewCount(IMap<String, Integer> viewedProductMap, ProductRepository productRepository) {
        this.viewedProductMap = viewedProductMap;
        this.productRepository = productRepository;
    }

    public void updateMap(String productId) {
        if (viewedProductMap.containsKey(productId)) {
            int newViewCount = viewedProductMap.get(productId) + 1;
            viewedProductMap.put(productId, newViewCount);
            log.info("Incremented view count for product {}: {}", productId, newViewCount);
        } else {
            viewedProductMap.put(productId, 1);
            log.info("Added new product {} with initial view count: 1", productId);
        }
    }

    public void updateProductViewCount() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3600000);
                    updateProductViewCountsAndClearMap();
                } catch (Exception e) {
                    log.info("Some Error Occurred: {}", e.getMessage());
                }
            }
        });
        thread.start();
    }

        public void updateProductViewCountsAndClearMap() {
        log.info("Updating product view counts and clearing the map...");
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            String productId = product.getProductId();
            if (viewedProductMap.containsKey(productId)) {
                int viewCount = viewedProductMap.get(productId);
                product.setViewCount(product.getViewCount() + viewCount);
                viewedProductMap.remove(productId);
                log.info("View count for product {} updated to: {}", productId, product.getViewCount());
            }
        }
        productRepository.saveAll(products);
        log.info("Product view counts updated and map cleared.");
    }


    @PostConstruct
    public void initialize() {
        updateProductViewCount();
    }

}
