package com.hc.wx.mp.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class QinglongService {

    @Value("${qinglong.url}")
    private String baseUrl;

    @Value("${qinglong.client-id}")
    private String clientId;

    @Value("${qinglong.client-secret}")
    private String clientSecret;

    private String token;
    private long tokenExpiration;

    @Data
    static class TokenResponse {
        private int code;
        private TokenData data;
    }

    @Data
    static class TokenData {
        private String token;
        private String token_type;
        private long expiration;
    }

    public String getToken() {
        if (isTokenValid()) {
            return token;
        }
        return refreshToken();
    }

    private boolean isTokenValid() {
        return token != null && System.currentTimeMillis() / 1000 < tokenExpiration - 60;
    }

    private synchronized String refreshToken() {
        try {
            String url = baseUrl + "/open/auth/token";
            String response = HttpRequest.get(url)
                    .form("client_id", clientId)
                    .form("client_secret", clientSecret)
                    .execute()
                    .body();

            TokenResponse tokenResponse = JSONUtil.toBean(response, TokenResponse.class);

            if (tokenResponse.getCode() == 200 && tokenResponse.getData() != null) {
                token = tokenResponse.getData().getToken();
                tokenExpiration = tokenResponse.getData().getExpiration();
                log.info("青龙token刷新成功，过期时间：{}", tokenExpiration);
                return token;
            }
            log.error("获取青龙token失败，响应内容：{}", response);
            throw new RuntimeException("获取青龙token失败，服务返回非200状态码");
        } catch (Exception e) {
            log.error("获取青龙token异常：{}", e.getMessage());
            throw new RuntimeException("获取青龙token失败：" + e.getMessage(), e);
        }
    }

    @Data
    static class EnvUpdateResponse {
        private int code;
        private String msg;
        private Object data;
    }

    /**
     * 更新环境变量
     */
    public void updateEnv(String name, String value, String remarks) {
        try {
            String token = getToken();
            // 修改 requestBody 格式，添加 id 字段
            String requestBody = String.format("{\"name\":\"%s\",\"value\":\"%s\",\"remarks\":%s,\"id\":53}",
                    name,
                    value,
                    remarks != null ? "\"" + remarks + "\"" : "null"
            );

            String url = baseUrl + "/open/envs?t=" + System.currentTimeMillis();
            String response = HttpRequest.put(url)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .execute()
                    .body();

            System.out.println("更新环境变量响应:"+response);
            EnvUpdateResponse result = JSONUtil.toBean(response, EnvUpdateResponse.class);
            if (result.getCode() != 200) {
                log.error("更新环境变量失败: {}", result.getMsg());
                throw new RuntimeException("更新环境变量失败: " + result.getMsg());
            }
            log.info("更新环境变量成功: name={}", name);
        } catch (Exception e) {
            log.error("更新环境变量异常: name={}", name, e);
            throw new RuntimeException("更新环境变量失败", e);
        }
    }

}