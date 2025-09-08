package com.hc.wx.mp.service;

import cn.hutool.http.HttpUtil;
import com.hc.wx.mp.entity.LotteryResponse;
import com.hc.wx.mp.entity.LotteryResult;
import com.hc.wx.mp.entity.PrizeGrade;
import com.hc.wx.mp.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 彩票服务类，负责处理所有与彩票相关的业务逻辑。
 * 包括：获取最新开奖信息、核对用户号码、处理中奖通知、以及管理预约查询任务。
 */
@Service
@Slf4j
public class LotteryService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${bark.key}")
    private String barkKey;

    private static final String LOTTERY_RESULT_URL = "http://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=1";
    private static final String BARK_URL_TEMPLATE = "https://api.day.app/%s";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    /**
     * 异步处理用户提交的彩票号码。
     */
    @Async
    public void processLotteryForUser(String openid, List<String> numberStrings) {
        log.info("开始为用户 {} 处理 {} 组彩票数据...", openid, numberStrings.size());
        LotteryResult winningNumbers = fetchLatestLotteryResult();
        if (winningNumbers == null) {
            log.warn("获取最新彩票数据失败，无法为用户 {} 处理。", openid);
            return;
        }

        String notificationContent = buildConsolidatedNotification(numberStrings, winningNumbers);
        System.out.println(notificationContent);
        // 通过检查通知内容中是否包含 "奖" 字来判断是否中奖
        boolean anyWins = notificationContent.contains("等奖");

        String notificationTitle = anyWins ?  "🎉恭喜您中奖啦！":"🤷‍♂️本次未中奖" ;

        sendBarkNotification(notificationTitle, notificationContent);
        log.info("为用户 {} 整合后的彩票结果通知已发送。", openid);
    }

    /**
     * 将彩票查询任务存入 Redis。
     */
    public void scheduleLotteryCheck(String openid, List<String> numberStrings, String issue) {
        String numbersData = String.join(";", numberStrings);
        String key = "lottery:schedule:" + issue;
        redisTemplate.opsForHash().put(key, openid, numbersData);
        log.info("为用户 {} 创建了期号 {} 的彩票预约任务。", openid, issue);
    }

    /**
     * 获取最新一期彩票的期号。
     */
    public String getLatestLotteryIssue() {
        LotteryResult result = fetchLatestLotteryResult();
        return (result != null) ? result.getCode() : null;
    }

    /**
     * 获取格式化后的最新一期彩票信息。
     */
    public String getLatestLotteryInfo() {
        LotteryResult result = fetchLatestLotteryResult();
        if (result != null) {
            return formatLotteryInfo(result);
        }
        return "获取最新一期彩票信息失败";
    }

    // ===================================================================================
    // 私有辅助方法
    // ===================================================================================

    private LotteryResult fetchLatestLotteryResult() {
        Request request = new Request.Builder()
                .url("http://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=1")
                .header("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                .header("Accept", "*/*")
                .header("Host", "www.cwl.gov.cn")
                .header("Connection", "keep-alive")
                .header("Referer", "http://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=1")
                .header("Cookie", "HMF_CI=5cabd3da5bbc427669409d1929b5ee59021c4775f570587b9e89e18c0830e53217873d7569ce54e50e7305343c3c44eb148db4b9935ce742568ba6e29c0c84a214")
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("请求彩票接口失败: {}", response);
                return null;
            }
            String responseStr = response.body().string();
            LotteryResponse lotteryResponse = JsonUtils.fromJson(responseStr, LotteryResponse.class);

            if (lotteryResponse != null && lotteryResponse.getState() == 0 && !CollectionUtils.isEmpty(lotteryResponse.getResult())) {
                return lotteryResponse.getResult().get(0);
            }
        } catch (Exception e) {
            log.error("请求彩票接口异常", e);
        }
        return null;
    }

    private String buildConsolidatedNotification(List<String> numberStrings, LotteryResult winningNumbers) {
        StringBuilder resultDetails = new StringBuilder();
        Set<Integer> winningRed = Arrays.stream(winningNumbers.getRed().split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        int winningBlue = Integer.parseInt(winningNumbers.getBlue());

        for (int i = 0; i < numberStrings.size(); i++) {
            String numberString = numberStrings.get(i);
            String formattedNumber = numberString.replace(" ", ",").replace("-", "+");
            resultDetails.append("您的号码").append(i + 1).append("（").append(formattedNumber).append("）：");

            Map<String, Object> numbers = validateAndParseNumbers(numberString);
            if (numbers == null) {
                resultDetails.append("格式错误\n");
                continue;
            }

            Set<Integer> userRed = (Set<Integer>) numbers.get("red");
            int userBlue = (int) numbers.get("blue");

            long redMatchCount = userRed.stream().filter(winningRed::contains).count();
            boolean blueMatch = (userBlue == winningBlue);

            String prize = calculatePrize(redMatchCount, blueMatch);
            String prizeDetails = getPrizeDetails(prize, winningNumbers);
            resultDetails.append(prize).append(" ").append(prizeDetails).append("\n");
        }

        String title = "双色球第" + winningNumbers.getCode() + "期开奖结果";
        StringBuilder finalMessage = new StringBuilder();
        finalMessage.append(title).append("\n\n")
                .append("开奖日期：").append(winningNumbers.getDate()).append("\n")
                .append("开奖号码：\n")
                .append("红球：").append(winningNumbers.getRed()).append("\n")
                .append("蓝球：").append(winningNumbers.getBlue()).append("\n\n")
                .append(resultDetails.toString());

        return finalMessage.toString();
    }

    /**
     * 根据中奖规则计算奖项名称。
     */
    private String calculatePrize(long redMatchCount, boolean blueMatch) {
        if (redMatchCount == 6 && blueMatch) return "一等奖";
        if (redMatchCount == 6) return "二等奖";
        if (redMatchCount == 5 && blueMatch) return "三等奖";
        if (redMatchCount == 5 || (redMatchCount == 4 && blueMatch)) return "四等奖";
        if (redMatchCount == 4 || (redMatchCount == 3 && blueMatch)) return "五等奖";
        if (blueMatch) return "六等奖";
        return "未中奖";
    }

    /**
     * 根据奖项名称从开奖结果中查找对应的奖金。
     */
    private String getPrizeDetails(String prize, LotteryResult winningNumbers) {
        if ("未中奖".equals(prize)) return "0元";

        int prizeType = -1;
        switch (prize) {
            case "一等奖":
                prizeType = 1;
                break;
            case "二等奖":
                prizeType = 2;
                break;
            case "三等奖":
                prizeType = 3;
                break;
            case "四等奖":
                prizeType = 4;
                break;
            case "五等奖":
                prizeType = 5;
                break;
            case "六等奖":
                prizeType = 6;
                break;
        }

        if (prizeType != -1) {
            for (PrizeGrade grade : winningNumbers.getPrizegrades()) {
                if (grade.getType() == prizeType) {
                    return formatMoney(grade.getTypemoney()) + "元";
                }
            }
        }
        return "未知金额";
    }

    /**
     * 验证并解析用户输入的单行号码字符串。
     */
    private Map<String, Object> validateAndParseNumbers(String numbers) {
        try {
            String[] parts = numbers.split("-");
            Set<Integer> redBalls = Arrays.stream(parts[0].split(" "))
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            int blueBall = Integer.parseInt(parts[1]);

            if (redBalls.size() != 6 || redBalls.stream().anyMatch(n -> n < 1 || n > 33))
                throw new IllegalArgumentException("红球规则错误");
            if (blueBall < 1 || blueBall > 16)
                throw new IllegalArgumentException("蓝球规则错误");

            Map<String, Object> parsed = new HashMap<>();
            parsed.put("red", redBalls);
            parsed.put("blue", blueBall);
            return parsed;
        } catch (Exception e) {
            log.error("号码格式错误: {}", numbers, e);
            return null;
        }
    }

    /**
     * 发送 Bark 通知。
     */
    private void sendBarkNotification(String title, String content) {
        if (barkKey == null || "your_bark_key_here".equals(barkKey) || barkKey.trim().isEmpty()) {
            return;
        }
        try {
            String url = String.format(BARK_URL_TEMPLATE, barkKey);
            HttpUtil.createGet(url).form("title", title).form("body", content).form("group", "双色球开奖").execute();
        } catch (Exception e) {
            log.error("Bark 通知发送失败", e);
        }
    }

    /**
     * 格式化完整的彩票信息，用于关键词回复。
     */
    private String formatLotteryInfo(LotteryResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎉 ").append(result.getName()).append(" 第 ").append(result.getCode()).append(" 期\n")
                .append("📅 开奖日期: ").append(result.getDate()).append("\n")
                .append("🔴 红球: ").append(result.getRed()).append("\n")
                .append("🔵 蓝球: ").append(result.getBlue()).append("\n\n")
                .append("💰 本期销量: ").append(formatMoney(result.getSales())).append(" 元\n")
                .append("累计奖池: ").append(formatMoney(result.getPoolmoney())).append(" 元\n\n")
                .append("--- 中奖详情 ---\n");
        for (PrizeGrade prize : result.getPrizegrades()) {
            if (prize.getTypenum() == null || prize.getTypenum().isEmpty()) continue;
            sb.append(getPrizeTypeName(prize.getType())).append(": ")
                    .append(prize.getTypenum()).append(" 注, ")
                    .append("每注 ").append(formatMoney(prize.getTypemoney())).append(" 元\n");
        }
        return sb.toString();
    }

    /**
     * 根据奖项类型ID获取奖项名称。
     */
    private String getPrizeTypeName(int type) {
        switch (type) {
            case 1:
                return "一等奖";
            case 2:
                return "二等奖";
            case 3:
                return "三等奖";
            case 4:
                return "四等奖";
            case 5:
                return "五等奖";
            case 6:
                return "六等奖";
            default:
                return "其他奖";
        }
    }

    /**
     * 格式化金额字符串，添加千位分隔符。
     */
    private String formatMoney(String money) {
        try {
            long moneyAmount = Long.parseLong(money);
            return String.format("%,d", moneyAmount);
        } catch (NumberFormatException e) {
            return money;
        }
    }
}
