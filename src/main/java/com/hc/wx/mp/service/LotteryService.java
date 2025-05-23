package com.hc.wx.mp.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class LotteryService {

    @Autowired
    private NotificationService notificationService;

    public String getLastExpect(String type) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://www.mxnzp.com/api/lottery/common/latest?code=" + type
                + "&app_id=vinnsglluwk0brol&app_secret=HbKwaYgoIr1DhFFZ9rFoHEHhHZB1bYUT";
        log.info("开始获取{}最新期号，请求URL: {}", type, url);

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "*/*")
                .header("Host", "www.mxnzp.com")
                .header("Connection", "keep-alive")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("请求失败: {}, URL: {}", response.code(), url);
                return null;
            }

            String responseBody = response.body().string();
            log.debug("获取期号响应数据: {}", responseBody);
            
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);
            if (jsonResult.getInt("code") == 1) {
                String expect = jsonResult.getJSONObject("data").getStr("expect");
                log.info("获取最新期号成功：{}, 彩票类型: {}", expect, type);
                return expect;
            }

            log.error("获取期号失败：{}, URL: {}", jsonResult.getStr("msg"), url);
            return null;

        } catch (Exception e) {
            log.error("获取期号异常，URL: {}", url, e);
            return null;
        }
    }

    public CheckResult checkWining(String expect, String number, String code) {
        String url = "https://www.mxnzp.com/api/lottery/common/check?code=" + code 
                + "&expect=" + expect 
                + "&lotteryNo=" + number
                + "&app_id=vinnsglluwk0brol&app_secret=HbKwaYgoIr1DhFFZ9rFoHEHhHZB1bYUT";
        
        log.info("开始查询中奖信息，期号: {}, 号码: {}, 彩种: {}", expect, number, code);
        log.debug("中奖查询请求URL: {}", url);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            log.debug("中奖查询响应数据: {}", responseBody);
            
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);

            if (jsonResult.getInt("code") != 1) {
                log.error("查询中奖失败：{}, URL: {}", jsonResult.getStr("msg"), url);
                return null;
            }
            
            CheckResult result = JSONUtil.toBean(jsonResult.getJSONObject("data"), CheckResult.class);
            log.info("中奖查询成功，期号: {}, 开奖号码: {}, 投注号码: {}, 中奖结果: {}", 
                    result.getExpect(), result.getOpenCode(), result.getCheckedCode(), result.getResultDetails());
            return result;
            
        } catch (Exception e) {
            log.error("查询中奖异常，URL: {}", url, e);
            return null;
        }
    }

    public void sendNotification(CheckResult result) {
        try {
            log.info("开始发送中奖通知，期号: {}", result.getExpect());
            
            NotificationService.LotteryResult lotteryResult = new NotificationService.LotteryResult();
            lotteryResult.setOpenCode(result.getOpenCode());
            lotteryResult.setCheckedCode(result.getCheckedCode());
            lotteryResult.setResultDetails(result.getResultDetails());

            notificationService.sendLotteryNotification(lotteryResult);
            log.info("中奖通知发送成功，期号: {}, 开奖号码: {}, 投注号码: {}, 中奖结果: {}", 
                    result.getExpect(), result.getOpenCode(), result.getCheckedCode(), result.getResultDetails());
            
        } catch (Exception e) {
            log.error("发送中奖通知失败，期号: {}", result.getExpect(), e);
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