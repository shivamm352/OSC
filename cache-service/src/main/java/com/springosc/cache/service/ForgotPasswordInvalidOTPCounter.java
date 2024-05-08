package com.springosc.cache.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ForgotPasswordInvalidOTPCounter {

    private final IMap<String, Integer> forgotPasswordInvalidOTPMap;

    public ForgotPasswordInvalidOTPCounter(HazelcastInstance hazelcastInstance) {
        this.forgotPasswordInvalidOTPMap = hazelcastInstance.getMap("forgotPasswordInvalidOTPMap");
    }

    public void storeInvalidOTPCount(String key, Integer value) {
        forgotPasswordInvalidOTPMap.put(key, value);
        log.info("Object stored in cache with key: " + key);
    }

    public int getInvalidOTPCount(String key) {
        return forgotPasswordInvalidOTPMap.getOrDefault(key, 0);
    }

}
