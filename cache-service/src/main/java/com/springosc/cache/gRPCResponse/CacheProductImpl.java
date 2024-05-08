package com.springosc.cache.gRPCResponse;

import com.google.protobuf.Empty;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.osc.product_cache.ProductDataDTO;
import com.osc.product_cache.CategoryProductMap;
import com.osc.product_cache.ProductMapResponse;
import com.osc.product_cache.ProductServiceGrpc;
import com.springosc.cache.dto.ProductsDTO;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@GrpcService
public class CacheProductImpl extends ProductServiceGrpc.ProductServiceImplBase {

    private final HazelcastInstance hazelcastInstance;

    public CacheProductImpl(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public void getProductMap(Empty request, StreamObserver<ProductMapResponse> responseObserver) {
        try {
            Map<String, CategoryProductMap> productMap = fetchProductMapFromHazelcast();

            log.info("Product map fetched successfully from Hazelcast.");

            ProductMapResponse response = ProductMapResponse.newBuilder()
                    .putAllProductMap(productMap)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("An error occurred while fetching product map from Hazelcast: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }


    private Map<String, CategoryProductMap> fetchProductMapFromHazelcast() {
        IMap<String, Map<String, ProductsDTO>> rawProductMap = hazelcastInstance.getMap("productMap");

        Map<String, CategoryProductMap> productMap = new HashMap<>();
        for (Map.Entry<String, Map<String, ProductsDTO>> entry : rawProductMap.entrySet()) {
            String categoryId = entry.getKey();
            Map<String, ProductsDTO> products = entry.getValue();

            CategoryProductMap.Builder categoryProductMapBuilder = CategoryProductMap.newBuilder();

            for (Map.Entry<String, ProductsDTO> productEntry : products.entrySet()) {
                String productId = productEntry.getKey();
                ProductsDTO productsDTO = productEntry.getValue();

                ProductDataDTO productDTO = ProductDataDTO.newBuilder()
                        .setCategoryId(productsDTO.getCategoryId())
                        .setProductId(productsDTO.getProductId())
                        .setProductName(productsDTO.getProductName())
                        .setProductPrice(productsDTO.getProdMarketPrice())
                        .setProductDescription(productsDTO.getProductDescription())
                        .setViewCount(productsDTO.getViewCount())
                        .build();

                categoryProductMapBuilder.putProductMap(productId, productDTO);
            }

            productMap.put(categoryId, categoryProductMapBuilder.build());
        }
        return productMap;
    }


}

