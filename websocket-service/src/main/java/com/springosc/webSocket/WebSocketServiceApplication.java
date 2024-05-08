package com.springosc.webSocket;

import io.vertx.core.Vertx;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.springosc.webSocket.websocket.WebSocketVertical;

@SpringBootApplication
@EnableScheduling
public class WebSocketServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(WebSocketServiceApplication.class, args);

        Vertx vertx = context.getBean(Vertx.class);
        WebSocketVertical webSocketVertical = context.getBean(WebSocketVertical.class);

        vertx.deployVerticle(webSocketVertical);
    }

}
