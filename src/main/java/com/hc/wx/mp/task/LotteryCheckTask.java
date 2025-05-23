package com.hc.wx.mp.task;

import cn.hutool.http.HttpUtil;
import com.hc.wx.mp.entity.Lottery;
import com.hc.wx.mp.service.LotteryService;
import com.hc.wx.mp.entity.LotteryCheckResult;
import com.hc.wx.mp.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class LotteryCheckTask {

    private static final String KEY = "lottery:";
    @Autowired
    private LotteryService lotteryService;

    @Autowired
    private RedisTemplate redisTemplate;


    // 添加公共方法处理彩票检查逻辑
    private void processLotteryCheck(String lotteryType) {
        try {
            Set keys = redisTemplate.keys(KEY + "*");
            String lastExpect = lotteryService.getLastExpect(lotteryType);
            LotteryCheckResult checkResult = new LotteryCheckResult();
            
            if (keys.size() > 0) {
                System.out.println(keys.size());
                keys.forEach(key -> {
                    Lottery lottery = (Lottery) redisTemplate.opsForValue().get(key);
                    System.out.println(lottery);
                    if (lottery != null && lottery.getNumbers().length() > 10 
                            && lottery.getStatus().equals("0") 
                            && lotteryType.equals(lottery.getType())) {
                        
                        String[] numbers = lottery.getNumbers().split("\n");
                        for (String number : numbers) {
                            number = number.replaceAll("\\s*-\\s*", "-")
                                    .replaceAll("\\s+", ",")
                                    .replace("-", "@")
                                    .replace("|", "@");
                            System.out.println(number);
                            LotteryService.CheckResult result = lotteryService.checkWining(lastExpect, number, lotteryType);
                            System.out.println(result);
                            updateCheckResult(result, checkResult);
                        }
                        
                        if (checkResult.getExpect() != null) {
                            String message = buildNotificationMessage(checkResult);
                            sendBarkNotification(lottery.getBarkId(),
                                    lotteryType.equals("ssq") ? "双色球中奖通知" : "超级大乐透中奖通知",
                                    message);
                        }

                        // 更新状态
                        String newKey = KEY + lottery.getBarkId();
                        redisTemplate.delete(key);
                        lottery.setStatus("1");
                        redisTemplate.opsForValue().set(newKey, lottery);
                        log.info("已更新彩票状态，key: {}", newKey);
                    }
                });
            }
        } catch (Exception e) {
            log.error("{}查询任务执行失败", lotteryType.equals("ssq") ? "双色球" : "大乐透", e);
        }
    }

    @Scheduled(cron = "0 15 22 ? * 2,4,7")
    public void checkSsqLottery() {
        processLotteryCheck("ssq");
    }

    @Scheduled(cron = "0 15 22 ? * 1,3,6")
    public void checkDltLottery() {
        processLotteryCheck("cjdlt");
    }

    private void updateCheckResult(LotteryService.CheckResult result, LotteryCheckResult checkResult) {
        if (result != null) {
            checkResult.setExpect(result.getExpect());
            checkResult.setOpenCode(result.getOpenCode());
            String[] openNumbers = result.getOpenCode().split("\\+");
            checkResult.setRedBalls(openNumbers[0]);
            checkResult.setBlueBall(openNumbers[1]);

            LotteryCheckResult.NumberResult numberResult = new LotteryCheckResult.NumberResult();
            numberResult.setNumber(result.getCheckedCode().replace("@", "+"));
            numberResult.setResult(result.getResultDetails());
            checkResult.getNumberResults().add(numberResult);
        }
    }

    private String buildNotificationMessage(LotteryCheckResult checkResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("开奖日期：").append(DateUtils.getDate()).append("\n");
        sb.append("开奖号码：\n");
        sb.append("红球：").append(checkResult.getRedBalls()).append("\n");
        sb.append("蓝球：").append(checkResult.getBlueBall()).append("\n");

        int i = 1;
        for (LotteryCheckResult.NumberResult nr : checkResult.getNumberResults()) {
            sb.append(String.format("号码%d（%s）：%s\n",
                    i++, nr.getNumber(), nr.getResult()));
        }
        return sb.toString();
    }

    private void sendBarkNotification(String barkId, String title, String content) {
        try {
            String url = String.format("https://api.day.app/%s/%s/%s?icon=https://pic.nximg.cn/file/20220825/12380842_233542920105_2.jpg",
                    barkId, title, content);
            HttpUtil.get(url);
            log.info("Bark通知发送成功");
        } catch (Exception e) {
            log.error("Bark通知发送失败", e);
        }
    }

}