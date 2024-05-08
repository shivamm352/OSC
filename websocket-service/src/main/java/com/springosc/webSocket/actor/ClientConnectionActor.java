package com.springosc.webSocket.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springosc.webSocket.constant.AppConstants;
import com.springosc.webSocket.dto.ProductViewedDTO;
import com.springosc.webSocket.websocket.WebSocketVertical;
import io.vertx.core.http.ServerWebSocket;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class ClientConnectionActor extends AbstractActor {

    private ServerWebSocket serverWebSocket;
    private WebSocketVertical webSocketVertical;
    private String users;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientConnectionActor(ServerWebSocket serverWebSocket, WebSocketVertical webSocketVertical, String userId) {
        this.serverWebSocket = serverWebSocket;
        this.webSocketVertical = webSocketVertical;
        this.users = userId;
    }

    public static Props props(ServerWebSocket webSocket, WebSocketVertical webSocketVertical, String userId) {
        return Props.create(ClientConnectionActor.class, () -> new ClientConnectionActor(webSocket, webSocketVertical, userId));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::pingResponse)
                .build();
    }

    private void pingResponse(String ping) {
        try {
            JsonNode jsonNode = objectMapper.readTree(ping);
            JsonNode mtNode = jsonNode.get("MT");

            if (mtNode != null && mtNode.isTextual()) {
                String mtValue = mtNode.asText();
                switch (mtValue) {
                    case AppConstants.PING:
                        serverWebSocket.writeTextMessage("{\"MT\":\"ping\"}");
                        break;

                    case AppConstants.VIEWED_PRODUCT:
                        log.info("MT:2 Ping");
                        String categoryId = jsonNode.get("catId").asText();
                        String productId = jsonNode.get("prodId").asText();
                        ProductViewedDTO productViewedDTO = new ProductViewedDTO();
                        productViewedDTO.setUserId(users);
                        productViewedDTO.setProductId(productId);
                        productViewedDTO.setCategoryId(categoryId);
                        webSocketVertical.publishViewedProductInTopic(productViewedDTO);
                        webSocketVertical.publishProductView(productId);
                        webSocketVertical.getProductInformation(serverWebSocket, categoryId, productId);
                        break;

                    case AppConstants.FILTERED_PRODUCTS:
                        log.info("MT:3 Ping");
                        String cateId = jsonNode.get("catId").asText();
                        String filter = jsonNode.get("filter").asText();
                        webSocketVertical.getFilterProducts(serverWebSocket, cateId, filter);
                        break;

                    case AppConstants.GET_CART_PRODUCTS:
                        log.info("MT:6 Ping");
                        String loggedInUserId = jsonNode.get("userId").asText();
                        webSocketVertical.getCartProducts(serverWebSocket, loggedInUserId);
                        break;

                    case AppConstants.ADD_TO_CART:
                        log.info("MT:9 Ping");
                        String mtPing = jsonNode.get("MT").asText();
                        String usersId = jsonNode.get("userId").asText();
                        String prodId = jsonNode.get("prodId").asText();
                        webSocketVertical.publishCartDataToTopic(mtPing, usersId, prodId);
                        break;

                    case AppConstants.REMOVE_FROM_CART:
                        log.info("MT:8 Ping");
                        String mtPings = jsonNode.get("MT").asText();
                        String userId = jsonNode.get("userId").asText();
                        String productsId = jsonNode.get("prodId").asText();
                        webSocketVertical.publishCartDataToTopic(mtPings, userId, productsId);
                        break;

                    case AppConstants.DELETE_FROM_CART:
                        log.info("MT:10 Ping");
                        String pings = jsonNode.get("MT").asText();
                        String userIds = jsonNode.get("userId").asText();
                        String productIds = jsonNode.get("prodId").asText();
                        webSocketVertical.publishCartDataToTopic(pings, userIds, productIds);
                        break;

                    case AppConstants.REFRESH_PRODUCT:
                        log.info("MT:11 Ping");
                        String refreshPing = jsonNode.get("MT").asText();
                        webSocketVertical.refreshProduct(serverWebSocket, refreshPing, users);
                        break;

                    case AppConstants.CLOSE_WEBSOCKET:
                        serverWebSocket.close();
                        break;

                    default:
                        break;
                }
            }
        } catch (Exception e) {
            log.error("Some Error Occurred: {}", e.getMessage());
        }
    }

}


