package com.hc.wx.mp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@DisplayName("彩票服务测试")
public class LotteryServiceTest {

    @Autowired
    private LotteryService lotteryService;

    @Test
    @DisplayName("获取最新彩票信息")
    void testGetLatestLotteryInfo() {
        String result = lotteryService.getLatestLotteryInfo();
        System.out.println(result);
        assertNotNull(result, "返回结果不应为 null");
        assertFalse(result.contains("失败"), "返回结果不应包含'失败'字样");
    }
}
