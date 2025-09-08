package com.hc.wx.mp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 结果存储服务测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "server.url=http://localhost:80",
    "api.search.base-url=http://m.kkqws.com",
    "api.search.uukk-base-url=http://uukk6.cn",
    "api.search.kkqws-token=i69"
})
class ResultStorageServiceTest {

    private ResultStorageService resultStorageService;

    @BeforeEach
    void setUp() {
        resultStorageService = new ResultStorageService();
    }

    @Test
    void testStoreAndRetrieveResult() {
        // 测试数据
        String testContent = "测试搜索结果:\nhttp://example1.com\nhttp://example2.com\n更多内容信息";
        
        // 存储结果
        String key = resultStorageService.storeResult(testContent);
        
        // 验证key不为空且长度为8
        assertNotNull(key);
        assertEquals(8, key.length());
        
        // 验证能够正确获取存储的内容
        String retrievedContent = resultStorageService.getResult(key);
        assertEquals(testContent, retrievedContent);
    }

    @Test
    void testStoreMultipleResults() {
        // 测试存储多个结果
        String content1 = "第一个搜索结果";
        String content2 = "第二个搜索结果";
        
        String key1 = resultStorageService.storeResult(content1);
        String key2 = resultStorageService.storeResult(content2);
        
        // 验证生成的key不同
        assertNotEquals(key1, key2);
        
        // 验证能够正确获取各自的内容
        assertEquals(content1, resultStorageService.getResult(key1));
        assertEquals(content2, resultStorageService.getResult(key2));
    }

    @Test
    void testGetNonExistentResult() {
        // 测试获取不存在的结果
        String nonExistentKey = "12345678";
        String result = resultStorageService.getResult(nonExistentKey);
        
        assertNull(result);
    }

    @Test
    void testKeyUniqueness() {
        // 测试key的唯一性
        String content = "测试内容";
        String key1 = resultStorageService.storeResult(content);
        String key2 = resultStorageService.storeResult(content);
        
        // 即使内容相同，key也应该不同
        assertNotEquals(key1, key2);
    }
}