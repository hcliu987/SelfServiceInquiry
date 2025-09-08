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
    
    /** HTTP连接池配置 - 优化：提高并发性能 */
    private HttpConfig http = new HttpConfig();

    @Data
    public static class ThreadPoolConfig {
        private int coreSize = 8;
        private int maxSize = 16;
        private int queueCapacity = 50;
        private long keepAliveSeconds = 30;
    }
    
    /** HTTP连接配置类 - 新增：优化网络请求性能 */
    @Data
    public static class HttpConfig {
        /** 连接超时时间(毫秒) */
        private int connectTimeout = 5000;
        /** 读取超时时间(毫秒) */
        private int readTimeout = 10000;
        /** 最大连接数 */
        private int maxConnections = 200;
        /** 每个路由的最大连接数 */
        private int maxConnectionsPerRoute = 50;
    }
}