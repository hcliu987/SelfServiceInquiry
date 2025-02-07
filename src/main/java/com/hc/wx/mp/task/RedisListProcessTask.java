package com.hc.wx.mp.task;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.service.QinglongService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RedisListProcessTask {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private QinglongService qinglongService;

    @Value("${qinglong.url}")
    private String baseUrl;

    private static final String REDIS_KEY = "sf";
    private static final int BATCH_SIZE = 6;
    private AtomicInteger currentHour = new AtomicInteger(0);

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetHourCounter() {
        currentHour.set(0);
        log.info("重置小时计数器");
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void processRedisListData() {
        try {
            Long listSize = redisTemplate.opsForList().size(REDIS_KEY);
            if (listSize == null || listSize == 0) {
                log.warn("Redis列表为空");
                return;
            }

            int startIndex = currentHour.get() * BATCH_SIZE;
            int endIndex;
            
            // 判断是否是最后一批数据
            if ((startIndex + BATCH_SIZE) >= listSize) {
                endIndex = listSize.intValue() - 1;
                log.info("处理最后一批数据，索引范围：{} - {}", startIndex, endIndex);
            } else {
                endIndex = startIndex + BATCH_SIZE - 1;
                log.info("处理第{}小时数据，索引范围：{} - {}", currentHour.get() + 1, startIndex, endIndex);
            }

            // 获取当前批次的数据
            List<String> batchData = redisTemplate.opsForList().range(REDIS_KEY, startIndex, endIndex);
            if (batchData != null && !batchData.isEmpty()) {
                processBatch(batchData);
            }

            currentHour.incrementAndGet();
            log.info("完成第{}小时数据处理，共处理{}条数据", currentHour.get(), batchData != null ? batchData.size() : 0);

        } catch (Exception e) {
            log.error("处理Redis列表数据失败", e);
        }
    }

    private void processBatch(List<String> batchData) {
        for (String url : batchData) {
            try {
                // 使用 URL 的哈希值作为变量名的一部分，确保唯一性
                String envName = "SF_URL_" + (Math.abs(url.hashCode()));
                
                // 直接使用 QinglongService 的 updateEnv 方法
                qinglongService.updateEnv(
                    envName,          // 环境变量名
                    url,             // 环境变量值（完整URL）
                    "顺丰链接"        // 备注信息
                );
                
                log.info("成功更新顺丰链接环境变量: {}", envName);
            } catch (Exception e) {
                log.error("处理顺丰链接失败: {}", url, e);
            }
        }
    }
}