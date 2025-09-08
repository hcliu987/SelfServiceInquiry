package com.hc.wx.mp.task;

import com.hc.wx.mp.service.LotteryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 彩票定时任务调度器。
 * <p>
 * 负责在双色球开奖日的指定时间，自动从 Redis 读取预约任务，
 * 并调用 LotteryService 对这些任务进行处理和通知。
 */
@Component
@Slf4j
public class LotteryScheduler {

    @Autowired
    private LotteryService lotteryService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 定时检查并处理彩票开奖结果。
     * <p>
     * <b>触发时间：</b>每周二、周四、周日的晚上 22:15。
     * 这个时间点通常在官方开奖数据公布之后，确保能获取到最新的结果。
     * <p>
     * <b>工作流程：</b>
     * 1. 获取最新的彩票期号。
     * 2. 使用该期号作为 key，从 Redis 中查找所有相关的预约任务。
     * 3. 如果找到任务，则遍历每个用户的预约数据。
     * 4. 对每个用户的号码列表调用 {@link LotteryService#processLotteryForUser} 进行处理。
     * 5. 所有任务处理完毕后，从 Redis 中删除该期号的预约记录，防止重复执行。
     */
    @Scheduled(cron = "0 15 22 * * TUE,THU,SUN")
    public void checkForLotteryResults() {
        log.info("【定时任务】开始执行彩票开奖查询...");

        // 步骤 1: 获取最新的官方开奖期号
        String latestIssue = lotteryService.getLatestLotteryIssue();
        if (latestIssue == null) {
            log.warn("【定时任务】无法获取最新期号，任务中止。");
            return;
        }

        // 步骤 2: 根据期号构建 Redis key，并查找预约任务
        String key = "lottery:schedule:" + latestIssue;
        Map<Object, Object> scheduledTasks = redisTemplate.opsForHash().entries(key);

        if (scheduledTasks.isEmpty()) {
            log.info("【定时任务】期号 {} 没有发现任何预约任务。", latestIssue);
            return;
        }

        log.info("【定时任务】发现期号 {} 的 {} 个预约任务，开始处理...", latestIssue, scheduledTasks.size());

        // 步骤 3: 遍历并处理所有预约任务
        for (Map.Entry<Object, Object> entry : scheduledTasks.entrySet()) {
            String openid = (String) entry.getKey();
            // 将存储在 Redis 中的、由分号拼接的字符串还原为列表
            List<String> numberStrings = Arrays.asList(((String) entry.getValue()).split(";"));
            try {
                // 调用核心业务逻辑进行处理和通知
                lotteryService.processLotteryForUser(openid, numberStrings);
            } catch (Exception e) {
                log.error("【定时任务】处理用户 {} 的预约任务时发生错误", openid, e);
            }
        }

        // 步骤 4: 清理已完成的任务
        redisTemplate.delete(key);
        log.info("【定时任务】期号 {} 的所有预约任务处理完毕，并已从 Redis 中清除。", latestIssue);
    }
}
