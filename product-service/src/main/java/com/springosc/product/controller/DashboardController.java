package com.springosc.product.controller;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.osc.product_cache.CategoryProductMap;
import com.springosc.product.dto.*;
import com.springosc.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class DashboardController {

    private final ProductService productService;

    private final IMap<String, CategoryProductMap> productData;

    public DashboardController(ProductService productService, IMap<String, CategoryProductMap> productData) {
        this.productService = productService;
        this.productData = productData;
    }

    @PostMapping("/dashBoard")
    public ResponseEntity<CustomResponseDTO> getDashboardData(@RequestBody DashBoardDTO dashboardDTO) {
        log.info("API call in DashBoard");
        String userId = dashboardDTO.getUserId();
        CustomResponseDTO customResponseDTO = productService.generateDashboardData(userId, productData);
        return ResponseEntity.ok(customResponseDTO);
    }

}


