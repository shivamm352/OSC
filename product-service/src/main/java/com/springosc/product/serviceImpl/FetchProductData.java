package com.springosc.product.serviceImpl;

import com.google.protobuf.Empty;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.osc.product_cache.CategoryProductMap;
import com.osc.product_cache.ProductServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class FetchProductData {

    private final ProductServiceGrpc.ProductServiceBlockingStub productServiceBlockingStub;
    private final IMap<String, CategoryProductMap> productData;

    public FetchProductData(@GrpcClient("cache")ProductServiceGrpc.ProductServiceBlockingStub productServiceBlockingStub,
                            IMap<String, CategoryProductMap> productData) {
        this.productServiceBlockingStub = productServiceBlockingStub;
        this.productData = productData;
    }

    public void getProductData() {
        try {
            Map<String, CategoryProductMap> productDetailsMap = productServiceBlockingStub.getProductMap(Empty.getDefaultInstance()).getProductMapMap();

            productData.putAll(productDetailsMap);

            log.info("GRPC call Completed {}", productDetailsMap);
            log.info("Data Stored: {}", productData);
        } catch (Exception exception) {
            log.error("Error to get Product Data", exception);
        }
    }

}


