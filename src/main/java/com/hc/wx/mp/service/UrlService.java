package com.hc.wx.mp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * URL生成服务
 */
@Service
@Slf4j
public class UrlService {
    
    @Value("${server.url:http://localhost:80}")
    private String serverUrl;
    
    /**
     * 生成搜索结果页面的完整URL
     * @param key 存储结果的唯一标识符
     * @return 访问结果的完整URL
     */
    public String generateResultUrl(String key) {
        String url = serverUrl + "/result/" + key;
        log.info("生成结果URL: {}", url);
        return url;
    }
    
    /**
     * 生成短链接（目前直接返回原URL，可以集成第三方短链接服务）
     * @param longUrl 长URL
     * @return 短链接
     */
    public String shortenUrl(String longUrl) {
        // 这里可以集成第三方短链接服务，如 bit.ly, tinyurl 等
        // 目前直接返回原URL
        return longUrl;
    }
}