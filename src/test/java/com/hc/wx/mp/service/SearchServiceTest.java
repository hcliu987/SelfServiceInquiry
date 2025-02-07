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

    @Test
    @DisplayName("测试获取小语功能")
    void testGetXiaoyu() throws Exception {
        String result = searchService.getXiaoyu(TEST_TEXT);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        log.info("小语结果: {}", result);
    }

    @Test
    @DisplayName("测试并行搜索功能")
    void testSearchParallel() {
        String result = searchService.searchParallel(TEST_TEXT);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        log.info("并行搜索结果: {}", result);
    }

    @Test
    @DisplayName("测试搜索结果合并功能")
    void testSearchAndMerge() {
        String result = searchService.searchAndMerge(TEST_TEXT);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        log.info("合并搜索结果: {}", result);
    }

    @Test
    @DisplayName("测试性能对比")
    void testPerformanceComparison() throws Exception {
        // 预热
        searchService.searchParallel(TEST_TEXT);
        Thread.sleep(1000);

        // 记录开始时间
        long startTime;
        
        // 测试 resultMsg
        startTime = System.currentTimeMillis();
        String resultMsg = searchService.resultMsg(TEST_TEXT);
        long resultMsgTime = System.currentTimeMillis() - startTime;
        
        // 测试 searchParallel
        startTime = System.currentTimeMillis();
        String searchParallel = searchService.searchParallel(TEST_TEXT);
        long searchParallelTime = System.currentTimeMillis() - startTime;
        
        // 测试 searchAndMerge
        startTime = System.currentTimeMillis();
        String searchAndMerge = searchService.searchAndMerge(TEST_TEXT);
        long searchAndMergeTime = System.currentTimeMillis() - startTime;

        // 输出性能数据
        log.info("性能测试结果:");
        log.info("resultMsg 方法耗时: {} ms, 结果长度: {}", resultMsgTime, resultMsg.length());
        log.info("searchParallel 方法耗时: {} ms, 结果长度: {}", searchParallelTime, searchParallel.length());
        log.info("searchAndMerge 方法耗时: {} ms, 结果长度: {}", searchAndMergeTime, searchAndMerge.length());
        
        // 验证结果
        assertNotNull(resultMsg);
        assertNotNull(searchParallel);
        assertNotNull(searchAndMerge);
        assertTrue(resultMsgTime > 0);
        assertTrue(searchParallelTime > 0);
        assertTrue(searchAndMergeTime > 0);
        
        // 记录到日志文件
        log.info("性能测试详细数据:");
        log.info("测试文本: {}", TEST_TEXT);
        log.info("resultMsg 结果: {}", resultMsg);
        log.info("searchParallel 结果: {}", searchParallel);
        log.info("searchAndMerge 结果: {}", searchAndMerge);
    }


}