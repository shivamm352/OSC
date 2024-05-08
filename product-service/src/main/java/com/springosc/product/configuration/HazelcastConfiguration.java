package com.springosc.product.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.osc.product_cache.CategoryProductMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class HazelcastConfiguration {

    @Bean
    public Config hazelcastConfig() {
        return new Config();
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        return Hazelcast.newHazelcastInstance(hazelcastConfig());
    }

    @Bean
    public IMap<String, CategoryProductMap> productData() {
        return hazelcastInstance().getMap("productData");
    }

    @Bean
    public IMap<String, List<String>> recentlyViewedMap(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("recentlyViewedMap");
    }

    @Bean
    public IMap<String, Map<String, Integer>> cartData(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("cartData");
    }

    @Bean
    public IMap<String, Integer> viewedProductMap(HazelcastInstance hazelcastInstance){
        return hazelcastInstance.getMap("viewedProductMap");
    }


}
