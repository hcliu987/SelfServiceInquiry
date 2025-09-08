package com.hc.wx.mp.entity;

import lombok.Data;
import java.util.List;

@Data
public class LotteryResponse {
    private int state;
    private String message;
    private List<LotteryResult> result;
}
