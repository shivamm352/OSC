package com.springosc.webSocket.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-server}")
    private String bootStrapServers;

    @Value("${spring.kafka.topic.logout-user}")
    private String logoutUser;

    @Value("${spring.kafka.topic.product-viewed}")
    private String productViewed;

    @Value("${spring.kafka.topic.cart-data}")
    private String cartData;

    @Bean
    public NewTopic logoutUser(){
        return new NewTopic("logout-user", 1,(short) 1);
    }

    @Bean
    public NewTopic productViewed(){
        return new NewTopic("product-viewed", 1,(short) 1);
    }

    @Bean
    public NewTopic cartData(){
        return new NewTopic("cart-data",1, (short) 1);
    }

}
