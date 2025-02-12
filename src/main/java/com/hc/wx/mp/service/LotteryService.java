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
    public String getLastExpect() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url("https://www.mxnzp.com/api/lottery/common/latest?code=ssq&app_id=vinnsglluwk0brol&app_secret=HbKwaYgoIr1DhFFZ9rFoHEHhHZB1bYUT").header("Accept", "*/*").header("Host", "www.mxnzp.com").header("Connection", "keep-alive").build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("请求失败: {}", response.code());
                return null;
            }

            String responseBody = response.body().string();
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);
            //{"code":1,"msg":"数据返回成功！","data":{"openCode":"07,11,13,18,27,31+11","code":"ssq","expect":"2025012","name":"双色球","time":"2025-02-06 21:15:00"}}
            if (jsonResult.getInt("code") == 1) {
                String expect = jsonResult.getJSONObject("data").getStr("expect");
                log.info("获取最新期号成功：{}", expect);
                return expect;
            }

            log.error("获取期号失败：{}", jsonResult.getStr("msg"));
            return null;

        } catch (Exception e) {
            log.error("获取期号异常：", e);
            return null;
        }
    }

    public CheckResult checkWining(String expect, String number) {
        //{
        //    "code": 1,
        //    "msg": "数据返回成功",
        //    "data": {
        //        "resultList": [
        //            {
        //                "num": "13",
        //                "lottery": true,
        //                "blue": false
        //            },
        //            ...这里只展示一条...
        //        ],
        //        "resultDetails": "一等奖，奖金跟随奖池浮动",
        //        "resultDesc": "5+2",
        //        "openCode": "13,19,28,30,33+02+12",
        //        "checkedCode": "13,19,28,30,33@02,12",
        //        "expect": "19090",
        //        "code": "ssq",
        //        "codeValue": "双色球"
        //    }
        //}
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.mxnzp.com/api/lottery/common/check?code=ssq&expect="+getLastExpect()+"&lotteryNo=" + number + "&app_id=vinnsglluwk0brol&app_secret=HbKwaYgoIr1DhFFZ9rFoHEHhHZB1bYUT"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);

            if (jsonResult.getInt("code") != 1) {
                log.error("查询中奖失败：{}", jsonResult.getStr("msg"));
                return null;
            }

            return JSONUtil.toBean(jsonResult.getJSONObject("data"), CheckResult.class);
        } catch (Exception e) {
            log.error("查询中奖异常：", e);
            return null;
        }
    }

    public void sendNotification(CheckResult result) {
        try {
            NotificationService.LotteryResult lotteryResult = new NotificationService.LotteryResult();
            lotteryResult.setOpenCode(result.getOpenCode());
            lotteryResult.setCheckedCode(result.getCheckedCode());
            lotteryResult.setResultDetails(result.getResultDetails());

            notificationService.sendLotteryNotification(lotteryResult);
            log.info("中奖通知发送成功，期号：{}", result.getExpect());
        } catch (Exception e) {
            log.error("发送中奖通知失败", e);
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