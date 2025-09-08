package com.hc.wx.mp.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchService 原始数据获取方法测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    \"server.url=http://localhost:80\",
    \"api.search.base-url=http://m.kkqws.com\",
    \"api.search.uukk-base-url=http://uukk6.cn\",
    \"api.search.kkqws-token=i69\"
})
class SearchServiceRawTest {

    @Autowired
    private SearchService searchService;

    @Test
    void testSearchAndMergeRaw() {
        // 测试新的原始数据获取方法
        String testQuery = \"Spring Boot\";
        
        String result = searchService.searchAndMergeRaw(testQuery);
        
        // 验证结果不为空
        assertNotNull(result);
        
        // 验证结果包含查询关键词
        assertTrue(result.contains(testQuery));
        
        // 验证结果包含预期的格式
        assertTrue(result.contains(\"查询关键词:\"));
        assertTrue(result.contains(\"数据源数量:\"));
        assertTrue(result.contains(\"获取时间:\"));
        assertTrue(result.contains(\"原始数据:\"));
        
        System.out.println(\"=== 原始数据获取测试结果 ===\");
        System.out.println(\"查询内容: \" + testQuery);
        System.out.println(\"结果长度: \" + result.length() + \" 字符\");
        System.out.println(\"结果预览: \" + result.substring(0, Math.min(200, result.length())) + \"...\");
    }

    @Test
    void testCompareWithOriginalMethod() {
        // 对比原始方法和新方法的差异
        String testQuery = \"Java\";
        
        String originalResult = searchService.searchAndMerge(testQuery);
        String rawResult = searchService.searchAndMergeRaw(testQuery);
        
        System.out.println(\"=== 方法对比测试 ===\");
        System.out.println(\"原始方法结果长度: \" + originalResult.length());
        System.out.println(\"新方法结果长度: \" + rawResult.length());
        
        // 新方法应该包含更多原始信息
        assertTrue(rawResult.length() >= originalResult.length());
        
        // 新方法应该包含格式化信息
        assertTrue(rawResult.contains(\"数据源数量:\"));
        assertFalse(originalResult.contains(\"数据源数量:\"));
    }
    
    @Test
    void testMultipleQueries() {
        // 测试多个查询，验证日志记录
        String[] queries = {\"微信小程序\", \"Spring Boot\", \"Java开发\"};
        
        for (String query : queries) {
            System.out.println(\"\n=== 测试查询: \" + query + \" ===\");
            String result = searchService.searchAndMergeRaw(query);
            
            assertNotNull(result);
            assertTrue(result.contains(query));
            
            // 简单验证结果格式
            String[] lines = result.split(\"\n\");
            assertTrue(lines.length >= 4); // 至少包含标题行
        }
    }
}