package com.springosc.product.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-server}")
    private String bootStrapServers;

    @Value("${spring.kafka.topic.product-data}")
    private String productData;

    @Bean
    public NewTopic userdata() {
        return new NewTopic("product-data", 1, (short) 1);
    }

}
