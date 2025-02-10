package com.hc.wx.mp.task;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.service.QinglongService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest
@DisplayName("顺丰数据处理测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SFTaskTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private QinglongService qinglongService;

    @Autowired
    private RedisListProcessTask redisListProcessTask;

    private static final String REDIS_KEY = "sf";
    private static final List<String> TEST_URLS = Arrays.asList(
        "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test1",
        "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test2",
        "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test3",
        "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test4",
        "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test5",
        "https://mcs-mimp-web.sf-express.com/mcs-mimp/share/app/activityRedirect?sign=test6"
    );

    @BeforeEach
    void setUp() {
        cleanupRedisData();
        prepareTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupRedisData();
    }

    private void cleanupRedisData() {
        redisTemplate.delete(REDIS_KEY);
        log.info("清理Redis测试数据，key: {}", REDIS_KEY);
    }

    private void prepareTestData() {
        TEST_URLS.forEach(url -> redisTemplate.opsForList().rightPush(REDIS_KEY, url));
        log.info("准备测试数据，key: {}, 共{}条", REDIS_KEY, TEST_URLS.size());
    }

    @Test
    @Order(1)
    @DisplayName("测试青龙认证 - 正常场景")
    void testQinglongAuth_Success() {
        String token = qinglongService.getToken();
        Assertions.assertNotNull(token, "获取token不应为空");
        Assertions.assertFalse(token.isEmpty(), "获取的token不应为空字符串");
        log.info("青龙认证成功，token: {}", token);
    }

    @Test
    @Order(2)
    @DisplayName("测试环境变量更新 - 正常场景")
    void testEnvUpdate_Success() {
        String testUrl = "https://mcs-mimp-web.sf-express.com/test";
        Assertions.assertDoesNotThrow(() -> {
            qinglongService.updateEnv(testUrl);
        }, "更新环境变量不应抛出异常");
        log.info("环境变量更新成功");
    }

    @Test
    @Order(3)
    @DisplayName("测试环境变量更新 - 空值场景")
    void testEnvUpdate_EmptyValue() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            qinglongService.updateEnv("");
        }, "空值应该抛出IllegalArgumentException");
        log.info("空值测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("测试Redis List数据处理 - 正常场景")
    void testRedisListProcess_Success() {
        Long initialSize = redisTemplate.opsForList().size(REDIS_KEY);
        Assertions.assertEquals(TEST_URLS.size(), initialSize, "初始数据量应该等于测试数据量");

        // 验证Redis List中的数据
        List<String> storedUrls = redisTemplate.opsForList().range(REDIS_KEY, 0, -1);
        Assertions.assertNotNull(storedUrls, "Redis中的数据不应为空");
        Assertions.assertEquals(TEST_URLS.size(), storedUrls.size(), "Redis中的数据数量应该与测试数据相同");
        Assertions.assertTrue(storedUrls.containsAll(TEST_URLS), "Redis中的数据应该包含所有测试URL");

        redisListProcessTask.processRedisListData();
        
        Long remainingSize = redisTemplate.opsForList().size(REDIS_KEY);
        Assertions.assertEquals(initialSize, remainingSize, "处理后数据量应保持不变");
        log.info("Redis List数据处理成功，key: {}, 处理数据量: {}", REDIS_KEY, initialSize);
    }

    @Test
    @Order(5)
    @DisplayName("测试完整流程 - 正常场景")
    void testFullProcess_Success() {
        redisListProcessTask.resetHourCounter();
        
        Long initialSize = redisTemplate.opsForList().size(REDIS_KEY);
        Assertions.assertEquals(TEST_URLS.size(), initialSize, "初始数据量应该等于测试数据量");
        
        redisListProcessTask.processRedisListData();
        
        Long finalSize = redisTemplate.opsForList().size(REDIS_KEY);
        Assertions.assertEquals(initialSize, finalSize, "处理后数据量应保持不变");
        log.info("完整流程测试成功，key: {}, 初始数据量: {}, 最终数据量: {}", REDIS_KEY, initialSize, finalSize);
    }

    @Test
    @Order(6)
    @DisplayName("测试空Redis List处理")
    void testEmptyRedisList() {
        cleanupRedisData();
        
        Assertions.assertDoesNotThrow(() -> {
            redisListProcessTask.processRedisListData();
        }, "处理空列表不应抛出异常");
        
        Long size = redisTemplate.opsForList().size(REDIS_KEY);
        Assertions.assertEquals(0, size, "空列表的大小应该为0");
        log.info("空列表处理测试成功，key: {}", REDIS_KEY);
    }
}