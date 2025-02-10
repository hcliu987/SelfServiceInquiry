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
import java.util.stream.Collectors;

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
    private static final int BATCH_SIZE = 5;
    private static final String ENV_NAME = "sfsyUrl";
    private AtomicInteger currentHour = new AtomicInteger(0);

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetHourCounter() {
        currentHour.set(0);
        log.info("重置小时计数器");
    }

    @Scheduled(cron = "0 0 * * * *")
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
        if (batchData == null || batchData.isEmpty()) {
            log.warn("批次数据为空，跳过处理");
            return;
        }
        
        String processedData = batchData.stream()
                .map(url -> url.replaceAll("^\"|\"$", ""))
                .collect(Collectors.joining("\n"));
        
        try {
            qinglongService.deleteEnv(ENV_NAME);
            qinglongService.updateEnv(ENV_NAME, processedData, "顺丰链接");
            log.info("成功更新顺丰链接环境变量，处理数据条数：{}，数据内容：{}\n", batchData.size(), processedData);
        } catch (Exception e) {
            log.error("处理顺丰链接失败，数据条数：{}，错误原因：{}", batchData.size(), e.getMessage(), e);
            throw new RuntimeException("更新顺丰链接环境变量失败", e);
        }
    }
}