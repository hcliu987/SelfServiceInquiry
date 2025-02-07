package com.hc.wx.mp.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.model.CheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LotteryService {

    @Value("${lottery.APPID}")
    private String appId;

    @Value("${lottery.APPSECRET}")
    private String appSecret;

    public String getLastExpect() {
        try {
            String url = "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry?gameNo=85&provinceId=0&pageSize=1&isVerify=1&pageNo=1";
            String response = HttpRequest.get(url)
                    .header("appid", appId)
                    .header("appsecret", appSecret)
                    .execute()
                    .body();

            JSONObject jsonObject = JSONUtil.parseObj(response);
            if (jsonObject.getBool("success")) {
                return jsonObject.getJSONObject("value")
                        .getJSONArray("list")
                        .getJSONObject(0)
                        .getStr("lotteryDrawNum");
            }
            return null;
        } catch (Exception e) {
            log.error("获取最新期号失败", e);
            return null;
        }
    }

    public CheckResult checkWining(String expect, String number) {
        try {
            String url = "https://webapi.sporttery.cn/gateway/lottery/checkWining.qry";
            String response = HttpRequest.get(url)
                    .header("appid", appId)
                    .header("appsecret", appSecret)
                    .form("gameNo", "85")
                    .form("lotteryDrawNum", expect)
                    .form("numbers", number)
                    .execute()
                    .body();

            JSONObject jsonObject = JSONUtil.parseObj(response);
            if (jsonObject.getBool("success")) {
                JSONObject value = jsonObject.getJSONObject("value");
                CheckResult result = new CheckResult();
                result.setExpect(expect);
                result.setNumber(number);
                result.setWinningAmount(value.getStr("totalWinningAmount", "0"));
                return result;
            }
            return null;
        } catch (Exception e) {
            log.error("查询中奖失败: expect={}, number={}", expect, number, e);
            return null;
        }
    }

    public void sendNotification(CheckResult result) {
        try {
            if (result == null) return;
            
            String message = String.format("双色球第%s期\n号码：%s\n中奖金额：%s元", 
                    result.getExpect(), 
                    result.getNumber(), 
                    result.getWinningAmount());
            
            log.info("发送中奖通知: {}", message);
            // TODO: 实现具体的通知发送逻辑
            
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }
}