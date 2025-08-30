package com.hc.wx.mp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "api.search")
public class ApiConfig {
    private String baseUrl;
    private String juziPath;
    private String xiaoyuPath;
    private String searchPath;
    private String token;
    private String searchXpath;
    private String ttzjbPath;
    private String dyfxPath;
    private String juzi2Path;
    private String girlsPath;
    private String xiaoyPath;
    private String ggangPath;
    private String uukk6Token;
    private String uukkBaseUrl;
    private String kkqwsToken;

    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    @Data
    public static class ThreadPoolConfig {
        private int coreSize = 8;
        private int maxSize = 16;
        private int queueCapacity = 50;
        private long keepAliveSeconds = 30;
    }
}