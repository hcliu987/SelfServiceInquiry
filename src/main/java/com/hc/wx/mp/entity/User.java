package com.hc.wx.mp.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode()
public class User {

    private List myNumbers;
    private String last;
    private String wxPusher;
}
