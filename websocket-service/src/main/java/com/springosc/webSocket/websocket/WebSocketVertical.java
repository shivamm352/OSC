package com.springosc.webSocket.websocket;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.cart_product.CartProduct;
import com.osc.cart_product.CartRequest;
import com.osc.cart_product.CartResponse;
import com.osc.cart_product.CartServiceGrpc;
import com.osc.filter_product.FilterProductRequest;
import com.osc.filter_product.FilteredProductResponse;
import com.osc.filter_product.FilteredProductServiceGrpc;
import com.osc.filter_product.Product;
import com.osc.product_response.*;
import com.osc.refresh_product.RefreshProductRequest;
import com.osc.refresh_product.RefreshProductResponse;
import com.osc.refresh_product.RefreshProductServiceGrpc;
import com.springosc.webSocket.actor.ClientConnectionActor;
import com.springosc.webSocket.dto.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
public class WebSocketVertical extends AbstractVerticle {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProductDetailServiceGrpc.ProductDetailServiceBlockingStub productDetailServiceBlockingStub;
    private final CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub;
    private final FilteredProductServiceGrpc.FilteredProductServiceBlockingStub filteredProductServiceBlockingStub;
    private final RefreshProductServiceGrpc.RefreshProductServiceBlockingStub refreshProductServiceBlockingStub;
    Map<String, ActorRef> userThreadMap = new HashMap<>();
    Map<String, Long> activeUserTimeMap = new HashMap<>();

