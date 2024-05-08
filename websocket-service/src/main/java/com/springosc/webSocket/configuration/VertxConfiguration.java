package com.springosc.webSocket.configuration;

import com.osc.cart_product.CartServiceGrpc;
import com.osc.filter_product.FilteredProductServiceGrpc;
import com.osc.product_response.ProductDetailServiceGrpc;
import com.osc.refresh_product.RefreshProductServiceGrpc;
import io.vertx.core.Vertx;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.springosc.webSocket.websocket.WebSocketVertical;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class VertxConfiguration {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProductDetailServiceGrpc.ProductDetailServiceBlockingStub productDetailServiceBlockingStub;
    private final CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub;
    private final FilteredProductServiceGrpc.FilteredProductServiceBlockingStub filteredProductServiceBlockingStub;
    private final RefreshProductServiceGrpc.RefreshProductServiceBlockingStub refreshProductServiceBlockingStub;

    public VertxConfiguration(KafkaTemplate<String, Object> kafkaTemplate,
                              @GrpcClient("product") ProductDetailServiceGrpc.ProductDetailServiceBlockingStub productDetailServiceBlockingStub,
                              @GrpcClient("product") CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub,
                              @GrpcClient("product") FilteredProductServiceGrpc.FilteredProductServiceBlockingStub filteredProductServiceBlockingStub,
                              @GrpcClient("product") RefreshProductServiceGrpc.RefreshProductServiceBlockingStub refreshProductServiceBlockingStub) {
        this.kafkaTemplate = kafkaTemplate;
        this.productDetailServiceBlockingStub = productDetailServiceBlockingStub;
        this.cartServiceBlockingStub = cartServiceBlockingStub;
        this.filteredProductServiceBlockingStub = filteredProductServiceBlockingStub;
        this.refreshProductServiceBlockingStub = refreshProductServiceBlockingStub;
    }

    @Bean
    public Vertx vertx(){
        return Vertx.vertx();
    }

    @Bean
    public WebSocketVertical webSocketVertical(){
        return new WebSocketVertical(kafkaTemplate, productDetailServiceBlockingStub, cartServiceBlockingStub, filteredProductServiceBlockingStub, refreshProductServiceBlockingStub);
    }

}
