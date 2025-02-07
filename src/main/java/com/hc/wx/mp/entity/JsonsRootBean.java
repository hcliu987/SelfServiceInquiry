package com.hc.wx.mp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class JsonsRootBean {

    private boolean us;
    private List<Lists> list;
    private boolean cache;
    private  String msg;
}
