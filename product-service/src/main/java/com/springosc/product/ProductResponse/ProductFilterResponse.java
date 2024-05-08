package com.springosc.product.ProductResponse;

import com.hazelcast.map.IMap;
import com.osc.filter_product.FilterProductRequest;
import com.osc.filter_product.FilteredProductResponse;
import com.osc.filter_product.FilteredProductServiceGrpc;
import com.osc.filter_product.Product;
import com.osc.product_cache.CategoryProductMap;
import com.osc.product_cache.ProductDataDTO;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class ProductFilterResponse extends FilteredProductServiceGrpc.FilteredProductServiceImplBase {

    private final IMap<String, CategoryProductMap> productData;

    public ProductFilterResponse(IMap<String, CategoryProductMap> productData) {
        this.productData = productData;
    }

    @Override
    public void filterProducts(FilterProductRequest request, StreamObserver<FilteredProductResponse> responseObserver) {
        try {
            log.info("inside filter products service");
            String categoryId = request.getCateId();
            CategoryProductMap categoryProductMap = productData.get(categoryId);

            log.info("received category Id is: " + categoryId);

            if (categoryProductMap == null) {
                log.error("No products found for category ID: {}", categoryId);
                responseObserver.onError(new RuntimeException("No products found for category ID: " + categoryId));
                return;
            }

            List<Product> productList = categoryProductMap.getProductMapMap().values().stream()
                    .map(productDTO -> Product.newBuilder()
                            .setProductId(productDTO.getProductId())
                            .setProdName(productDTO.getProductName())
                            .setProdMarketPrice(productDTO.getProductPrice())
                            .setCatId(productDTO.getCategoryId())
                            .build())
                    .collect(Collectors.toList());

            FilteredProductResponse response = null;

            switch (request.getFilter()) {
                case "HL":
                    log.info("requested filter is: " + request.getFilter());
                    productList.sort(Comparator.comparingDouble(Product::getProdMarketPrice).reversed());
                    response = FilteredProductResponse.newBuilder()
                            .setCateId(categoryId)
                            .addAllProduct(productList)
                            .build();
                    break;
                case "LH":
                    log.info("requested filter is: " + request.getFilter());
                    productList.sort(Comparator.comparingDouble(Product::getProdMarketPrice));
                    response = FilteredProductResponse.newBuilder()
                            .setCateId(categoryId)
                            .addAllProduct(productList)
                            .build();
                    break;
                case "P":
                    log.info("requested filter is: " + request.getFilter());
                    Collection<ProductDataDTO> productDataDTO = categoryProductMap.getProductMapMap().values();
                    List<ProductDataDTO> productDataList = new ArrayList<>(productDataDTO);
                    List<ProductDataDTO> sortedList = productDataList.stream()
                            .sorted(Comparator.comparingInt(ProductDataDTO::getViewCount).reversed())
                            .toList();

                    List<Product> list = sortedList.stream()
                            .map(productDTO -> Product.newBuilder().
                            setProductId(productDTO.getProductId()).
                            setProdName(productDTO.getProductName()).
                            setCatId(productDTO.getCategoryId()).
                            setProdMarketPrice(productDTO.getProductPrice()).build())
                            .toList();
                    response = FilteredProductResponse.newBuilder()
                            .setCateId(categoryId)
                            .addAllProduct(list)
                            .build();
                    break;
                case "NF":
                    log.info("requested filter is: " + request.getFilter());
                    Collection<ProductDataDTO> product = categoryProductMap.getProductMapMap().values();
                    List<ProductDataDTO> productDTOList = new ArrayList<>(product);
                    List<ProductDataDTO> sortedProduct = productDTOList.stream()
                            .sorted(Comparator.comparingInt(ProductDataDTO::getViewCount))
                            .toList();

                    List<Product> products = sortedProduct.stream()
                            .map(productDTO -> Product.newBuilder().
                                    setProductId(productDTO.getProductId()).
                                    setProdName(productDTO.getProductName()).
                                    setCatId(productDTO.getCategoryId()).
                                    setProdMarketPrice(productDTO.getProductPrice()).build())
                            .toList();
                    response = FilteredProductResponse.newBuilder()
                            .setCateId(categoryId)
                            .addAllProduct(products)
                            .build();
                    break;
                default:
                    log.warn("Unknown filter: {}", request.getFilter());
                    break;
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        }catch (Exception e){
            log.error("Unable to send Response: {}", e.getMessage());
        }

    }
}