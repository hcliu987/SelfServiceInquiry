package com.hc.wx.mp.task;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.service.QinglongService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest
@DisplayName("顺丰数据处理测试")
class SFTaskTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private QinglongService qinglongService;

    @Autowired
    private RedisListProcessTask redisListProcessTask;

    @BeforeEach
    void setUp() {
        // 清空旧数据
        redisTemplate.delete("sf");
        
        // 准备测试数据
        List<String> testUrls = Arrays.asList(
            "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test1",
            "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test2",
            "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test3",
            "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test4",
            "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test5",
            "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test6"
        );
        
        testUrls.forEach(url -> redisTemplate.opsForList().rightPush("sf", url));
    }

    @Test
    @DisplayName("测试青龙认证")
    void testQinglongAuth() {
        String token = qinglongService.getToken();
        assert token != null && !token.isEmpty();
        log.info("青龙认证成功");
    }

    @Test
    @DisplayName("测试环境变量更新")
    void testEnvUpdate() {
        String testUrl = "https://mcs-mimp-web.sf-express.com/test";
        String envName = "SF_URL_TEST";
        qinglongService.updateEnv(envName, testUrl, "测试顺丰链接");
        log.info("环境变量更新成功");
    }

    @Test
    @DisplayName("测试单批次处理")
    void testSingleBatch() {
        redisListProcessTask.processRedisListData();
        Long remaining = redisTemplate.opsForList().size("sf");
        log.info("处理完成，剩余数据: {}", remaining);
    }

    @Test
    @DisplayName("测试完整流程")
    void testFullProcess() {
    }
}