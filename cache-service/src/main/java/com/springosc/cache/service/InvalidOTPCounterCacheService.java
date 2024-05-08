package com.springosc.cache.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InvalidOTPCounterCacheService {

    private final IMap<String, Integer> invalidOTPMap;

    public InvalidOTPCounterCacheService(HazelcastInstance hazelcastInstance) {
        this.invalidOTPMap = hazelcastInstance.getMap("invalidOTPMap");
    }

    public void storeInvalidOTPCount(String key, Integer value) {
        invalidOTPMap.put(key, value);
        log.info("Object stored in cache with key: " + key);
    }

    public int getInvalidOTPCount(String key) {
        return invalidOTPMap.getOrDefault(key, 0);
    }

}

















//    public void incrementInvalidOTPCount(String key) {
//        int count = invalidOTPMap.getOrDefault(key, 0);
//        if (count < 3) {
//            count++;
//            invalidOTPMap.put(key, count);
//            log.info("Invalid OTP count incremented for key: {}", key);
//        } else {
//            log.error("OTP attempt limit exceeded for key: {}. Please regenerate OTP.", key);
//        }
//    }

