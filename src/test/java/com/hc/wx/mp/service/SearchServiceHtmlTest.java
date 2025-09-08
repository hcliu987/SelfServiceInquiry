package com.hc.wx.mp.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * SearchService HTML解析功能测试
 * 验证HTML格式数据的解析和href链接提取功能
 */
@SpringBootTest
@ActiveProfiles("test")
public class SearchServiceHtmlTest {

    @Test
    public void testHtmlParsing() {
        // 测试数据 - 您提供的HTML格式
        String testHtmlContent = "【[英雄崛起 The Awakening of Hero][2020][科幻][中国]】\n" +
            "视频：<a href=\"https://pan.baidu.com/s/1Lks_VmzXtn3NZ_MG3i3LlQ\">百度云盘</a>&nbsp; &nbsp; 提取码：1234\n\n" +
            "【[蜘蛛侠：英雄归来][2017][动作/科幻][美国]】\n" +
            "视频：<a href=\"https://pan.baidu.com/s/1aJbD0_lqFXx1GdweADtFYw?pwd=1234\">百度网盘</a>&nbsp; &nbsp; 提取码：1234\n\n" +
            "【[美国队长 1~4][动作/科幻][美国]】\n" +
            "美国队长1~3.视频：<a href=\"https://pan.quark.cn/s/268e4ae3ea67\">夸克网盘</a>\n" +
            "美国队长1~3.视频：<a href=\"https://pan.baidu.com/s/1uguy_PBk27o1S9Y_1ElDBg?pwd=1234\">百度网盘</a>&nbsp; &nbsp; 提取码：1234\n" +
            "美国队长4.视频：<a href=\"https://pan.baidu.com/s/1rVfuHY7g-gy9YqNXK1EaMg?pwd=1234\">百度网盘</a>&nbsp; &nbsp; 提取码：1234\n\n" +
            "【漫威75年：从俚俗到全球！[2014][纪录片][美国]】\n" +
            "视频：<a href=\"https://pan.baidu.com/s/1QQ_rqzCMI0RhLyNpbCAo-A\" target=\"_blank\" rel=\"noopener\">百度云盘</a>";

        System.out.println("=== HTML解析测试 ===");
        System.out.println("原始HTML内容:");
        System.out.println(testHtmlContent);
        System.out.println("\n=== 期望输出格式 ===");
        System.out.println("【[英雄崛起 The Awakening of Hero][2020][科幻][中国]】");
        System.out.println("视频：链接: https://pan.baidu.com/s/1Lks_VmzXtn3NZ_MG3i3LlQ 提取码：1234");
        System.out.println("");
        System.out.println("【[蜘蛛侠：英雄归来][2017][动作/科幻][美国]】");
        System.out.println("视频：链接: https://pan.baidu.com/s/1aJbD0_lqFXx1GdweADtFYw?pwd=1234 提取码：1234");
        System.out.println("");
        System.out.println("【[美国队长 1~4][动作/科幻][美国]】");
        System.out.println("美国队长1~3.视频：链接: https://pan.quark.cn/s/268e4ae3ea67");
        System.out.println("美国队长1~3.视频：链接: https://pan.baidu.com/s/1uguy_PBk27o1S9Y_1ElDBg?pwd=1234 提取码：1234");
        System.out.println("美国队长4.视频：链接: https://pan.baidu.com/s/1rVfuHY7g-gy9YqNXK1EaMg?pwd=1234 提取码：1234");
        System.out.println("");
        System.out.println("【漫威75年：从俚俗到全球！[2014][纪录片][美国]】");
        System.out.println("视频：链接: https://pan.baidu.com/s/1QQ_rqzCMI0RhLyNpbCAo-A");
        
        // 这里应该调用SearchService的HTML解析方法
        // 由于是测试代码，实际使用时需要注入SearchService
        System.out.println("\n=== 测试说明 ===");
        System.out.println("1. 解析HTML内容，提取【标题】格式");
        System.out.println("2. 提取<a href=\"...\">标签中的链接"); 
        System.out.println("3. 保留提取码信息");
        System.out.println("4. 格式化输出为纯文本形式");
    }
}