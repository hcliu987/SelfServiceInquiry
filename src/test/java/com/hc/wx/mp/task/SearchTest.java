package com.hc.wx.mp.task;

import com.hc.wx.mp.handler.MsgHandler;
import com.hc.wx.mp.service.LotteryService;
import com.hc.wx.mp.service.SearchService;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SearchTest {

    @Autowired
    private SearchService service;

    @Autowired
    MsgHandler handler;
    @Autowired
    private LotteryService lotteryService;

//    @Autowired
//    RedisTemplate redisTemplate;
    @Test
    public void test() throws Exception {
//        redisTemplate.delete("lottery:qEnhyuDqQAcAtCKRCBWJ4e");
    }

    @Test
    public void testSearchParallel() throws Exception {
        String query = "天然子结构";
        String s = handler.performConcurrentSearch(query, "1");
        System.out.printf("", s);
    }

    @Test
    public void testLottery() throws InterruptedException {
        // 这是一个示例 openid，请替换为有效的测试 openid
        String openid = "o-R_i6-b-i-z-Z_z-Z_z-Z_z-Z_z";
        // 示例彩票号码
        List<String> numbers = Collections.singletonList("01 02 03 04 05 06-07");
        lotteryService.processLotteryForUser(openid, numbers);
        // 等待异步方法执行
        Thread.sleep(3000);
    }
}
