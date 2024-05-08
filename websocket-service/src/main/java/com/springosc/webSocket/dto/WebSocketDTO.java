package com.springosc.webSocket.dto;

import io.vertx.core.http.ServerWebSocket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketDTO {

    private String userId;
    private String websocketMessage;

    private String categoryId;
    private String ProductId;

    public WebSocketDTO(String closeWebsocket) {
    }

    public WebSocketDTO(ServerWebSocket webSocket) {
    }
}
