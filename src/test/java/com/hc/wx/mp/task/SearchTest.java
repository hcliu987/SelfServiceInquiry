package com.hc.wx.mp.task;

import com.hc.wx.mp.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SearchTest {

    @Autowired
    private SearchService service;

    @Autowired
    RedisTemplate redisTemplate;
    @Test
    public void test() throws Exception {
        redisTemplate.delete("lottery:qEnhyuDqQAcAtCKRCBWJ4e");
    }
}
