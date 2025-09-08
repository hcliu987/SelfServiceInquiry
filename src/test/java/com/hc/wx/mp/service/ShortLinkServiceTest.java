package com.hc.wx.mp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 短链接服务集成测试
 * 测试 ResultStorageService 和 UrlService 的协作
 */
class ShortLinkServiceTest {

    @Mock
    private com.hc.wx.mp.config.ApiConfig apiConfig;

    @InjectMocks
    private ResultStorageService resultStorageService;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resultStorageService = new ResultStorageService();
        urlService = new UrlService();
        
        // 通过反射设置 UrlService 的 serverUrl 字段
        try {
            var field = UrlService.class.getDeclaredField("serverUrl");
            field.setAccessible(true);
            field.set(urlService, "http://localhost:80");
        } catch (Exception e) {
            // 忽略反射异常，使用默认值
        }
    }

    @Test
    void testShortLinkWorkflow() {
        // 1. 存储搜索结果
        String content = "测试搜索结果:\nhttp://example.com\n更多内容...";
        String key = resultStorageService.storeResult(content);
        
        // 2. 验证存储成功
        assertNotNull(key);
        assertEquals(8, key.length());
        assertEquals(content, resultStorageService.getResult(key));
        
        // 3. 生成短链接
        String url = urlService.generateResultUrl(key);
        assertNotNull(url);
        assertTrue(url.contains("/result/" + key));
        
        // 4. 测试短链接处理
        String shortUrl = urlService.shortenUrl(url);
        assertEquals(url, shortUrl); // 目前直接返回原URL
    }

    @Test
    void testMultipleResultsStorage() {
        String content1 = "第一个结果";
        String content2 = "第二个结果";
        
        String key1 = resultStorageService.storeResult(content1);
        String key2 = resultStorageService.storeResult(content2);
        
        // 验证key唯一性
        assertNotEquals(key1, key2);
        
        // 验证内容正确性
        assertEquals(content1, resultStorageService.getResult(key1));
        assertEquals(content2, resultStorageService.getResult(key2));
        
        // 验证URL唯一性
        String url1 = urlService.generateResultUrl(key1);
        String url2 = urlService.generateResultUrl(key2);
        assertNotEquals(url1, url2);
    }

    @Test
    void testInvalidKeyHandling() {
        // 测试不存在的key
        String invalidKey = "invalid1";
        assertNull(resultStorageService.getResult(invalidKey));
        
        // 但仍可以为无效key生成URL（这是正常行为）
        String url = urlService.generateResultUrl(invalidKey);
        assertTrue(url.contains("/result/" + invalidKey));
    }
}