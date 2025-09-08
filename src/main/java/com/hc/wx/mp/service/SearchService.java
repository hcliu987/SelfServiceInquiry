package com.hc.wx.mp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SearchResult {
        private List<AnswerItem> list;
        public List<AnswerItem> getList() { return list; }
        public void setList(List<AnswerItem> list) { this.list = list; }
    }

    /**
     * 答案项数据类，用于映射JSON响应中的list数组元素
     * 包含answer和question字段，符合API响应格式
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnswerItem {
        /** 答案内容，通常包含链接和提取码等信息 */
        private String answer;
        /** 问题标题，用作展示名称 */
        private String question;
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }
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
        String jsonResponse = getFirstValidResult(futures, "searchAndMerge");

        if (StrUtil.isBlank(jsonResponse)) {
            return "";
        }

        try {
            SearchResult result = JsonUtils.fromJson(jsonResponse, SearchResult.class);
            if (result == null || result.getList() == null || result.getList().isEmpty()) {
                return "未找到相关内容。";
            }
            
            StringBuilder finalResult = new StringBuilder();
            finalResult.append(text).append(":\n");

            List<String> allLinks = new ArrayList<>();
            for (AnswerItem item : result.getList()) {
                if (StrUtil.isNotBlank(item.getAnswer())) {
                    Pattern linkPattern = Pattern.compile("https?:\\/\\/[^\\s\"'<>]+");
                    Matcher matcher = linkPattern.matcher(item.getAnswer());
                    while (matcher.find()) {
                        allLinks.add(matcher.group());
                    }
                }
            }
            finalResult.append(String.join("\n", allLinks));
            System.out.println(finalResult.toString());
            return finalResult.toString();

        } catch (Exception e) {
            log.error("格式化搜索结果失败, JSON: {}", jsonResponse, e);
            return jsonResponse;
        }
    }

    /**
     * 多线程获取数据并合并，处理JSON数据提取answer作为链接内容，question作为展示名称
     * 
     * 功能说明：
     * 1. 使用固定大小的线程池进行多线程数据获取
     * 2. 收集所有线程的执行结果，保证数据完整性
     * 3. 解析JSON响应提取question和answer字段
     * 4. 按照【标题】\n内容\n\n的格式输出
     * 
     * 遵循JSON数据处理规范：
     * - 仅提取answer字段作为链接展示
     * - question字段作为展示标题
     * - 格式化输出为【标题】\n内容\n\n的形式
     * 
     * @param text 搜索关键词
     * @return 格式化后的搜索结果字符串，包含所有数据源的结果
     */
    public String searchAndMergeRaw(String text) {
        log.info("开始多线程数据获取并处理，查询内容: {}", text);
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        futures.add(createSearchFuture(() -> getJuziKkqws(text), "getJuziKkqws"));
        futures.add(createSearchFuture(() -> getXiaoyuKkqws(text), "getXiaoyuKkqws"));
        futures.add(createSearchFuture(() -> searchKkqws(text), "searchKkqws"));
        futures.add(createSearchFuture(() -> getDyfxKkqws(text), "getDyfxKkqws"));
        
        // 收集所有结果
        List<String> allResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        // 等待所有任务完成或超时
        while (System.currentTimeMillis() - startTime < TIMEOUT_MILLIS && !futures.isEmpty()) {
            futures.removeIf(future -> {
                if (future.isDone()) {
                    try {
                        String result = future.get();
                        if (!result.isEmpty() && !isInvalidResult(result)) {
                            allResults.add(result);
                            log.info("获取到有效结果，长度: {} 字符", result.length());
                            log.info("原始JSON数据: {}", result);
                        } else {
                            log.warn("获取到无效或空结果");
                        }
                    } catch (Exception e) {
                        log.error("获取结果时发生异常", e);
                    }
                    return true;
                }
                return false;
            });
            
            if (!futures.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // 记录汇总信息
        log.info("多线程数据获取完成，共收集到 {} 个有效结果", allResults.size());
        
        if (allResults.isEmpty()) {
            log.warn("未获取到任何有效数据");
            return "";
        }
        
        // 处理JSON数据，提取question和answer字段
        StringBuilder finalResult = new StringBuilder();
        List<ResultItem> processedItems = new ArrayList<>();
        
        for (String jsonResult : allResults) {
            try {
                SearchResult searchResult = JsonUtils.fromJson(jsonResult, SearchResult.class);
                if (searchResult != null && searchResult.getList() != null) {
                    for (AnswerItem item : searchResult.getList()) {
                        if (StrUtil.isNotBlank(item.getAnswer())) {
                            ResultItem resultItem = new ResultItem();
                            // 使用question作为展示名称，如果没有question则使用搜索关键词
                            resultItem.setTitle(StrUtil.isNotBlank(item.getQuestion()) ? item.getQuestion() : text);
                            resultItem.setContent(item.getAnswer());
                            processedItems.add(resultItem);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("解析JSON数据失败: {}", jsonResult, e);
                // 如果JSON解析失败，尝试简单的文本处理
                if (jsonResult.contains("answer")) {
                    ResultItem resultItem = new ResultItem();
                    resultItem.setTitle(text);
                    resultItem.setContent(jsonResult);
                    processedItems.add(resultItem);
                }
            }
        }
        
        if (processedItems.isEmpty()) {
            log.warn("未能从JSON数据中提取到有效的内容项");
            return "未找到相关内容";
        }
        
        // 格式化输出
        for (ResultItem item : processedItems) {
            finalResult.append("【").append(item.getTitle()).append("】\n");
            finalResult.append(item.getContent()).append("\n\n");
        }
        
        String result = finalResult.toString().trim();
        log.info("处理完成，共提取 {} 个内容项，总长度: {} 字符", processedItems.size(), result.length());
        log.info("最终处理结果: {}", result);
        
        return result;
    }
    
    /**
     * 结果项数据类，用于内部数据处理和格式化
     * 将解析后的JSON数据转换为统一的结果格式
     */
    private static class ResultItem {
        /** 展示标题，来自question字段或搜索关键词 */
        private String title;
        /** 内容信息，来自answer字段，包含链接和提取码 */
        private String content;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
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
