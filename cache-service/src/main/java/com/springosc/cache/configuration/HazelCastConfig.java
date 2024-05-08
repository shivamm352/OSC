package com.springosc.cache.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelCastConfig {

    @Bean
    public HazelcastInstance hazelcastInstance (){
        Config config = new Config();
        return Hazelcast.newHazelcastInstance(config);
    }

}
