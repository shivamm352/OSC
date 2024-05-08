package com.springosc.product.ProductResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.map.IMap;
import com.osc.product_cache.CategoryProductMap;
import com.osc.refresh_product.RefreshProductRequest;
import com.osc.refresh_product.RefreshProductResponse;
import com.osc.refresh_product.RefreshProductServiceGrpc;
import com.springosc.product.dao.RecentlyViewedRepository;
import com.springosc.product.dto.CustomResponseDTO;
import com.springosc.product.serviceImpl.ProductServiceImpl;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


@Slf4j
@GrpcService
public class RefreshResponse extends RefreshProductServiceGrpc.RefreshProductServiceImplBase {

    private final ProductServiceImpl productService;
    private final IMap<String, CategoryProductMap> productData;

    public RefreshResponse(ProductServiceImpl productService,
                           IMap<String, CategoryProductMap> productData) {
        this.productService = productService;
        this.productData = productData;
    }

    @Override
    public void getRefreshRequest(RefreshProductRequest request, StreamObserver<RefreshProductResponse> responseObserver) {

        log.info("Received RefreshProductRequest: {}", request);

        String mtPing = request.getMT();
        RefreshProductResponse.Builder responseBuilder = RefreshProductResponse.newBuilder();

        if ("11".equals(mtPing)) {
            CustomResponseDTO responseDTO = productService.generateDashboardData(request.getUserId(), productData); // Pass appropriate productMapMap

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonData = objectMapper.writeValueAsString(responseDTO);

                JsonNode jsonNode = objectMapper.readTree(jsonData);

                ((ObjectNode) jsonNode).put("MT", "11");

                String modifiedJsonData = objectMapper.writeValueAsString(jsonNode);

                responseBuilder.setRefreshResponse(modifiedJsonData);

                responseObserver.onNext(responseBuilder.build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error("Error converting response to JSON: {}", e.getMessage());
                responseObserver.onError(e);
            }
        } else {
            log.warn("Invalid mtPing value received: {}", mtPing);
        }

    }
}