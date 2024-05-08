package com.springosc.product.ProductResponse;

import com.hazelcast.map.IMap;
import com.osc.product_cache.CategoryProductMap;
import com.osc.product_cache.ProductDataDTO;
import com.osc.save_cart_product.*;
import com.springosc.product.dao.CartRepository;
import com.springosc.product.entity.Cart;
import com.springosc.product.entity.Product;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@GrpcService
public class SaveCartAddedProduct extends SaveCartAddedProductsGrpc.SaveCartAddedProductsImplBase {

    private final IMap<String, Map<String, Integer>> cartData;

    private final IMap<String, CategoryProductMap> productData;

    private final CartRepository cartRepository;

    public SaveCartAddedProduct(IMap<String, Map<String, Integer>> cartData,
                                IMap<String, CategoryProductMap> productData,
                                CartRepository cartRepository) {
        this.cartData = cartData;
        this.productData = productData;
        this.cartRepository = cartRepository;
    }

    @Override
    public void saveCartProducts(CartProductRequest request, StreamObserver<com.osc.save_cart_product.SaveCartProduct> responseObserver) {
        try {
            String userId = request.getUserId();

            Map<String, Integer> userCart = cartData.get(userId);
            if (userCart == null || userCart.isEmpty()) {
                log.info("No cart data found for user: {}", userId);
                responseObserver.onNext(SaveCartProduct.newBuilder()
                        .setMessage("No cart data found for user").build());
                responseObserver.onCompleted();
                return;
            }

            boolean success = saveCartAddedProducts(userId, userCart);

            String message = success ? "Success" : "Failed";

            SaveCartProduct response = SaveCartProduct.newBuilder()
                    .setMessage(message)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error occurred while saving cart products: {}", e.getMessage());
            responseObserver.onError(Status.INTERNAL.withDescription("Error occurred while saving cart products")
                    .asRuntimeException());
        }
    }

    private boolean saveCartAddedProducts(String userId, Map<String, Integer> userCart) {
        boolean success = true;
        try {
            // iterate over each entry in the userCart map
            for (Map.Entry<String, Integer> entry : userCart.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();

                log.info("Processing product for user {}: productId={}", userId, productId);

                // get cart data
                Map<String, Integer> userCartData = cartData.getOrDefault(userId, new HashMap<>());
                int oldCount = userCartData.getOrDefault(productId, 0);
                log.info("Retrieved existing quantity for product {} from user's cart data: {}", productId, oldCount);

                // get category id from product id
                String categoryId = extractCategory(productId);
                log.info("Extracted category ID: {} from productId: {}", categoryId, productId);

                // retrieve categoryProductMap for the extracted category id
                CategoryProductMap categoryProductMap = productData.get(categoryId);
                log.info("Retrieved CategoryProductMap for category ID: {}", categoryId);
                if (categoryProductMap == null) {
                    log.warn("CategoryProductMap not found for category ID: {}", categoryId);
                    continue;
                }

                // retrieve product map from categoryProductMap
                Map<String, ProductDataDTO> productMap = categoryProductMap.getProductMapMap();
                log.info("Retrieved product map from CategoryProductMap");

                // retrieve productDataDTO for the product id
                ProductDataDTO productDTO = productMap.get(productId);
                if (productDTO == null) {
                    log.error("Product not found for categoryId: {} and productId: {}", categoryId, productId);
                    success = false;
                    continue;
                }

                // convert productDataDTO to product object
                Product product = new Product();
                product.setProductName(productDTO.getProductName());
                product.setProductPrice(productDTO.getProductPrice());
                log.info("Converted ProductDTO to Product object: {}", product);

                // check if there is an existing cart entry for the user and product
                Cart existingCartEntry = cartRepository.findByUserIdAndProductId(userId, productId);
                if (existingCartEntry != null) {
                    existingCartEntry.setQuantity(quantity);
                    log.info("Updating existing cart entry for user {}: productId={}, old quantity={}, new quantity={}", userId, productId, oldCount, quantity);
                    cartRepository.save(existingCartEntry);
                } else {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    cart.setCategoryId(categoryId);
                    cart.setProductId(productId);
                    cart.setProductName(product.getProductName());
                    cart.setProductPrice(product.getProductPrice());
                    cart.setQuantity(quantity);
                    log.info("Saving new cart entry for user {}: productId={}, quantity={}", userId, productId, quantity);
                    cartRepository.save(cart);
                }
            }
        } catch (Exception e) {
            success = false;
            log.error("Error occurred while saving cart products: {}", e.getMessage());
        }
        return success;
    }

    private String extractCategory(String productId) {
        return productId.substring(0, 1);
    }

}

