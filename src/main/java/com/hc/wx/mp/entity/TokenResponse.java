package com.hc.wx.mp.entity;

import lombok.Data;
import java.util.List;

/**
 * 用于解析 /v/api/gettoken 接口响应的 DTO。
 */
@Data
public class TokenResponse {
    private String token;
    private List<String> user;
}
