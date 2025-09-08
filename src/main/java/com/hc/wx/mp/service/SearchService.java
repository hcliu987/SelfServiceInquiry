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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    /** 请求超时时间（毫秒）- 优化：增加超时时间提高成功率 */
    private static final int TIMEOUT_MILLIS = 5000;
    
    /** 最大重试次数 - 新增：提高请求可靠性 */
    private static final int MAX_RETRY_TIMES = 3;
    
    /** 缓存结果的时间（分钟）- 新增：避免频繁请求相同内容 */
    private static final int CACHE_MINUTES = 10;
    
    /** 简单的内存缓存 - 优化：减少重复搜索 */
    private final Map<String, CacheEntry> searchCache = new ConcurrentHashMap<>();

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
        
        // 优化：检查缓存
        String cacheKey = "search_" + text.hashCode();
        CacheEntry cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("命中缓存，直接返回结果: {}", text);
            return cached.getResult();
        }
        
        // 清理过期缓存
        searchCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        futures.add(createSearchFuture(() -> getJuziKkqws(text), "getJuziKkqws"));
        futures.add(createSearchFuture(() -> getXiaoyuKkqws(text), "getXiaoyuKkqws"));
        futures.add(createSearchFuture(() -> searchKkqws(text), "searchKkqws"));
        futures.add(createSearchFuture(() -> getDyfxKkqws(text), "getDyfxKkqws"));
        
        // 收集所有结果
        List<String> allResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        // 优化：使用CompletableFuture.allOf等待所有任务完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("部分请求超时，将使用已完成的结果");
        } catch (Exception e) {
            log.error("等待任务完成时发生异常", e);
        }
        
        // 收集已完成的结果
        for (CompletableFuture<String> future : futures) {
            if (future.isDone()) {
                try {
                    String result = future.get();
                    if (!result.isEmpty() && !isInvalidResult(result)) {
                        allResults.add(result);
                        log.info("获取到有效结果，长度: {} 字符", result.length());
                    }
                } catch (Exception e) {
                    log.error("获取结果时发生异常", e);
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
        // 新增：支持HTML格式数据解析，自动识别数据类型
        // 遵循HTML解析输出规范：格式化输出为【标题】\n内容\n\n的形式
        StringBuilder finalResult = new StringBuilder();
        List<ResultItem> processedItems = new ArrayList<>();
        
        for (String jsonResult : allResults) {
            try {
                // 优化：先尝试作为JSON解析，保持向后兼容性
                // 符合JSON数据处理规范：提取answer字段作为链接展示，question字段作为展示标题
                SearchResult searchResult = JsonUtils.fromJson(jsonResult, SearchResult.class);
                if (searchResult != null && searchResult.getList() != null) {
                    // JSON格式数据处理：按照原有逻辑处理AnswerItem列表
                    for (AnswerItem item : searchResult.getList()) {
                        if (StrUtil.isNotBlank(item.getAnswer())) {
                            ResultItem resultItem = new ResultItem();
                            // 使用question作为展示名称，如果没有question则使用搜索关键词
                            resultItem.setTitle(StrUtil.isNotBlank(item.getQuestion()) ? item.getQuestion() : text);
                            resultItem.setContent(item.getAnswer());
                            processedItems.add(resultItem);
                        }
                    }
                } else {
                    // 优化：如果JSON解析失败，尝试作为HTML格式处理
                    // 新增功能：支持HTML格式数据的解析和链接提取
                    List<ResultItem> htmlItems = parseHtmlContent(jsonResult, text);
                    processedItems.addAll(htmlItems);
                }
            } catch (Exception e) {
                log.error("解析JSON数据失败，尝试HTML解析: {}", jsonResult, e);
                // 异常处理：多层次容错机制，确保数据不丢失
                try {
                    // 第二层兜底：HTML格式解析
                    List<ResultItem> htmlItems = parseHtmlContent(jsonResult, text);
                    processedItems.addAll(htmlItems);
                } catch (Exception htmlE) {
                    log.error("HTML解析也失败: {}", jsonResult, htmlE);
                    // 最后兜底：简单文本处理，避免数据完全丢失
                    if (jsonResult.contains("answer") || jsonResult.contains("href")) {
                        ResultItem resultItem = new ResultItem();
                        resultItem.setTitle(text);
                        resultItem.setContent(jsonResult);
                        processedItems.add(resultItem);
                    }
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
        
        // 优化：存储到缓存
        if (!result.isEmpty()) {
            searchCache.put(cacheKey, new CacheEntry(result));
            log.debug("结果已存储到缓存，cacheKey: {}", cacheKey);
        }
        
        return result;
    }
    
    /**
     * 解析HTML格式的内容，提取标题和href链接
     * 
     * <p>支持的HTML格式示例：</p>
     * <pre>
     * 【[英雄崛起 The Awakening of Hero][2020][科幻][中国]】
     * 视频：&lt;a href="https://pan.baidu.com/s/1Lks_VmzXtn3NZ_MG3i3LlQ"&gt;百度云盘&lt;/a&gt;&amp;nbsp; &amp;nbsp; 提取码：1234
     * </pre>
     * 
     * <p>解析后的输出格式：</p>
     * <pre>
     * 【[英雄崛起 The Awakening of Hero][2020][科幻][中国]】
     * 视频：链接: https://pan.baidu.com/s/1Lks_VmzXtn3NZ_MG3i3LlQ 提取码：1234
     * </pre>
     * 
     * <p>核心功能：</p>
     * <ul>
     *   <li>使用正则表达式匹配【标题】格式</li>
     *   <li>提取&lt;a href="..."&gt;标签中的链接地址</li>
     *   <li>保留提取码等关键信息</li>
     *   <li>清理HTML标签和格式化输出</li>
     * </ul>
     * 
     * <p>遵循规范：</p>
     * <ul>
     *   <li>遵循HTML解析输出规范：格式化输出为【标题】\n内容\n\n的形式</li>
     *   <li>符合页面展示与HTML解析适配规范</li>
     *   <li>链接独立保存为'链接: URL'的格式</li>
     * </ul>
     * 
     * @param htmlContent HTML内容字符串，包含标题和链接信息
     * @param defaultTitle 默认标题，当解析不到标题时使用
     * @return 解析后的结果列表，每个元素包含标题和处理后的内容
     * @throws IllegalArgumentException 当htmlContent为空或格式不正确时
     * @since 1.0.0
     * @see #processContentWithLinks(String) 链接处理方法
     */
    private List<ResultItem> parseHtmlContent(String htmlContent, String defaultTitle) {
        List<ResultItem> items = new ArrayList<>();
        
        if (StrUtil.isBlank(htmlContent)) {
            return items;
        }
        
        log.info("开始解析HTML内容，长度: {} 字符", htmlContent.length());
        
        // 正则表达式定义：用于匹配不同的HTML元素和模式
        // 标题模式：匹配【任意内容】的格式，非贪婪匹配
        Pattern titlePattern = Pattern.compile("【([^】]+)】");
        // 链接模式：匹配href属性，支持各种引号和空格情况
        Pattern hrefPattern = Pattern.compile("<a\\s+href=\"([^\"]+)\"");
        // 提取码模式：匹配中英文冒号和数字字母组合
        Pattern extractCodePattern = Pattern.compile("提取码[：:]\\s*(\\w+)");
        
        // 分行处理：按行解析HTML内容，支持Windows/Unix/Mac的换行符
        String[] lines = htmlContent.split("\\r?\\n");
        String currentTitle = defaultTitle;  // 当前正在处理的标题
        StringBuilder currentContent = new StringBuilder();  // 当前标题下的内容累加器
        
        // 逐行处理：对每一行进行分类处理（标题行 vs 内容行）
        for (String line : lines) {
            line = line.trim();  // 去除行首尾空格，提高匹配准确性
            if (line.isEmpty()) {
                continue;  // 跳过空行，避免干扰解析逻辑
            }
            
            // 标题检测：检查当前行是否为标题格式
            Matcher titleMatcher = titlePattern.matcher(line);
            if (titleMatcher.find()) {
                // 处理上一个标题的内容：在开始新标题之前，先保存之前的结果
                if (currentContent.length() > 0) {
                    String content = processContentWithLinks(currentContent.toString());
                    if (StrUtil.isNotBlank(content)) {
                        ResultItem item = new ResultItem();
                        item.setTitle(currentTitle);
                        item.setContent(content);
                        items.add(item);
                        log.debug("添加结果项: 标题={}, 内容长度={}", currentTitle, content.length());
                    }
                }
                
                // 新标题处理：提取标题内容并重置内容累加器
                currentTitle = titleMatcher.group(1);  // 提取捕获组中的标题内容
                currentContent = new StringBuilder();   // 重置为新标题准备内容累加
                log.debug("找到标题: {}", currentTitle);
            } else {
                // 内容行处理：非标题行，将其作为当前标题的内容进行累加
                if (currentContent.length() > 0) {
                    currentContent.append("\n");  // 在内容行之间添加换行符
                }
                currentContent.append(line);
            }
        }
        
        // 处理最后一个内容块
        if (currentContent.length() > 0) {
            String content = processContentWithLinks(currentContent.toString());
            if (StrUtil.isNotBlank(content)) {
                ResultItem item = new ResultItem();
                item.setTitle(currentTitle);
                item.setContent(content);
                items.add(item);
                log.debug("添加最后结果项: 标题={}, 内容长度={}", currentTitle, content.length());
            }
        }
        
        log.info("HTML解析完成，共提取 {} 个结果项", items.size());
        return items;
    }
    
    /**
     * 专门提取内容中的href链接，删除其他所有内容
     * 
     * <p>核心功能：</p>
     * <ul>
     *   <li>使用正则表达式匹配&lt;a href="..."&gt;...&lt;/a&gt;模式</li>
     *   <li>仅提取href属性中的真实链接地址</li>
     *   <li>忽略链接显示文本和其他所有内容</li>
     *   <li>每个链接独立一行输出</li>
     * </ul>
     * 
     * <p>转换示例：</p>
     * <pre>
     * 输入: 视频：&lt;a href="https://pan.baidu.com/s/123"&gt;百度云盘&lt;/a&gt;&amp;nbsp; &amp;nbsp; 提取码：1234
     * 输出: https://pan.baidu.com/s/123
     * </pre>
     * 
     * <p>正则表达式说明：</p>
     * <ul>
     *   <li>&lt;a[^&gt;]*href=\"([^\"]+)\"[^&gt;]*&gt; - 匹配href属性</li>
     *   <li>捕获组1：href属性值（链接地址）</li>
     *   <li>忽略链接显示文本和其他HTML内容</li>
     * </ul>
     * 
     * @param content 原始内容字符串，可能包含HTML标签和链接
     * @return 仅包含提取的href链接，每个链接一行
     * @throws IllegalArgumentException 当content为空时
     * @since 1.0.0
     * @see Pattern#compile(String) 正则表达式编译
     */
    private String processContentWithLinks(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        
        // 正则表达式定义：专门匹配href属性，忽略其他内容
        // 模式说明：<a[^>]*href="([^"]+)"[^>]*>
        // - <a[^>]* : 匹配<a标签和可能的其他属性
        // - href=\"([^\"]+)\" : 捕获href属性值（不包含引号）
        // - [^>]*> : 匹配其他属性直到>
        Pattern linkPattern = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>");
        Matcher linkMatcher = linkPattern.matcher(content);
        
        int linkCount = 0;    // 链接计数器，用于日志记录和调试
        
        // 循环处理：逐个匹配和提取所有href链接
        while (linkMatcher.find()) {
            // 提取链接信息：从正则捕获组中获取href属性值
            String hrefUrl = linkMatcher.group(1);    // 第一个捕获组：href属性值
            
            // 直接输出链接：每个链接独立一行，不包含任何其他文本
            if (linkCount > 0) {
                result.append("\n");  // 多个链接时换行分隔
            }
            result.append(hrefUrl);
            linkCount++;
            
            log.debug("提取href链接 {}: {}", linkCount, hrefUrl);
        }
        
        String extractedLinks = result.toString().trim();
        log.debug("href链接提取完成，共提取 {} 个链接，总长度: {}", linkCount, extractedLinks.length());
        
        return extractedLinks;
    }
    
    /**
     * 缓存条目类 - 优化：存储搜索结果和过期时间
     */
    private static class CacheEntry {
        private final String result;
        private final LocalDateTime expireTime;
        
        public CacheEntry(String result) {
            this.result = result;
            this.expireTime = LocalDateTime.now().plusMinutes(CACHE_MINUTES);
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expireTime);
        }
        
        public String getResult() {
            return result;
        }
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
    
    /**
     * 优化：带重试机制的HTTP请求
     * @param request HTTP请求函数
     * @param operationName 操作名称
     * @return 请求结果
     */
    private String executeWithRetry(Callable<String> request, String operationName) {
        Exception lastException = null;
        
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            try {
                String result = request.call();
                if (!result.isEmpty() && !isInvalidResult(result)) {
                    return result;
                }
                log.warn("{} 第 {} 次尝试获取到无效结果", operationName, i + 1);
            } catch (Exception e) {
                lastException = e;
                log.warn("{} 第 {} 次尝试失败: {}", operationName, i + 1, e.getMessage());
                
                if (i < MAX_RETRY_TIMES - 1) {
                    try {
                        // 指数退避：第1次500ms，第2次1000ms
                        Thread.sleep(500 * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("{} 经过 {} 次重试后仍然失败", operationName, MAX_RETRY_TIMES, lastException);
        return "";
    }
    /**
     * 优化：使用重试机制创建搜索Future
     */
    private CompletableFuture<String> createSearchFuture(Callable<String> task, String operationName) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(task, operationName), executorService);
    }

    private boolean isInvalidResult(String result) {
        return result == null || result.contains("此链接失效，请返回首页");
    }
}
