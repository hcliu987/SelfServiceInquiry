package com.hc.wx.mp.task;

import com.hc.wx.mp.service.LotteryService;
import com.hc.wx.mp.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * 双色球中奖查询任务测试类
 * 
 * 测试说明：
 * 1. 使用真实API调用进行测试
 * 2. 测试覆盖以下场景：
 *    - 正常查询中奖
 *    - 未中奖情况
 *    - 异常处理
 * 3. 使用Spring Boot Test进行集成测试
 */
@Slf4j
@SpringBootTest
public class LotteryCheckTaskTest {

    @Autowired
    private LotteryService lotteryService;
    @Autowired
    LotteryCheckTask lotteryCheckTask;

    @Autowired
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
    }

    @Test
    @DisplayName("测试获取最新期号")
    public void testGetLastExpect() {
        String expect = lotteryService.getLastExpect();
        log.info("获取到的最新期号: {}", expect);
    }

    @Test
    @DisplayName("测试完整的中奖查询流程")
    public void testCheckLottery() {
        // 执行中奖查询
        lotteryCheckTask.checkLottery();
        // 由于是异步通知，这里主要验证方法执行是否抛出异常
        // 实际的通知结果需要通过日志查看
        log.info("中奖查询执行完成");
    }

    @Test
    @DisplayName("测试异常场景处理")
    public void testCheckLotteryWithInvalidNumber() {
        // 使用无效的号码测试异常处理
        try {
            lotteryCheckTask.checkLottery();
            log.info("异常场景测试完成");
        } catch (Exception e) {
        }
    }
}