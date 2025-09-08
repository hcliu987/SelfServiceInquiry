package com.hc.wx.mp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * 搜索结果存储服务
 * 用于存储搜索结果并生成短链接
 */
@Service
@Slf4j
public class ResultStorageService {
    
    private final Map<String, String> resultStorage = new ConcurrentHashMap<>();
    
    /**
     * 存储搜索结果并返回唯一标识符
     * @param content 要存储的搜索结果内容
     * @return 存储内容的唯一标识符
     */
    public String storeResult(String content) {
        String key = generateUniqueKey();
        resultStorage.put(key, content);
        log.info("存储搜索结果，key: {}", key);
        return key;
    }
    
    /**
     * 根据key获取搜索结果
     * @param key 唯一标识符
     * @return 存储的内容，如果不存在则返回null
     */
    public String getResult(String key) {
        return resultStorage.get(key);
    }
    
    /**
     * 生成短的唯一标识符
     * @return 8位唯一标识符
     */
    private String generateUniqueKey() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}