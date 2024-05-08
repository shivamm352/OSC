package com.springosc.user.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-server}")
    private String bootStrapServers;

    @Value("${spring.kafka.topic.user-data}")
    private String userdata;

    @Value("${spring.kafka.topic.session-stream}")
    private String sessionStream;

    @Value("${spring.kafka.topic.user-password-reset}")
    private String userPasswordReset;

    @Bean
    public NewTopic userdata() {
        return new NewTopic("user-data", 1, (short) 1);
    }

    @Bean
    public NewTopic sessionStream(){
        return new NewTopic("session-stream", 1, (short) 1);
    }

    @Bean
    public NewTopic userPasswordReset(){
        return new NewTopic("user-password-reset", 1,(short)1);
    }



}
