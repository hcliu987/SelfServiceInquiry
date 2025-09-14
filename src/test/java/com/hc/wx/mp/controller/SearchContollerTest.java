package com.hc.wx.mp.controller;

import com.hc.wx.mp.service.ResultStorageService;
import com.hc.wx.mp.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 搜索控制器测试类
 */
@WebMvcTest(SearchController.class)
class SearchContollerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResultStorageService resultStorageService;
    
    @MockBean
    private SearchService searchService;

    private String testKey;
    private String testContent;

    @BeforeEach
    void setUp() {
        testKey = "abc12345";
        testContent = "测试搜索结果:\nhttp://example1.com\nhttp://example2.com\n更多内容信息";
    }

    @Test
    void testShowResult_Success() throws Exception {
        // 模拟服务返回测试内容
        when(resultStorageService.getResult(testKey)).thenReturn(testContent);

        mockMvc.perform(get("/result/" + testKey))
                .andExpect(status().isOk())
                .andExpect(view().name("search_result"))
                .andExpect(model().attribute("content", testContent))
                .andExpect(model().attribute("key", testKey));
    }

    @Test
    void testShowResult_NotFound() throws Exception {
        // 模拟服务返回null（结果不存在）
        when(resultStorageService.getResult(testKey)).thenReturn(null);

        mockMvc.perform(get("/result/" + testKey))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("error", "结果不存在或已过期"));
    }

    @Test
    void testShowResult_WithSpecialCharacters() throws Exception {
        // 测试包含特殊字符的key
        String specialKey = "test123@";
        String specialContent = "包含特殊字符的搜索结果\n@#$%^&*()";
        
        when(resultStorageService.getResult(specialKey)).thenReturn(specialContent);

        mockMvc.perform(get("/result/" + specialKey))
                .andExpect(status().isOk())
                .andExpect(view().name("search_result"))
                .andExpect(model().attribute("content", specialContent))
                .andExpect(model().attribute("key", specialKey));
    }

    @Test
    void testOriginalShowEndpoint() throws Exception {
        // 测试原有的/res/show端点
        mockMvc.perform(get("/res/show").param("r", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("show"));
    }
}