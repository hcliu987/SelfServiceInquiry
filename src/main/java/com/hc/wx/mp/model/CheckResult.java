package com.hc.wx.mp.model;

import lombok.Data;

@Data
public class CheckResult {
    private String expect;
    private String number;
    private String winningAmount;
}