    public WebSocketVertical(KafkaTemplate<String, Object> kafkaTemplate,
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

    @Override
    public void start() {
        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol("OSC-WebSocket-Protocol");
        HttpServer server = vertx.createHttpServer(options);

        WebSocketVertical webSocketVertical = new WebSocketVertical(kafkaTemplate, productDetailServiceBlockingStub, cartServiceBlockingStub, filteredProductServiceBlockingStub, refreshProductServiceBlockingStub);
        server.webSocketHandler(socket -> handleWebSocket(socket, webSocketVertical));

        server.listen(8888, result -> {
            if (result.succeeded()) {
                log.info("Server started on port 8888");
            } else {
                log.info("Server failed to start");
            }
        });
        vertx.setPeriodic(5000, timerId -> {
            checkHeartbeat();
        });
    }

    private void handleWebSocket(ServerWebSocket socket, WebSocketVertical webSocketVertical) {
        MultiMap headers = socket.headers();
        String protocolHeader = headers.get("Sec-WebSocket-Protocol");
        String[] headerComponents = protocolHeader.split(",");

        System.out.println("Header: " + headers);

        String protocol = headerComponents[0];
        String userId = headerComponents[1].trim();
        String sessionId = headerComponents[2];
        String device = headerComponents[3];
        String customUserId = userId + "_" + sessionId.trim() + "_" + device;

        log.info("Custom UserId created is: "+ customUserId);

        if (isProtocolHeaderEmpty(headerComponents)) {
            socket.close((short) 400, "Invalid Connection: Missing Parameters");
        } else {
            createWebSocketThread(customUserId, socket, webSocketVertical, userId);
        }

        webSocketMessageHandler(socket, customUserId);

        closeWebSocketHandler(socket);
    }


    private void createWebSocketThread(String customUserId, ServerWebSocket webSocket, WebSocketVertical webSocketVertical, String userId) {
        ActorSystem actorSystem = ActorSystem.create();
        ActorRef actorRef = actorSystem.actorOf(ClientConnectionActor.props(webSocket, webSocketVertical, userId), "ClientProps");
        userThreadMap.put(customUserId, actorRef);
        log.info("THREAD CREATED SUCCESSFULLY");
    }

    public void webSocketMessageHandler(ServerWebSocket webSocket, String customUserId) {
        try {
            webSocket.textMessageHandler(message -> {
                try {
                    if (userThreadMap.containsKey(customUserId)) {
                        ActorRef actorRef = userThreadMap.get(customUserId);
                        actorRef.tell(message, ActorRef.noSender());//pass the msg
                    } else {
                        closeWebSocketHandler(webSocket);
                    }
                } catch (Exception e) {
                    log.error("Error handling WebSocket message: {}", e.getMessage());
                }
            });
        } catch (Exception exception) {
            log.error("Error setting up WebSocket text message handler: {}", exception.getMessage());
        }
    }


    public void closeWebSocketHandler(ServerWebSocket webSocket) {
        webSocket.closeHandler(close -> {
            try {
                log.info("WEB SOCKET CLOSED");
            } catch (Exception e) {
                log.info("Unable to Close Web Socket: {}", e.getMessage());
            }
        });
    }


    private void checkHeartbeat() {
        long currentTime = System.currentTimeMillis();
        activeUserTimeMap.entrySet().removeIf(entry -> {
            String customUserId = entry.getKey();
            long lastHeartbeatTime = entry.getValue();
            long diff = (currentTime - lastHeartbeatTime) / 1000;

            if (diff > 30 && diff <= 35) {
                closeWebSocket(customUserId);
            } else if (diff >= 120) {
                logoutUser(customUserId);
                log.info("User is Logged OUT");
            }
            return false;
        });
    }

    private void closeWebSocket(String customUserId) {
        ActorRef actorRef = userThreadMap.get(customUserId);
        if (actorRef != null) {
            actorRef.tell("{\"MT\": \"Close WebSocket\"}", ActorRef.noSender());
            actorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
        userThreadMap.remove(customUserId);
    }

    private void logoutUser(String customUserId) {
        activeUserTimeMap.remove(customUserId);
        String[] userInfoArr = customUserId.split("_");

        LogoutDTO logoutDto = new LogoutDTO();
        logoutDto.setUserId(userInfoArr[1]);
        logoutDto.setSessionId(userInfoArr[2].trim());

        webSocketLogout(logoutDto);
        log.info("USER LOGGED OUT SUCCESSFULLY for user: " + customUserId);
    }

    private void webSocketLogout(LogoutDTO logoutDTO) {
        try {
            String userJson = new ObjectMapper().writeValueAsString(logoutDTO);

            kafkaTemplate.send("logout-user", userJson);
            log.info("Logout information sent to Kafka topic: logout-user {}", userJson);
        } catch (JsonProcessingException exception) {
            log.error("Error converting LogoutDto to JSON" + exception.getMessage());
        }
    }

    private boolean isProtocolHeaderEmpty(String[] strings) {
        Predicate<String> predicate = str -> str == null && str.trim().isEmpty();
        for (String str : strings) {
            if (predicate.test(str)) {
                return true;
            }
        }
        return false;
    }

    public void publishViewedProductInTopic(ProductViewedDTO productViewedDTO) {
        try {
            String productJson = new ObjectMapper().writeValueAsString(productViewedDTO);
            kafkaTemplate.send("product-viewed", productJson);
            log.info("Published userId: {}, productId: {} and CategoryId: {} to Kafka topic product-viewed", productViewedDTO.getUserId(), productViewedDTO.getProductId(), productViewedDTO.getCategoryId());
        } catch (Exception e) {
            log.error("An error occurred while publishing to Kafka: " + e.getMessage());
        }
    }

    public void publishProductView(String productId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonProductId = objectMapper.writeValueAsString(new ObjectMapper().createObjectNode().put("productId", productId));
            kafkaTemplate.send("product-view-count", jsonProductId);
            log.info("Viewed Product Published in Kafka Topic is: {}", productId);
        } catch (Exception e) {
            log.error("Error publishing product view: {}", e.getMessage());
        }
    }

    public void publishCartDataToTopic(String mtPing, String userId, String productId){
        try{
            String message = mtPing + "," + userId + "," + productId;
            kafkaTemplate.send("cart-data", message);
            log.info("Product is Published in Kafka Topic: {}", message);
        }catch(Exception e){
            log.error("Error Occurred: {}", e.getMessage());
        }
    }

    public void getProductInformation(ServerWebSocket webSocket, String categoryId, String productId) {
        ProductRequest productRequest = ProductRequest.newBuilder()
                .setCategoryId(categoryId)
                .setProductId(productId)
                .build();
        ProductResponse response = productDetailServiceBlockingStub.getProductInformation(productRequest);
        ProductDetails productDetails = response.getProductDetails();

        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setMt("2");
        responseDTO.setCatId(categoryId);
        responseDTO.setProdId(productId);
        responseDTO.setProdName(productDetails.getProductName());
        responseDTO.setProdDesc(productDetails.getProductDescription());
        responseDTO.setProdMarketPrice(productDetails.getProductPrice());

        List<SimilarProductResponseDTO> similarProductDTOs = new ArrayList<>();
        for (SimilarProduct similarProduct : response.getSimilarProductsList()) {
            SimilarProductResponseDTO similarProductDTO = new SimilarProductResponseDTO();
            similarProductDTO.setCategoryId(similarProduct.getCategoryId());
            similarProductDTO.setProductId(similarProduct.getProductId());
            similarProductDTO.setProdName(similarProduct.getProductName());
            similarProductDTO.setProdMarketPrice(similarProduct.getProductPrice());
            similarProductDTOs.add(similarProductDTO);
        }
        log.info("Similar Products: {}", similarProductDTOs);

        responseDTO.setSimilarProducts(similarProductDTOs);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(responseDTO);
            webSocket.writeTextMessage(jsonResponse);
        } catch (JsonProcessingException e) {
            log.error("Error Occurred: ", e);
        }
    }


