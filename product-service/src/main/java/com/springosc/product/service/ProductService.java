package com.springosc.product.service;

import com.osc.product_cache.CategoryProductMap;
import com.springosc.product.dto.CustomResponseDTO;

import java.util.List;
import java.util.Map;

public interface ProductService {

    void publishProductsToKafka();

    List<Map.Entry<String, Integer>> getCategories(Map<String, CategoryProductMap> productMap);

    List<CategoryProductMap> getProducts(Map<String, CategoryProductMap> productMap);

    CustomResponseDTO generateDashboardData(String userId, Map<String, CategoryProductMap> productMap);

}
