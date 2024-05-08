package com.springosc.product.ProductResponse;

import com.hazelcast.map.IMap;
import com.osc.cart_product.CartProduct;
import com.osc.cart_product.CartRequest;
import com.osc.cart_product.CartResponse;
import com.osc.cart_product.CartServiceGrpc;
import com.osc.product_cache.CategoryProductMap;
import com.osc.product_cache.ProductDataDTO;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
public class CartProductResponse extends CartServiceGrpc.CartServiceImplBase {

    private final IMap<String, CategoryProductMap> productData;

    private final IMap<String, Map<String, Integer>> cartData;

    public CartProductResponse(IMap<String, CategoryProductMap> productData,
                               IMap<String, Map<String, Integer>> cartData) {
        this.productData = productData;
        this.cartData = cartData;
    }

    @Override
    public void getCartResponse(CartRequest request, StreamObserver<CartResponse> responseObserver) {
        String userId = request.getUserId();

        try {
            Map<String, Integer> userCart = cartData.get(userId);

            if (userCart != null && !userCart.isEmpty()) {
                List<CartProduct> cartProducts = new ArrayList<>();

                for (Map.Entry<String, Integer> entry : userCart.entrySet()) {
                    String productId = entry.getKey();
                    int quantity = entry.getValue();

                    if (quantity > 0) {
                        CategoryProductMap categoryProductMap = productData.get(productId.substring(0, 1));
                        if (categoryProductMap != null) {
                            Map<String, ProductDataDTO> productMap = categoryProductMap.getProductMapMap();
                            ProductDataDTO productDTO = productMap.get(productId);
                            if (productDTO != null) {
                                CartProduct cartProduct = CartProduct.newBuilder()
                                        .setProdId(productId)
                                        .setProdName(productDTO.getProductName())
                                        .setPrice(productDTO.getProductPrice())
                                        .setCartQty(quantity)
                                        .build();
                                cartProducts.add(cartProduct);
                            } else {
                                log.warn("Product details not found for productId: {}", productId);
                            }
                        } else {
                            log.warn("Category details not found for productId: {}", productId.substring(0, 1));
                        }
                    }
                }
                CartResponse response = CartResponse.newBuilder()
                        .addAllCartProduct(cartProducts)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            log.error("Error occurred while fetching cart products: {}", e.getMessage());
        }
    }
}

