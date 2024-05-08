package com.springosc.cache.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResetPasswordCacheService {

    private final IMap<String, Long> forgotPasswordOtpMap;

    public ResetPasswordCacheService(HazelcastInstance hazelcastInstance) {
        this.forgotPasswordOtpMap = hazelcastInstance.getMap("forgotPasswordOtpMap");
    }

    public void storeOtpInCache(String key, long value) {
        forgotPasswordOtpMap.put(key, value);
        log.info("Object stored in cache with key: " + key);
    }

    public Long getStoredOTP(String key) {
        log.info("Object stored in cache with key: " + key);
        return forgotPasswordOtpMap.get(key);
    }

}
