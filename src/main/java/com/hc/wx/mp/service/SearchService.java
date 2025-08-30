package com.hc.wx.mp.service;

import cn.hutool.core.util.StrUtil;
import com.hc.wx.mp.config.ApiConfig;
import com.hc.wx.mp.entity.TokenResponse;
import com.hc.wx.mp.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
public class SearchService {
    private final ApiConfig apiConfig;
    private final ExecutorService executorService;
    private static final int TIMEOUT_MILLIS = 300;

    public SearchService(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
        this.executorService = new ThreadPoolExecutor(
            apiConfig.getThreadPool().getCoreSize(),
            apiConfig.getThreadPool().getMaxSize(),
            apiConfig.getThreadPool().getKeepAliveSeconds(),
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(apiConfig.getThreadPool().getQueueCapacity()),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    // ===================================================================================
    // 主入口方法
    // ===================================================================================

    /**
     * m.kkqws.com 的主搜索入口。
     * 并发调用其核心API，并返回第一个有效结果。
     */
    public String searchAndMerge(String text) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        futures.add(createSearchFuture(() -> getJuziKkqws(text), "getJuziKkqws"));
        futures.add(createSearchFuture(() -> getXiaoyuKkqws(text), "getXiaoyuKkqws"));
        futures.add(createSearchFuture(() -> searchKkqws(text), "searchKkqws"));
        futures.add(createSearchFuture(() -> getDyfxKkqws(text), "getDyfxKkqws"));
        return getFirstValidResult(futures, "searchAndMerge");
    }

    /**
     * uukk6.cn 的主搜索入口。
     * 自动获取Token，然后并发调用所有API，并返回第一个有效结果。
     */
    public String searchUukkAll(String name) {
        try {
            TokenResponse tokenResponse = getToken();
            if (tokenResponse == null || StrUtil.isEmpty(tokenResponse.getToken())) {
                log.error("获取Token失败，无法继续搜索。");
                return "";
            }
            String token = tokenResponse.getToken();
            
            List<CompletableFuture<String>> futures = new ArrayList<>();
            futures.add(createSearchFuture(() -> getDyfxUukk(name, token), "getDyfxUukk"));
            futures.add(createSearchFuture(() -> getGGangUukk(name, token), "getGGangUukk"));
            // ... add all other uukk6 methods
            return getFirstValidResult(futures, "searchUukkAll");
        } catch (Exception e) {
            log.error("searchUukkAll 执行过程中发生异常", e);
            return "";
        }
    }

    // ===================================================================================
    // API 实现 - uukk6.cn
    // ===================================================================================
    
    public TokenResponse getToken() throws Exception {
        String jsonResponse = sendUukkGetRequest("/v/api/gettoken");
        return JsonUtils.fromJson(jsonResponse, TokenResponse.class);
    }
    
    public String getDyfxUukk(String name, String token) throws Exception {
        return sendUukkPostRequest("/v/api/getDyfx", "name=" + URLEncoder.encode(name, "UTF-8") + "&token=" + token);
    }
    
    public String getGGangUukk(String name, String token) throws Exception {
        return sendUukkPostRequest("/v/api/getGGang", "name=" + URLEncoder.encode(name, "UTF-8") + "&token=" + token);
    }
    // ... all other uukk6 methods here

    // ===================================================================================
    // API 实现 - m.kkqws.com
    // ===================================================================================

    public String getJuziKkqws(String text) throws Exception {
        return sendKkqwsPostRequest(apiConfig.getJuziPath(), text);
    }

    public String getXiaoyuKkqws(String text) throws Exception {
        return sendKkqwsPostRequest(apiConfig.getXiaoyuPath(), text);
    }

    public String searchKkqws(String text) throws Exception {
        return sendKkqwsPostRequest(apiConfig.getSearchPath(), text);
    }

    public String getDyfxKkqws(String text) throws Exception {
        return sendKkqwsPostRequest(apiConfig.getDyfxPath(), text);
    }


    // ===================================================================================
    // 底层HTTP与并发逻辑
    // ===================================================================================

    private String getFirstValidResult(List<CompletableFuture<String>> futures, String operationName) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < TIMEOUT_MILLIS) {
            for (CompletableFuture<String> future : futures) {
                if (future.isDone()) {
                    String result = future.getNow("");
                    if (!result.isEmpty() && !isInvalidResult(result)) {
                        log.info("{} 成功从一个并发任务中获取到结果。", operationName);
                        return result;
                    }
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
        }
        log.warn("{} 所有并发任务在超时 {}ms 内均未返回有效结果。", operationName, TIMEOUT_MILLIS);
        return "";
    }
    
    private String sendKkqwsPostRequest(String path, String text) throws Exception {
        URL url = new URL(apiConfig.getBaseUrl() + path);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");
        httpConn.setConnectTimeout(TIMEOUT_MILLIS);
        httpConn.setReadTimeout(TIMEOUT_MILLIS);
        httpConn.setDoOutput(true);
        setKkqwsHeaders(httpConn);

        String postData = "name=" + URLEncoder.encode(text, "UTF-8") + "&token=" + apiConfig.getKkqwsToken();
        try (OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream())) {
            writer.write(postData);
            writer.flush();
        }
        return handleResponse(httpConn);
    }
    
    private String sendUukkPostRequest(String path, String postData) throws Exception {
        URL url = new URL(apiConfig.getUukkBaseUrl() + path);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");
        httpConn.setConnectTimeout(TIMEOUT_MILLIS);
        httpConn.setReadTimeout(TIMEOUT_MILLIS);
        httpConn.setDoOutput(true);
        setUukkHeaders(httpConn);

        try (OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream())) {
            writer.write(postData);
            writer.flush();
        }
        return handleResponse(httpConn);
    }
    
    private String sendUukkGetRequest(String path) throws Exception {
        URL url = new URL(apiConfig.getUukkBaseUrl() + path);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");
        httpConn.setConnectTimeout(TIMEOUT_MILLIS);
        httpConn.setReadTimeout(TIMEOUT_MILLIS);
        setUukkHeaders(httpConn);
        return handleResponse(httpConn);
    }

    private String handleResponse(HttpURLConnection httpConn) throws IOException {
        int responseCode = httpConn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = httpConn.getInputStream();
            String contentEncoding = httpConn.getContentEncoding();

            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                inputStream = new GZIPInputStream(inputStream);
            }

            try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }
        } else {
            log.error("HTTP请求失败，响应码: {}", responseCode);
            return "";
        }
    }

    private void setUukkHeaders(HttpURLConnection httpConn) {
        httpConn.setRequestProperty("Host", "uukk6.cn");
        httpConn.setRequestProperty("Origin", "http://uukk6.cn");
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        httpConn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        httpConn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    }
    
    private void setKkqwsHeaders(HttpURLConnection httpConn) {
         // Headers for m.kkqws.com
        httpConn.setRequestProperty("Host", "m.kkqws.com");
        httpConn.setRequestProperty("Origin", "http://m.kkqws.com");
        // ... other headers
    }
    
    private CompletableFuture<String> createSearchFuture(Callable<String> task, String operationName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                log.error("{} 异常", operationName, e);
                return "";
            }
        }, executorService);
    }

    private boolean isInvalidResult(String result) {
        return result == null || result.contains("此链接失效，请返回首页");
    }
}
