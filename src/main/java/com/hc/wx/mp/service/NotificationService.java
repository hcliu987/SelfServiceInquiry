package com.hc.wx.mp.service;

import cn.hutool.http.HttpUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {
    
    @Value("${notification.bark.key}")
    private String barkKey;
    
    @Value("${notification.ftqq.key}")
    private String ftqqKey;
    
    @Value("${notification.pushdeer.key}")
    private String pushdeerKey;
    
    @Value("${notification.pushplus.token}")
    private String pushplusToken;

    public void sendLotteryNotification(LotteryResult result) {
        try {
            // 构建通知内容
            String title = "双色球中奖通知";
            String content = String.format("本期福利双色球号码:%s\n购买号码:%s\n中奖金额：%s",
                    result.getOpenCode(),
                    result.getCheckedCode(),
                    result.getResultDetails());

            // 并行发送所有通知
            sendBarkNotification(title, content);
            sendFtqqNotification(title, content);
            sendPushdeerNotification(title, content);
            sendPushplusNotification(title, content);
            
            log.info("所有通知发送完成");
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }

    private void sendBarkNotification(String title, String content) {
        try {
            String url = String.format("https://api.day.app/%s/%s/%s?icon=https://pic.nximg.cn/file/20220825/12380842_233542920105_2.jpg",
                    barkKey, title, content);
            HttpUtil.get(url);
            log.info("Bark通知发送成功");
        } catch (Exception e) {
            log.error("Bark通知发送失败", e);
        }
    }

    private void sendFtqqNotification(String title, String content) {
        try {
            String serverUrl = "https://sctapi.ftqq.com/" + ftqqKey + ".send";
            Map<String, Object> params = new HashMap<>();
            params.put("text", title);
            params.put("desp", content);
            HttpUtil.post(serverUrl, params);
            log.info("方糖通知发送成功");
        } catch (Exception e) {
            log.error("方糖通知发送失败", e);
        }
    }

    private void sendPushdeerNotification(String title, String content) {
        try {
            String url = String.format("https://api2.pushdeer.com/message/push?pushkey=%s&text=%s",
                    pushdeerKey, title + "\n" + content);
            HttpUtil.get(url);
            log.info("PushDeer通知发送成功");
        } catch (Exception e) {
            log.error("PushDeer通知发送失败", e);
        }
    }

    private void sendPushplusNotification(String title, String content) {
        try {
            String url = String.format("http://www.pushplus.plus/send?token=%s&title=%s&content=%s",
                    pushplusToken, title, content);
            HttpUtil.get(url);
            log.info("PushPlus通知发送成功");
        } catch (Exception e) {
            log.error("PushPlus通知发送失败", e);
        }
    }

    @Data
    public static class LotteryResult {
        private String openCode;
        private String checkedCode;
        private String resultDetails;
    }
}