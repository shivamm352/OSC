package com.springosc.product.ProductResponse;

import com.hazelcast.map.IMap;
import com.osc.product_cache.CategoryProductMap;
import com.osc.product_cache.ProductDataDTO;
import com.osc.product_response.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class ProductResponseServiceImpl extends ProductDetailServiceGrpc.ProductDetailServiceImplBase {

    private final IMap<String, CategoryProductMap> productData;

    @Autowired
    public ProductResponseServiceImpl(IMap<String, CategoryProductMap> productData) {
        this.productData = productData;
    }

    @Override
    public void getProductInformation(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {

        String categoryId = request.getCategoryId();
        String productId = request.getProductId();

        ProductDetails productDetails = getProductDetails(categoryId, productId);
        if (productDetails != null) {

            ProductResponse response = ProductResponse.newBuilder()
                    .setProductDetails(productDetails)
                    .addAllSimilarProducts(getSimilarProducts(categoryId, productId))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
        }
    }


    private ProductDetails getProductDetails(String categoryId, String productId) {
        CategoryProductMap categoryProductMap = productData.get(categoryId);
        if (categoryProductMap != null) {
            ProductDataDTO productDTO = categoryProductMap.getProductMapMap().get(productId);
            if (productDTO != null) {
                return ProductDetails.newBuilder()
                        .setCategoryId(productDTO.getCategoryId())
                        .setProductId(productDTO.getProductId())
                        .setProductName(productDTO.getProductName())
                        .setProductDescription(productDTO.getProductDescription())
                        .setProductPrice(productDTO.getProductPrice())
                        .build();
            }
        }
        return null;
    }

    private List<SimilarProduct> getSimilarProducts(String categoryId, String selectedProductId) {
        CategoryProductMap categoryProductMap = productData.get(categoryId);
        if (categoryProductMap != null) {
            Collection<ProductDataDTO> products = categoryProductMap.getProductMapMap().values();

            products = products.stream()
                    .filter(productDTO -> !productDTO.getProductId().equals(selectedProductId))
                    .toList();

            List<ProductDataDTO> sortedProducts = products.stream()
                    .sorted(Comparator.comparingInt(ProductDataDTO::getViewCount).reversed())
                    .toList();

            return sortedProducts.stream()
                    .limit(6)
                    .map(productDTO -> SimilarProduct.newBuilder()
                            .setCategoryId(productDTO.getCategoryId())
                            .setProductId(productDTO.getProductId())
                            .setProductName(productDTO.getProductName())
                            .setProductPrice(productDTO.getProductPrice())
                            .build())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
