package com.hc.wx.mp.entity;

import lombok.Data;
import java.util.List;

@Data
public class LotteryResult {
    private String name;
    private String code;
    private String date;
    private String week;
    private String red;
    private String blue;
    private String sales;
    private String poolmoney;
    private String content;
    private List<PrizeGrade> prizegrades;
}
