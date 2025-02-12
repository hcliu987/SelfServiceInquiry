package com.hc.wx.mp.task;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.service.LotteryService;
import com.hc.wx.mp.service.NotificationService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class LotteryCheckTask {


    @Autowired
    private LotteryService lotteryService;


    @Scheduled(cron = "0 30 22 ? * 2,4,7")
    public void checkLottery() {
        try {
            if (lotteryService == null) {
                log.error("通知服务未初始化");
                return;
            }
            log.info("开始执行双色球中奖金额查询");
            List<String> numbers = Arrays.asList("11,13,17,20,23,31|11", "01,04,16,17,21,25|06");

            String expect = lotteryService.getLastExpect();
            System.out.println(expect);
            if (!expect.isEmpty()) {
                for (String number : numbers) {
                    number = number.replace("|", "@");
                    LotteryService.CheckResult result = lotteryService.checkWining(expect, number);
                    if (result != null) {
                        lotteryService.sendNotification(result);
                    }
                }
            } else {
                log.error("获取开奖信息失败:{}");
            }
        } catch (Exception e) {
            log.error("双色球中奖查询任务执行失败", e);
        }
    }

    
}