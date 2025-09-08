package com.hc.wx.mp.test;

import com.hc.wx.mp.service.ResultStorageService;
import com.hc.wx.mp.service.UrlService;

/**
 * 简单的短链接功能测试
 * 不依赖Spring容器，可以直接运行
 */
public class SimpleShortLinkTest {
    
    public static void main(String[] args) {
        System.out.println("=== 简单短链接测试开始 ===");
        
        // 创建服务实例
        ResultStorageService storageService = new ResultStorageService();
        UrlService urlService = new UrlService();
        
        // 设置测试URL（通过反射）
        try {
            var field = UrlService.class.getDeclaredField("serverUrl");
            field.setAccessible(true);
            field.set(urlService, "http://localhost:80");
        } catch (Exception e) {
            System.err.println("设置服务器URL失败: " + e.getMessage());
        }
        
        // 测试1：基本功能
        System.out.println("\n--- 测试1: 基本存储和URL生成 ---");
        testBasicFunctionality(storageService, urlService);
        
        // 测试2：多个结果
        System.out.println("\n--- 测试2: 多个结果存储 ---");
        testMultipleResults(storageService, urlService);
        
        // 测试3：边界情况
        System.out.println("\n--- 测试3: 边界情况测试 ---");
        testEdgeCases(storageService, urlService);
        
        System.out.println("\n=== 所有测试完成 ===");
    }
    
    private static void testBasicFunctionality(ResultStorageService storageService, UrlService urlService) {
        String content = "测试搜索结果:\nhttp://example.com\n更多内容...";
        
        // 存储结果
        String key = storageService.storeResult(content);
        System.out.println("生成Key: " + key);
        System.out.println("Key长度: " + key.length() + " (预期8位)");
        
        // 获取结果
        String retrieved = storageService.getResult(key);
        boolean contentMatch = content.equals(retrieved);
        System.out.println("内容匹配: " + (contentMatch ? "✅ 成功" : "❌ 失败"));
        
        // 生成URL
        String url = urlService.generateResultUrl(key);
        System.out.println("生成URL: " + url);
        System.out.println("URL包含key: " + (url.contains(key) ? "✅ 成功" : "❌ 失败"));
        
        // 短链接处理
        String shortUrl = urlService.shortenUrl(url);
        System.out.println("短链接: " + shortUrl);
    }
    
    private static void testMultipleResults(ResultStorageService storageService, UrlService urlService) {
        String content1 = "第一个搜索结果";
        String content2 = "第二个搜索结果";
        
        String key1 = storageService.storeResult(content1);
        String key2 = storageService.storeResult(content2);
        
        System.out.println("Key1: " + key1);
        System.out.println("Key2: " + key2);
        System.out.println("Key唯一性: " + (!key1.equals(key2) ? "✅ 成功" : "❌ 失败"));
        
        String url1 = urlService.generateResultUrl(key1);
        String url2 = urlService.generateResultUrl(key2);
        System.out.println("URL唯一性: " + (!url1.equals(url2) ? "✅ 成功" : "❌ 失败"));
        
        // 验证内容正确性
        boolean content1Match = content1.equals(storageService.getResult(key1));
        boolean content2Match = content2.equals(storageService.getResult(key2));
        System.out.println("内容1正确: " + (content1Match ? "✅ 成功" : "❌ 失败"));
        System.out.println("内容2正确: " + (content2Match ? "✅ 成功" : "❌ 失败"));
    }
    
    private static void testEdgeCases(ResultStorageService storageService, UrlService urlService) {
        // 测试空内容
        String emptyContent = "";
        String emptyKey = storageService.storeResult(emptyContent);
        System.out.println("空内容存储: " + (emptyKey != null ? "✅ 成功" : "❌ 失败"));
        
        // 测试不存在的key
        String nonExistentKey = "invalid123";
        String result = storageService.getResult(nonExistentKey);
        System.out.println("不存在key处理: " + (result == null ? "✅ 成功" : "❌ 失败"));
        
        // 测试大内容
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeContent.append("这是第").append(i).append("行内容\\n");
        }
        String largeKey = storageService.storeResult(largeContent.toString());
        System.out.println("大内容存储: " + (largeKey != null ? "✅ 成功" : "❌ 失败"));
        
        // 测试特殊字符
        String specialContent = "包含特殊字符: @#$%^&*()_+{}|:<>?[];',./";
        String specialKey = storageService.storeResult(specialContent);
        String retrievedSpecial = storageService.getResult(specialKey);
        System.out.println("特殊字符处理: " + (specialContent.equals(retrievedSpecial) ? "✅ 成功" : "❌ 失败"));
    }
}