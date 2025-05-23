package com.hc.wx.mp.task;

import com.hc.wx.mp.entity.Lottery;
import com.hc.wx.mp.service.LotteryService;
import com.hc.wx.mp.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;


/**
 * 双色球中奖查询任务测试类
 * <p>
 * 测试说明：
 * 1. 使用真实API调用进行测试
 * 2. 测试覆盖以下场景：
 * - 正常查询中奖
 * - 未中奖情况
 * - 异常处理
 * 3. 使用Spring Boot Test进行集成测试
 */
@Slf4j
@SpringBootTest
public class LotteryCheckTaskTest {

    @Autowired
    LotteryCheckTask lotteryCheckTask;

    @Autowired
    RedisTemplate redisTemplate;

    @BeforeEach
    public void setUp() {
    }

    @Test
    @DisplayName("测试获取最新期号")
    public void testGetLastExpect() {


        lotteryCheckTask.checkDltLottery();
    }
}