package com.hc.wx.mp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * URL生成服务测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "server.url=http://test.example.com",
    "api.search.base-url=http://m.kkqws.com",
    "api.search.uukk-base-url=http://uukk6.cn",
    "api.search.kkqws-token=i69"
})
class UrlServiceTest {

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService();
        // 设置测试用的服务器URL
        ReflectionTestUtils.setField(urlService, "serverUrl", "http://test.example.com");
    }

    @Test
    void testGenerateResultUrl() {
        // 测试数据
        String testKey = "abc12345";
        
        // 生成URL
        String resultUrl = urlService.generateResultUrl(testKey);
        
        // 验证URL格式正确
        String expectedUrl = "http://test.example.com/result/" + testKey;
        assertEquals(expectedUrl, resultUrl);
    }

    @Test
    void testGenerateResultUrlWithDifferentKeys() {
        // 测试不同key生成不同URL
        String key1 = "key12345";
        String key2 = "key67890";
        
        String url1 = urlService.generateResultUrl(key1);
        String url2 = urlService.generateResultUrl(key2);
        
        assertNotEquals(url1, url2);
        assertTrue(url1.contains(key1));
        assertTrue(url2.contains(key2));
    }

    @Test
    void testShortenUrl() {
        // 测试短链接功能（目前返回原URL）
        String longUrl = "http://test.example.com/result/abc12345";
        String shortUrl = urlService.shortenUrl(longUrl);
        
        // 目前实现是直接返回原URL
        assertEquals(longUrl, shortUrl);
    }

    @Test
    void testUrlServiceWithDefaultServerUrl() {
        // 测试默认服务器URL
        UrlService defaultUrlService = new UrlService();
        ReflectionTestUtils.setField(defaultUrlService, "serverUrl", "http://localhost:80");
        
        String testKey = "test1234";
        String resultUrl = defaultUrlService.generateResultUrl(testKey);
        
        String expectedUrl = "http://localhost:80/result/" + testKey;
        assertEquals(expectedUrl, resultUrl);
    }
}