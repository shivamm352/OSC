package com.springosc.product.ProductResponse;

import com.hazelcast.map.IMap;
import com.osc.saveRecentlyViewed.*;
import com.springosc.product.dao.RecentlyViewedRepository;
import com.springosc.product.entity.RecentlyViewed;
import com.springosc.product.productViewed.RecentlyViewedProduct;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class SaveViewedProducts extends SaveViewedProductsGrpc.SaveViewedProductsImplBase {

    private final RecentlyViewedRepository recentlyViewedRepository;

    private final IMap<String, List<String>> recentlyViewedMap;


    public SaveViewedProducts(RecentlyViewedRepository recentlyViewedRepository, IMap<String, List<String>> recentlyViewedMap) {
        this.recentlyViewedRepository = recentlyViewedRepository;
        this.recentlyViewedMap = recentlyViewedMap;
    }

    @Override
    public void saveRecentlyViewedData(RecentlyViewedDataRequest request, StreamObserver<SaveResponse> responseObserver) {
        String userId = request.getUserId();

        List<String> recentlyViewedProducts = recentlyViewedMap.get(userId);
        boolean success = saveRecentlyViewedDataToTable(userId, recentlyViewedProducts);

        String message = success ? "Success" : "Failed";

        SaveResponse response = SaveResponse.newBuilder()
                .setMessage(message)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    private boolean saveRecentlyViewedDataToTable(String userId, List<String> recentlyViewedProducts) {
        try {
            for (String productWithCategory : recentlyViewedProducts) {
                String[] parts = productWithCategory.split(":");
                if (parts.length != 2) {
                    log.error("Invalid product format: {}", productWithCategory);
                    continue;
                }
                String productId = parts[0];
                String categoryId = parts[1];

                boolean exists = recentlyViewedRepository.existsByUserIdAndProductId(userId, productId);
                if (exists) {
                    log.info("Product {} already exists for userId {}, skipping insertion.", productId, userId);
                    continue;
                }

                RecentlyViewed recentlyViewed = new RecentlyViewed();
                recentlyViewed.setUserId(userId);
                recentlyViewed.setProductId(productId);
                recentlyViewed.setCategoryId(categoryId);

                recentlyViewedRepository.save(recentlyViewed);

                log.info("Saved recently viewed product {} with categoryId {} for userId {}", productId, categoryId, userId);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to save recently viewed products for userId {}: {}", userId, e.getMessage());
            return false;
        }
    }


}