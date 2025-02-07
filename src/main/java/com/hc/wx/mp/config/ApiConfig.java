package com.hc.wx.mp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "api.search")
public class ApiConfig {
    private String baseUrl = "http://m.kkqws.com";
    private String juziPath = "/v/api/getJuzi";
    private String xiaoyuPath = "/v/api/getXiaoyu";
    private String searchPath = "/v/api/search";
    private String token = "i69";
}