package com.hc.wx.mp.service;

import com.hc.wx.mp.config.ApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@DisplayName("搜索服务测试")
class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ApiConfig apiConfig;

    private static final String TEST_TEXT = "漂白";

    @BeforeEach
    void setUp() {
        assertNotNull(apiConfig.getBaseUrl(), "基础URL不能为空");
        assertNotNull(apiConfig.getToken(), "Token不能为空");
    }

    @Test
    @DisplayName("测试基础搜索功能")
    void testSearch() throws Exception {
        String result = searchService.search(TEST_TEXT);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        log.info("搜索结果: {}", result);
    }

    @Test
    @DisplayName("测试获取句子功能")
    void testGetJuzi() throws Exception {
        String result = searchService.getJuzi(TEST_TEXT);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        log.info("句子结果: {}", result);
    }


}