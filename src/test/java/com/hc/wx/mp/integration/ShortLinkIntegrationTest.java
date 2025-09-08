package com.hc.wx.mp.integration;

import com.hc.wx.mp.service.ResultStorageService;
import com.hc.wx.mp.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 短链接功能集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "server.url=http://localhost",
    "api.search.base-url=http://m.kkqws.com",
    "api.search.uukk-base-url=http://uukk6.cn",
    "api.search.kkqws-token=i69"
})
class ShortLinkIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ResultStorageService resultStorageService;

    @Autowired
    private UrlService urlService;

    @Test
    void testCompleteShortLinkWorkflow() {
        // 1. 创建测试搜索结果
        String testContent = "集成测试搜索结果:\n" +
                            "http://example1.com/article1\n" +
                            "http://example2.com/article2\n" +
                            "更多相关信息...";

        // 2. 存储结果并生成短链接
        String resultKey = resultStorageService.storeResult(testContent);
        String resultUrl = urlService.generateResultUrl(resultKey);

        // 3. 验证key和URL格式
        assertNotNull(resultKey);
        assertEquals(8, resultKey.length());
        assertTrue(resultUrl.contains("/result/" + resultKey));

        // 4. 通过HTTP请求访问短链接
        String fullUrl = "http://localhost:" + port + "/result/" + resultKey;
        ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);

        // 5. 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 验证页面包含预期内容
        assertTrue(responseBody.contains("搜索结果"));
        assertTrue(responseBody.contains("example1.com"));
        assertTrue(responseBody.contains("example2.com"));
        assertTrue(responseBody.contains(resultKey));
    }

    @Test
    void testAccessNonExistentShortLink() {
        // 测试访问不存在的短链接
        String nonExistentKey = "99999999";
        String fullUrl = "http://localhost:" + port + "/result/" + nonExistentKey;
        
        ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 应该显示错误页面
        assertTrue(responseBody.contains("结果不存在或已过期") || 
                   responseBody.contains("error"));
    }

    @Test
    void testMultipleShortLinks() {
        // 测试多个短链接的并发访问
        String content1 = "第一个搜索结果\nhttp://site1.com";
        String content2 = "第二个搜索结果\nhttp://site2.com";
        
        String key1 = resultStorageService.storeResult(content1);
        String key2 = resultStorageService.storeResult(content2);
        
        // 验证两个不同的key
        assertNotEquals(key1, key2);
        
        // 分别访问两个链接
        String url1 = "http://localhost:" + port + "/result/" + key1;
        String url2 = "http://localhost:" + port + "/result/" + key2;
        
        ResponseEntity<String> response1 = restTemplate.getForEntity(url1, String.class);
        ResponseEntity<String> response2 = restTemplate.getForEntity(url2, String.class);
        
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        
        assertTrue(response1.getBody().contains("site1.com"));
        assertTrue(response2.getBody().contains("site2.com"));
    }
}