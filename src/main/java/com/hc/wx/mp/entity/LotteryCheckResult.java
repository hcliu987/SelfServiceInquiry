package com.hc.wx.mp.entity;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class LotteryCheckResult {
    private String drawDate;        // 开奖日期
    private String openCode;        // 开奖号码
    private String redBalls;        // 红球
    private String blueBall;        // 蓝球
    private String expect;          // 期号
    private List<NumberResult> numberResults = new ArrayList<>(); // 投注号码结果列表

    @Data
    public static class NumberResult {
        private String number;      // 投注号码
        private String result;      // 中奖结果
        private String prize;       // 中奖金额
    }
}