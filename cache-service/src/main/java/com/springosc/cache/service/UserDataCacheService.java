package com.springosc.cache.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.springosc.cache.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserDataCacheService {

    private final IMap<String, UserDTO> userMap;

    public UserDataCacheService(HazelcastInstance hazelcastInstance) {
        this.userMap = hazelcastInstance.getMap("userMap");
    }

    public void storeUserDataInCache(String key, UserDTO value) {
        userMap.put(key, value);
        log.info("Object stored in cache with key: " + key);
    }

    public UserDTO getDataFromMap(String key) {
        log.info("Object stored in cache with key: " + key);
        return userMap.get(key);
    }

}