    public void getFilterProducts(ServerWebSocket webSocket, String categoryId, String filter) {
        FilterProductRequest request = FilterProductRequest.newBuilder()
                .setCateId(categoryId)
                .setFilter(filter)
                .build();

        FilteredProductResponse response = filteredProductServiceBlockingStub.filterProducts(request);

        FilterProductCategoryDTO filterProductCategoryDTO = new FilterProductCategoryDTO();
        filterProductCategoryDTO.setMt("3");
        filterProductCategoryDTO.setCatId(categoryId);
        List<FilterProductResponseDTO> products = new ArrayList<>();
        for (Product grpcProduct : response.getProductList()) {
            FilterProductResponseDTO dto = FilterProductResponseDTO.builder()
                    .productId(grpcProduct.getProductId())
                    .catId(grpcProduct.getCatId())
                    .prodName(grpcProduct.getProdName())
                    .prodMarketPrice(String.valueOf(grpcProduct.getProdMarketPrice()))
                    .build();
            products.add(dto);
        }
        filterProductCategoryDTO.setProducts(products);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonResponse = objectMapper.writeValueAsString(filterProductCategoryDTO);
            log.info("Converting Response into JSON: {}", jsonResponse);
            webSocket.writeTextMessage(jsonResponse);
        } catch (Exception e) {
            log.info("Some Error Occurred in sending Response: {}", e.getMessage());
        }
    }

    public void refreshProduct(ServerWebSocket webSocket, String mtPing, String userId){
        try {
            RefreshProductRequest request = RefreshProductRequest.newBuilder()
                    .setMT(mtPing)
                    .setUserId(userId)
                    .build();
            RefreshProductResponse response = refreshProductServiceBlockingStub.getRefreshRequest(request);
            String refreshResponse = response.getRefreshResponse();
            webSocket.writeTextMessage(refreshResponse);
        } catch (Exception e) {
            log.error("Exception occurred during refreshProduct: {}", e.getMessage());
        }
    }


    public void getCartProducts(ServerWebSocket webSocket, String userId){
        try {
            CartRequest request = CartRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            CartResponse response = cartServiceBlockingStub.getCartResponse(request);
            List<CartProduct> products = response.getCartProductList();

            List<CartResponseDTO> cartResponseDTOs = new ArrayList<>();

            for (CartProduct product : products) {
                log.info("Cart Response Received is: {}", product);

                CartResponseDTO cartResponseDTO = new CartResponseDTO();
                cartResponseDTO.setMt("6");
                cartResponseDTO.setProdId(product.getProdId());
                cartResponseDTO.setProdName(product.getProdName());
                cartResponseDTO.setPrice(product.getPrice());
                cartResponseDTO.setCartQty(product.getCartQty());

                cartResponseDTOs.add(cartResponseDTO);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String cartResponseJson = objectMapper.writeValueAsString(cartResponseDTOs);

            webSocket.writeTextMessage(cartResponseJson);
        } catch(Exception e) {
            log.error("Error occurred while processing cart products: {}", e.getMessage());
        }
    }


}
















