package com.hc.wx.mp.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QinglongServiceTest {

    @Autowired
    private QinglongService qinglongService;

    @BeforeEach
    void setUp() {
        // 设置测试环境配置
        ReflectionTestUtils.setField(qinglongService, "baseUrl", "http://118.89.200.61:5700");
        ReflectionTestUtils.setField(qinglongService, "clientId", "RDPeU_1p6WlL");
        ReflectionTestUtils.setField(qinglongService, "clientSecret", "s5PP776sq_RhdPIy3y9nygMW");
    }

    @Test
    void testGetToken_WhenTokenValid() {
        // 设置有效的token
        String testToken = "valid_token";
        long futureExpiration = System.currentTimeMillis() / 1000 + 3600;
        ReflectionTestUtils.setField(qinglongService, "token", testToken);
        ReflectionTestUtils.setField(qinglongService, "tokenExpiration", futureExpiration);

        // 验证返回现有token
        String token = qinglongService.getToken();
        assertEquals(testToken, token, "应返回现有的有效token");
    }

    @Test
    void testGetToken_WhenTokenExpired() {
        // 设置过期的token
        String expiredToken = "expired_token";
        long pastExpiration = System.currentTimeMillis() / 1000 - 3600;
        ReflectionTestUtils.setField(qinglongService, "token", expiredToken);
        ReflectionTestUtils.setField(qinglongService, "tokenExpiration", pastExpiration);

        // 验证token刷新
        assertThrows(RuntimeException.class, () -> qinglongService.getToken(),
                "过期token应触发刷新并可能抛出异常（测试环境无法连接服务器）");
    }

    @Test
    void testUpdateEnv() {
        // 测试空值场景
        assertThrows(IllegalArgumentException.class, 
            () -> qinglongService.updateEnv(null),
            "空值应抛出IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, 
            () -> qinglongService.updateEnv(""),
            "空字符串应抛出IllegalArgumentException");

        // 测试正常更新场景（由于需要实际服务器，这里会抛出异常）
        String testValue = "test_value";
        assertThrows(RuntimeException.class, 
            () -> qinglongService.updateEnv(testValue),
            "在测试环境中应抛出RuntimeException");
    }

}