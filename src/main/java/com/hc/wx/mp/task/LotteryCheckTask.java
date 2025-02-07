package com.hc.wx.mp.task;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.service.NotificationService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class LotteryCheckTask {

    @Value("${lottery.APPID}")
    private String appId;

    @Value("${lottery.APPSECRET}")
    private String appSecret;

    @Autowired
    private NotificationService notificationService;

    private static final String API_BASE_URL = "https://www.mxnzp.com/api/lottery/common";
    private static final String LOTTERY_CODE = "ssq";
    private static final OkHttpClient okHttpClient = new OkHttpClient();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Scheduled(cron = "0 0 23 ? * 2,4,7")
    public void checkLottery() {
        try {
            validateNotificationService();
            log.info("开始执行双色球中奖金额查询");
            
            String expect = getLastExpect();
            if (expect == null || expect.isEmpty()) {
                log.error("获取开奖信息失败");
                return;
            }
            
            List<String> numbers = Arrays.asList("11,13,17,20,23,31|11", "01,04,16,17,21,25|06");
            numbers.forEach(number -> processLotteryNumber(expect, number));
            
        } catch (Exception e) {
            log.error("双色球中奖查询任务执行失败", e);
        }
    }

    private void validateNotificationService() {
        if (notificationService == null) {
            log.error("通知服务未初始化");
            throw new IllegalStateException("通知服务未初始化");
        }
    }

    private void processLotteryNumber(String expect, String number) {
        try {
            String formattedNumber = number.replace("|", "@");
            CheckResult result = checkWining(expect, formattedNumber);
            if (result != null) {
                sendNotification(result);
            }
        } catch (Exception e) {
            log.error("处理彩票号码失败: number={}", number, e);
        }
    }

    public String getLastExpect() {
        Request request = new Request.Builder()
                .url(String.format("%s/aim_lottery?expect=2025012&code=%s&app_id=%s&app_secret=%s",
                        API_BASE_URL, LOTTERY_CODE, appId, appSecret))
                .header("Accept", "*/*")
                .header("Host", "www.mxnzp.com")
                .header("Connection", "keep-alive")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("请求失败: {}", response.code());
                return null;
            }

            String responseBody = response.body().string();
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);
            
            if (jsonResult.getInt("code") == 1) {
                String expect = jsonResult.getJSONObject("data").getStr("expect");
                log.info("获取最新期号成功：{}", expect);
                return expect;
            }

            log.error("获取期号失败：{}", jsonResult.getStr("msg"));
            return null;

        } catch (Exception e) {
            log.error("获取期号异常", e);
            return null;
        }
    }

    private CheckResult checkWining(String expect, String number) {
        String url = String.format("%s/check?code=%s&expect=%s&lotteryNo=%s&app_id=%s&app_secret=%s",
                API_BASE_URL, LOTTERY_CODE, expect, number, appId, appSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);

            if (jsonResult.getInt("code") != 1) {
                log.error("查询中奖失败：{}", jsonResult.getStr("msg"));
                return null;
            }

            return JSONUtil.toBean(jsonResult.getJSONObject("data"), CheckResult.class);
        } catch (Exception e) {
            log.error("查询中奖异常：number={}", number, e);
            return null;
        }
    }

    private void sendNotification(CheckResult result) {
        try {
            NotificationService.LotteryResult lotteryResult = new NotificationService.LotteryResult();
            lotteryResult.setOpenCode(result.getOpenCode());
            lotteryResult.setCheckedCode(result.getCheckedCode());
            lotteryResult.setResultDetails(result.getResultDetails());

            notificationService.sendLotteryNotification(lotteryResult);
            log.info("中奖通知发送成功，期号：{}", result.getExpect());
        } catch (Exception e) {
            log.error("发送中奖通知失败：期号={}", result.getExpect(), e);
        }
    }

    @Data
    static class CheckResponse {
        private Integer code;
        private String msg;
        private CheckResult data;
    }

    @Data
    public static class CheckResult {
        private String expect;
        private String openCode;
        private String checkedCode;
        private String resultDetails;
    }
}