/*
    PENDING TO REFACTOR

    @NoArgsConstructor
public class WebSocketVerticle extends AbstractVerticle {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    //modify object dto

    Map<String, ActorRef> userThreadMap = new HashMap<>();
    Map<String, Long> activeUserTimeMap = new HashMap<>();

    //use cache for both maps

    private static final Logger logger = Logger.getLogger(WebSocketVerticle.class.getName());

    @Override
    public void start() {
        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol("OSC-WebSocket-Protocol");
        //add in application properties or in constants = OSC-WebSocket-Protocol

        HttpServer server = vertx.createHttpServer(options);

        server.webSocketHandler(this::handleWebSocket);

        server.listen(8888, result -> {
            if (result.succeeded()) {
                logger.info("Server started on port 8888");
            } else {
                logger.info("Server failed to start");
            }
        });
        vertx.setPeriodic(5000, timerId -> {
            checkHeartbeat();
        });
    }

    private void handleWebSocket(ServerWebSocket socket) {
        MultiMap headers = socket.headers();
        String protocolHeader = headers.get("Sec-WebSocket-Protocol");
        //add in application properties or in constants = OSC-WebSocket-Protocol

        String[] headerComponents = protocolHeader.split(",");

        System.out.println("Header: " + headers);

        String userId = headerComponents[0];
        String sessionId = headerComponents[1];
        String device = headerComponents[2];
        String customUserId = userId + "_" + sessionId.trim() + "_" + device;

        System.out.println("Custom UserId created is: " + customUserId);

        if (isProtocolHeaderEmpty(headerComponents)) {
            socket.close((short) 400, "Invalid Connection: Missing Parameters");
            //this check need to performed before validations
        } else {
            createWebSocketThread(customUserId, socket);
        }
        webSocketMessageHandler(socket, customUserId);
        closeWebSocketHandler(socket);
    }


    private void createWebSocketThread(String customUserId, ServerWebSocket webSocket) {
        ActorSystem actorSystem = ActorSystem.create();
        ActorRef actorRef = actorSystem.actorOf(ClientConnectionActor.props(webSocket, UserSessionDTO.builder().build()), "ClientProps");
        userThreadMap.put(customUserId, actorRef);
        logger.info("THREAD CREATED SUCCESSFULLY");
    }

    public void webSocketMessageHandler(ServerWebSocket webSocket, String customUserId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            //Rnd on object mapper

            webSocket.textMessageHandler(message -> {
                try {
                    System.out.println(message);

                    //use logger instead of System.out.println

                    System.out.println("Message is Printed : " + message);

                    MessageRequestDTO messageRequestDTO = new MessageRequestDTO();
                    messageRequestDTO.setMt("Ping");

                    //RnD on MessageRequestDTO

                    String jsonPingMessage = objectMapper.writeValueAsString(messageRequestDTO);

                    webSocket.writeTextMessage(jsonPingMessage);

                    MessageRequestDTO receivedMessage = objectMapper.readValue(message, MessageRequestDTO.class);
                    //don't create object if not in use
                    //don't use if not in use
                    //do RnD in object Mapper Jackson

                    String device = customUserId.split("_")[0];
                    String userId = customUserId.split("_")[1];
                    String sessionId = customUserId.split("_")[2].trim();

                    logger.info("Received message from client: " + message);

                    if (isPingMessage(messageRequestDTO)) {
                        activeUserTimeMap.put(customUserId, System.currentTimeMillis());
                        System.out.println("Message of DTO " + messageRequestDTO);
                    }

                    String newUserId = userId.trim() + "_" + device.trim() + "_" + sessionId.toUpperCase();

                    WebSocketDTO websocketDtoObj = new WebSocketDTO();
                    websocketDtoObj.setUserId(newUserId);
                    websocketDtoObj.setWebsocketMessage(message);

                    userThreadMap.get(customUserId).tell(websocketDtoObj, ActorRef.noSender());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }



    private boolean isPingMessage(MessageRequestDTO requestMessage) {
        System.out.println("Is Ping is Ready");
        //use log
        return requestMessage.getMt().equalsIgnoreCase(AppConstants.PING);
    }

    public void closeWebSocketHandler(ServerWebSocket webSocket) {
        webSocket.closeHandler(close -> {
            try {
                logger.info("WEB SOCKET CLOSED");
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }


    private void checkHeartbeat() {
        long currentTime = System.currentTimeMillis();

        activeUserTimeMap.entrySet().removeIf(entry -> {
            String customUserId = entry.getKey();
            long lastHeartbeatTime = entry.getValue();
            long diff = (currentTime - lastHeartbeatTime) / 1000;

            if (diff > 30 && diff <= 35) {
                closeWebSocket(customUserId);
            } else if (diff >= 120) {
                System.out.println("User is Logged OUT");
                logoutUser(customUserId);
            }

            //for complete if - else go through doc

            return false;
        });
    }


    private void closeWebSocket(String customUserId) {
        ActorRef actorRef = userThreadMap.get(customUserId);
        if (actorRef != null) {
            actorRef.tell("{\"MT\": \"Close WebSocket\"}", ActorRef.noSender());
            actorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
        userThreadMap.remove(customUserId);
    }

    private void logoutUser(String customUserId) {
        activeUserTimeMap.remove(customUserId);
        String[] userInfoArr = customUserId.split("_");

        LogoutDTO logoutDto = new LogoutDTO();
        logoutDto.setUserId(userInfoArr[1]);
        logoutDto.setSessionId(userInfoArr[2].trim());

        webSocketLogout(logoutDto);
        logger.info("USER LOGGED OUT SUCCESSFULLY for user: " + customUserId);
    }

    //no need to store actor reference in map after user gets logout forcefully


    private void webSocketLogout(LogoutDTO logoutDTO) {
        try {
            String userJson = new ObjectMapper().writeValueAsString(logoutDTO);

            kafkaTemplate.send("logout-user", userJson);
            logger.info("Logout information sent to Kafka topic: logout-user - {}" + userJson);
        } catch (JsonProcessingException exception) {
            logger.warning("Error converting LogoutDto to JSON" + exception.getMessage());
        }
    }


    private boolean isProtocolHeaderEmpty(String[] strings) {
        Predicate<String> predicate = str -> str == null && str.trim().isEmpty();
        for (String str : strings) {
            if (predicate.test(str)) {
                return true;
            }
        }
        return false;
    }

}



====================================================================================================================





//    public void webSocketMessageHandler(ServerWebSocket webSocket, String customUserId) {
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//
//            //Rnd on object mapper
//
//            webSocket.textMessageHandler(message -> {
//                try {
//                    System.out.println(message);
//
//                    //use logger instead of System.out.println
//
//                    System.out.println("Message is Printed : " + message);
//
//                    MessageRequestDTO messageRequestDTO = new MessageRequestDTO();
//                    messageRequestDTO.setMt("Ping");
//
//                    //RnD on MessageRequestDTO
//
//                    String jsonPingMessage = objectMapper.writeValueAsString(messageRequestDTO);
//
//                    webSocket.writeTextMessage(jsonPingMessage);
//
//                    MessageRequestDTO receivedMessage = objectMapper.readValue(message, MessageRequestDTO.class);
//                    //don't create object if not in use
//                    //don't use if not in use
//                    //do RnD in object Mapper Jackson
//
//                    String device = customUserId.split("_")[0];
//                    String userId = customUserId.split("_")[1];
//                    String sessionId = customUserId.split("_")[2].trim();
//
//                    logger.info("Received message from client: " + message);
//
//                    if (isPingMessage(messageRequestDTO)) {
//                        activeUserTimeMap.put(customUserId, System.currentTimeMillis());
//                        System.out.println("Message of DTO " + messageRequestDTO);
//                    }
//
//                    String newUserId = userId.trim() + "_" + device.trim() + "_" + sessionId.toUpperCase();
//
//                    WebSocketDTO websocketDtoObj = new WebSocketDTO();
//                    websocketDtoObj.setUserId(newUserId);
//                    websocketDtoObj.setWebsocketMessage(message);
//
//                    userThreadMap.get(customUserId).tell(websocketDtoObj, ActorRef.noSender());
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
//    }




*/
