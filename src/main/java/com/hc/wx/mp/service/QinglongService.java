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

    public void updateEnv(String name, String value, String remarks) {
        try {
            String token = getToken();
            Map<String, Object> envData = new HashMap<>();
            envData.put("name", name);
            envData.put("value", value);
            envData.put("remarks", remarks);

            // 查询是否存在
            String searchUrl = baseUrl + "/open/envs?searchValue=" + value;
            String searchResponse = HttpRequest.get(searchUrl)
                    .header("Authorization", "Bearer " + token)
                    .execute()
                    .body();

            JSONObject searchResult = JSONUtil.parseObj(searchResponse);
            List<Object> existingEnvs = searchResult.getJSONArray("data").toList(Object.class);

            if (existingEnvs.isEmpty()) {
                createEnv(envData, token);
            } else {
                updateExistingEnv(envData, existingEnvs.get(0), token);
            }
        } catch (Exception e) {
            log.error("更新环境变量失败: name={}, value={}", name, value, e);
            throw new RuntimeException("更新环境变量失败", e);
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
    public void deleteEnv(String name) {
        try {
            String token = getToken();
            String searchUrl = baseUrl + "/open/envs?searchValue=" + name;
            String searchResponse = HttpRequest.get(searchUrl)
                    .header("Authorization", "Bearer " + token)
                    .execute()
                    .body();

            JSONObject searchResult = JSONUtil.parseObj(searchResponse);
            List<Object> existingEnvs = searchResult.getJSONArray("data").toList(Object.class);

            if (!existingEnvs.isEmpty()) {
                JSONObject existingEnv = JSONUtil.parseObj(existingEnvs.get(0));
                int envId = existingEnv.getInt("id");
                
                String deleteUrl = baseUrl + "/open/envs";
                String response = HttpRequest.delete(deleteUrl)
                        .header("Authorization", "Bearer " + token)
                        .body(JSONUtil.toJsonStr(Collections.singletonList(envId)))
                        .execute()
                        .body();
                log.info("删除环境变量成功: {}", name);
            }
        } catch (Exception e) {
            log.error("删除环境变量失败: name={}", name, e);
            throw new RuntimeException("删除环境变量失败", e);
        }
    }
}