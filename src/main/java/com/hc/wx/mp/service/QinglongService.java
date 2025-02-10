package com.hc.wx.mp.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            throw new RuntimeException("获取青龙token失败: " + response);
        } catch (Exception e) {
            log.error("获取青龙token异常", e);
            throw new RuntimeException("获取青龙token失败", e);
        }
    }

    public void updateEnv(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("环境变量值不能为空");
        }

        try {
            String token = getToken();
            Map<String, Object> envData = new HashMap<>();
            envData.put("name", "sfsyUrl");
            envData.put("value", value.trim());
            envData.put("remarks", "顺丰速运链接");

            // 查询是否存在
            String searchUrl = baseUrl + "/open/envs?searchValue=" + value.trim();
            String searchResponse = HttpRequest.get(searchUrl)
                    .header("Authorization", "Bearer " + token)
                    .execute()
                    .body();

            JSONObject searchResult = JSONUtil.parseObj(searchResponse);
            List<Object> existingEnvs = searchResult.getJSONArray("data").toList(Object.class);

            if (existingEnvs.isEmpty()) {
                createEnv(envData, token);
                log.info("成功创建环境变量sfsyUrl: {}", value);
            } else {
                updateExistingEnv(envData, existingEnvs.get(0), token);
                log.info("成功更新环境变量sfsyUrl: {}", value);
            }
        } catch (Exception e) {
            log.error("更新环境变量sfsyUrl失败: value={}", value, e);
            throw new RuntimeException("更新环境变量sfsyUrl失败", e);
        }
    }

    private void createEnv(Map<String, Object> envData, String token) {
        String createUrl = baseUrl + "/open/envs";
        String response = HttpRequest.post(createUrl)
                .header("Authorization", "Bearer " + token)
                .body(JSONUtil.toJsonStr(Collections.singletonList(envData)))
                .execute()
                .body();
        log.info("创建环境变量成功: {}", envData.get("name"));
    }

    private void updateExistingEnv(Map<String, Object> envData, Object existing, String token) {
        JSONObject existingEnv = JSONUtil.parseObj(existing);
        envData.put("id", existingEnv.getInt("id"));
        
        String updateUrl = baseUrl + "/open/envs";
        String response = HttpRequest.put(updateUrl)
                .header("Authorization", "Bearer " + token)
                .body(JSONUtil.toJsonStr(envData))
                .execute()
                .body();
        log.info("更新环境变量成功: {}", envData.get("name"));
    }
}