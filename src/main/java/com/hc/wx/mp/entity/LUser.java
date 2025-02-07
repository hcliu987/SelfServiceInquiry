package com.hc.wx.mp.entity;

import lombok.Data;

import java.util.List;

@Data
public class LUser {
    private String id;
    private String last;
    private List<String> myNumbers;
}
