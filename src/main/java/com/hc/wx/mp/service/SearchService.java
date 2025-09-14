package com.hc.wx.mp.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hc.wx.mp.config.ApiConfig;
import com.hc.wx.mp.entity.TokenResponse;
import com.hc.wx.mp.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
public class SearchService {

    // ================================ 常量定义 ================================
    /** 请求超时时间（毫秒） */
    private static final int TIMEOUT_MILLIS = 5000;
    /** 最大重试次数 */
    private static final int MAX_RETRY_TIMES = 3;
    /** 缓存结果的时间（分钟） */
    private static final int CACHE_MINUTES = 10;
    /** 重试间隔基础时间（毫秒） */
    private static final int RETRY_BASE_DELAY = 500;

    // ================================ 成员变量 ================================
    private final ApiConfig apiConfig;
    private final ExecutorService executorService;
    private final Map<String, CacheEntry> searchCache = new ConcurrentHashMap<>();

    // ================================ 内部类 ================================
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SearchResult {
        private List<AnswerItem> list;
        public List<AnswerItem> getList() { return list; }
        public void setList(List<AnswerItem> list) { this.list = list; }
    }

    /**
     * Makifx 搜索响应数据类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MakifxResponse {
        private int code;
        private String message;
        private MakifxData data;
        
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public MakifxData getData() { return data; }
        public void setData(MakifxData data) { this.data = data; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MakifxData {
        private int total;
        private Map<String, List<MakifxItem>> merged_by_type;
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public Map<String, List<MakifxItem>> getMerged_by_type() { return merged_by_type; }
        public void setMerged_by_type(Map<String, List<MakifxItem>> merged_by_type) { this.merged_by_type = merged_by_type; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MakifxItem {
        private String url;
        private String password;
        private String note;
        private String datetime;
        private String source;
        private List<String> images;
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getDatetime() { return datetime; }
        public void setDatetime(String datetime) { this.datetime = datetime; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }
    }

    /**
     * 答案项数据类，用于映射JSON响应中的list数组元素
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnswerItem {
        private String answer;
        private String question;
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }

    /**
     * 结果项数据类，用于内部数据处理和格式化
     */
    private static class ResultItem {
        private String title;
        private String content;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    /**
     * 缓存条目类
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

    // ================================ 构造函数 ================================
    public SearchService(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
        this.executorService = createOptimizedThreadPool();
        // 初始化SSL上下文，信任所有证书（仅用于开发和测试）
        initSSLContext();
    }

    private ThreadPoolExecutor createOptimizedThreadPool() {
        return new ThreadPoolExecutor(
            apiConfig.getThreadPool().getCoreSize(),
            apiConfig.getThreadPool().getMaxSize(),
            apiConfig.getThreadPool().getKeepAliveSeconds(),
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(apiConfig.getThreadPool().getQueueCapacity()),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * 初始化SSL上下文，信任所有证书
     * 注意：这种做法仅适用于开发和测试环境
     * 生产环境应该使用正确的证书验证
     */
    private void initSSLContext() {
        try {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // 不做任何检查
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // 不做任何检查
                    }
                }
            };
            
            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // 设置默认的SSL Socket Factory
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
            // 设置默认的Hostname Verifier
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true; // 信任所有hostname
                }
            });
            
            log.info("SSL上下文初始化完成，已设置信任所有证书");
            
        } catch (Exception e) {
            log.error("SSL上下文初始化失败", e);
        }
    }

    // ================================ 主入口方法 ================================

    /**
     * m.kkqws.com 的主搜索入口。
     * 并发调用其核心API，并返回第一个有效结果。
     */
    public String searchAndMerge(String text) {
        List<CompletableFuture<String>> futures = createKkqwsSearchFutures(text);
        String jsonResponse = getFirstValidResult(futures, "searchAndMerge");

        if (StrUtil.isBlank(jsonResponse)) {
            return "";
        }

        return formatKkqwsSearchResult(jsonResponse, text);
    }

    /**
     * 多线程获取数据并合并，处理JSON数据提取answer作为链接内容，
     * question作为展示名称，按照【标题】\n内容\n\n的格式输出
     */
    public String searchAndMergeRaw(String text) {
        log.info("开始多线程数据获取并处理，查询内容: {}", text);
        
        // 检查缓存
        String cachedResult = getCachedResult(text);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // 清理过期缓存
        cleanExpiredCache();
        
        // 执行搜索并收集结果
        List<String> allResults = executeSearchAndCollectResults(text);
        
        if (allResults.isEmpty()) {
            log.warn("未获取到任何有效数据");
            return "";
        }
        
        // 处理结果并缓存
        String finalResult = processAndFormatResults(allResults, text);
        cacheResult(text, finalResult);
        
        return finalResult;
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
            
            List<CompletableFuture<String>> futures = createUukkSearchFutures(name, token);
            return getFirstValidResult(futures, "searchUukkAll");
        } catch (Exception e) {
            log.error("searchUukkAll 执行过程中发生异常", e);
            return "";
        }
    }

    /**
     * 根据名字搜索 Makifx 资源
     * 调用 https://sou.makifx.com API，返回格式化的搜索结果
     * 
     * @param keyword 搜索关键词
     * @return 格式化的搜索结果，只显示链接
     */
    public String searchMakifx(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return "搜索关键词不能为空";
        }
        
        // 详细记录输入参数
        log.info("开始搜索 Makifx 资源，原始关键词: [{}], 字符长度: {}, 字符编码检查: {}", 
                keyword, keyword.length(), java.util.Arrays.toString(keyword.toCharArray()));
        
        // 检查缓存
        String cacheKey = "makifx_" + keyword.hashCode();
        CacheEntry cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("Makifx 搜索命中缓存: {}", keyword);
            return cached.getResult();
        }
        
        try {
            // 调用 Makifx API
            String jsonResponse = sendMakifxRequest(keyword);
            if (StrUtil.isBlank(jsonResponse)) {
                log.warn("Makifx API 返回空响应，关键词: {}", keyword);
                return "未获取到搜索结果";
            }
            
            // 解析并格式化结果
            String formattedResult = formatMakifxResult(jsonResponse, keyword);
            
            // 只有成功的结果才缓存
            if (StrUtil.isNotBlank(formattedResult) && 
                !formattedResult.contains("未找到") && 
                !formattedResult.contains("失败") && 
                !formattedResult.contains("异常") &&
                !formattedResult.contains("不可用")) {
                searchCache.put(cacheKey, new CacheEntry(formattedResult));
                log.info("Makifx 搜索结果已缓存，关键词: {}", keyword);
            }
            
            return formattedResult;
            
        } catch (Exception e) {
            log.error("Makifx 搜索失败，关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return "搜索过程中发生错误: " + e.getMessage();
        }
    }

    /**
     * 调试方法：验证URL编码过程
     * @param keyword 输入关键词
     * @return 编码信息
     */
    public String debugUrlEncoding(String keyword) {
        try {
            StringBuilder debug = new StringBuilder();
            debug.append(String.format("输入关键词: [%s]\n", keyword));
            debug.append(String.format("字符长度: %d\n", keyword.length()));
            debug.append(String.format("字符数组: %s\n", java.util.Arrays.toString(keyword.toCharArray())));
            
            String encoded = URLEncoder.encode(keyword, "UTF-8");
            debug.append(String.format("URL编码结果: %s\n", encoded));
            
            String decoded = java.net.URLDecoder.decode(encoded, "UTF-8");
            debug.append(String.format("解码验证: [%s]\n", decoded));
            debug.append(String.format("编码正确性: %s\n", keyword.equals(decoded)));
            
            // 检查是否与"天然子结构"的编码相同
            String naturalStructure = "天然子结构";
            String naturalEncoded = URLEncoder.encode(naturalStructure, "UTF-8");
            debug.append(String.format("天然子结构编码: %s\n", naturalEncoded));
            debug.append(String.format("是否与天然子结构编码相同: %s\n", encoded.equals(naturalEncoded)));
            
            return debug.toString();
        } catch (Exception e) {
            return "调试失败: " + e.getMessage();
        }
    }
    
    /**
     * 简化的Makifx搜索方法，用于调试
     */
    public String debugMakifxSearch(String keyword) {
        log.info("=== 开始Makifx搜索调试 ===");
        log.info("调试 - 接收到的关键词: [{}]", keyword);
        
        String debugInfo = debugUrlEncoding(keyword);
        log.info("URL编码调试信息:\n{}", debugInfo);
        
        try {
            String result = sendMakifxRequest(keyword);
            log.info("API调用完成，结果长度: {} 字符", result.length());
            return String.format("调试信息：\n%s\n\nAPI调用结果长度: %d 字符", debugInfo, result.length());
        } catch (Exception e) {
            log.error("调试搜索失败: {}", e.getMessage(), e);
            return String.format("调试信息：\n%s\n\n调试搜索失败: %s", debugInfo, e.getMessage());
        }
    }

    private String getCachedResult(String text) {
        String cacheKey = "search_" + text.hashCode();
        CacheEntry cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("命中缓存，直接返回结果: {}", text);
            return cached.getResult();
        }
        return null;
    }

    private void cleanExpiredCache() {
        searchCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void cacheResult(String text, String result) {
        if (!result.isEmpty()) {
            String cacheKey = "search_" + text.hashCode();
            searchCache.put(cacheKey, new CacheEntry(result));
            log.debug("结果已存储到缓存，cacheKey: {}", cacheKey);
        }
    }

    // ================================ 搜索执行方法 ================================

    private List<CompletableFuture<String>> createKkqwsSearchFutures(String text) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        futures.add(createSearchFuture(() -> getJuziKkqws(text), "getJuziKkqws"));
        futures.add(createSearchFuture(() -> getXiaoyuKkqws(text), "getXiaoyuKkqws"));
        futures.add(createSearchFuture(() -> searchKkqws(text), "searchKkqws"));
        futures.add(createSearchFuture(() -> getDyfxKkqws(text), "getDyfxKkqws"));
        return futures;
    }

    private List<CompletableFuture<String>> createUukkSearchFutures(String name, String token) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        futures.add(createSearchFuture(() -> getDyfxUukk(name, token), "getDyfxUukk"));
        futures.add(createSearchFuture(() -> getGGangUukk(name, token), "getGGangUukk"));
        return futures;
    }

    private List<String> executeSearchAndCollectResults(String text) {
        List<CompletableFuture<String>> futures = createKkqwsSearchFutures(text);
        List<String> allResults = new ArrayList<>();
        
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
        
        log.info("多线程数据获取完成，共收集到 {} 个有效结果", allResults.size());
        return allResults;
    }
    // ================================ 结果处理方法 ================================

    private String formatKkqwsSearchResult(String jsonResponse, String text) {
        try {
            SearchResult result = JsonUtils.fromJson(jsonResponse, SearchResult.class);
            if (result == null || result.getList() == null || result.getList().isEmpty()) {
                return "未找到相关内容。";
            }
            
            StringBuilder finalResult = new StringBuilder();
            finalResult.append(text).append(":\n");

            List<String> allLinks = extractLinksFromAnswers(result.getList());
            finalResult.append(String.join("\n", allLinks));
            System.out.println(finalResult.toString());
            return finalResult.toString();

        } catch (Exception e) {
            log.error("格式化搜索结果失败, JSON: {}", jsonResponse, e);
            return jsonResponse;
        }
    }

    private List<String> extractLinksFromAnswers(List<AnswerItem> answerItems) {
        List<String> allLinks = new ArrayList<>();
        Pattern linkPattern = Pattern.compile("https?:\\/\\/[^\\s\"'<>]+");
        
        for (AnswerItem item : answerItems) {
            if (StrUtil.isNotBlank(item.getAnswer())) {
                Matcher matcher = linkPattern.matcher(item.getAnswer());
                while (matcher.find()) {
                    allLinks.add(matcher.group());
                }
            }
        }
        return allLinks;
    }

    private String processAndFormatResults(List<String> allResults, String text) {
        List<ResultItem> processedItems = new ArrayList<>();
        
        for (String jsonResult : allResults) {
            processedItems.addAll(parseJsonToResultItems(jsonResult, text));
        }
        
        if (processedItems.isEmpty()) {
            log.warn("未能从 JSON 数据中提取到有效的内容项");
            return "未找到相关内容";
        }
        
        return formatResultItems(processedItems);
    }

    private List<ResultItem> parseJsonToResultItems(String jsonResult, String defaultTitle) {
        List<ResultItem> items = new ArrayList<>();
        
        try {
            SearchResult searchResult = JsonUtils.fromJson(jsonResult, SearchResult.class);
            if (searchResult != null && searchResult.getList() != null) {
                items.addAll(convertAnswerItemsToResultItems(searchResult.getList(), defaultTitle));
            } else {
                items.addAll(parseHtmlContent(jsonResult, defaultTitle));
            }
        } catch (Exception e) {
            log.error("解析 JSON 数据失败，尝试 HTML 解析: {}", jsonResult, e);
            try {
                items.addAll(parseHtmlContent(jsonResult, defaultTitle));
            } catch (Exception htmlE) {
                log.error("HTML 解析也失败: {}", jsonResult, htmlE);
                if (jsonResult.contains("answer") || jsonResult.contains("href")) {
                    ResultItem resultItem = new ResultItem();
                    resultItem.setTitle(defaultTitle);
                    resultItem.setContent(jsonResult);
                    items.add(resultItem);
                }
            }
        }
        
        return items;
    }

    private List<ResultItem> convertAnswerItemsToResultItems(List<AnswerItem> answerItems, String defaultTitle) {
        List<ResultItem> items = new ArrayList<>();
        
        for (AnswerItem item : answerItems) {
            if (StrUtil.isNotBlank(item.getAnswer())) {
                ResultItem resultItem = new ResultItem();
                resultItem.setTitle(StrUtil.isNotBlank(item.getQuestion()) ? item.getQuestion() : defaultTitle);
                resultItem.setContent(item.getAnswer());
                items.add(resultItem);
            }
        }
        
        return items;
    }

    private String formatResultItems(List<ResultItem> processedItems) {
        StringBuilder finalResult = new StringBuilder();
        
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
     * 解析HTML内容，提取标题和href链接
     */
    private List<ResultItem> parseHtmlContent(String htmlContent, String defaultTitle) {
        List<ResultItem> items = new ArrayList<>();
        
        if (StrUtil.isBlank(htmlContent)) {
            return items;
        }
        
        log.info("开始解析HTML内容，长度: {} 字符", htmlContent.length());
        
        Pattern titlePattern = Pattern.compile("【([^】]+)】");
        String[] lines = htmlContent.split("\\r?\\n");
        String currentTitle = defaultTitle;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            Matcher titleMatcher = titlePattern.matcher(line);
            if (titleMatcher.find()) {
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
                
                currentTitle = titleMatcher.group(1);
                currentContent = new StringBuilder();
                log.debug("找到标题: {}", currentTitle);
            } else {
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(line);
            }
        }
        
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
     * 提取内容中的href链接
     */
    private String processContentWithLinks(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        Pattern linkPattern = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>");
        Matcher linkMatcher = linkPattern.matcher(content);
        
        int linkCount = 0;
        
        while (linkMatcher.find()) {
            String hrefUrl = linkMatcher.group(1);
            
            if (linkCount > 0) {
                result.append("\n");
            }
            result.append(hrefUrl);
            linkCount++;
            
            log.debug("提取href链接 {}: {}", linkCount, hrefUrl);
        }
        
        String extractedLinks = result.toString().trim();
        log.debug("href链接提取完成，共提取 {} 个链接，总长度: {}", linkCount, extractedLinks.length());
        
        return extractedLinks;
    }
    
    // ================================ 底层HTTP与并发逻辑 ================================

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

    private CompletableFuture<String> createSearchFuture(Callable<String> task, String operationName) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(task, operationName), executorService);
    }

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
                        Thread.sleep(RETRY_BASE_DELAY * (i + 1));
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

    private boolean isInvalidResult(String result) {
        return result == null || result.contains("此链接失效，请返回首页");
    }
    // ================================ API 实现 ================================

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

    // ================================ HTTP请求方法 ================================

    private String sendKkqwsPostRequest(String path, String text) throws Exception {
        String postData = "name=" + URLEncoder.encode(text, "UTF-8") + "&token=" + apiConfig.getKkqwsToken();
        return sendPostRequest(apiConfig.getBaseUrl() + path, postData, this::setKkqwsHeaders);
    }
    
    private String sendUukkPostRequest(String path, String postData) throws Exception {
        return sendPostRequest(apiConfig.getUukkBaseUrl() + path, postData, this::setUukkHeaders);
    }
    
    private String sendUukkGetRequest(String path) throws Exception {
        return sendGetRequest(apiConfig.getUukkBaseUrl() + path, this::setUukkHeaders);
    }

    private String sendPostRequest(String urlStr, String postData, HeaderSetter headerSetter) throws Exception {
        return sendHttpRequest(urlStr, "POST", postData, headerSetter);
    }

    private String sendGetRequest(String urlStr, HeaderSetter headerSetter) throws Exception {
        return sendHttpRequest(urlStr, "GET", null, headerSetter);
    }

    private String sendHttpRequest(String urlStr, String method, String postData, HeaderSetter headerSetter) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod(method);
        httpConn.setConnectTimeout(TIMEOUT_MILLIS);
        httpConn.setReadTimeout(TIMEOUT_MILLIS);
        
        if ("POST".equals(method)) {
            httpConn.setDoOutput(true);
        }
        
        headerSetter.setHeaders(httpConn);

        if ("POST".equals(method) && postData != null) {
            try (OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream())) {
                writer.write(postData);
                writer.flush();
            }
        }
        
        return handleResponse(httpConn);
    }

    @FunctionalInterface
    private interface HeaderSetter {
        void setHeaders(HttpURLConnection connection);
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
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        httpConn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        httpConn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    }
    
    private void setKkqwsHeaders(HttpURLConnection httpConn) {
        httpConn.setRequestProperty("Host", "m.kkqws.com");
        httpConn.setRequestProperty("Origin", "http://m.kkqws.com");
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        httpConn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
    }
    
    /**
     * 设置 Makifx 请求头
     * 避免 Brotli 压缩，只接受 GZIP 或无压缩
     */
    private void setMakifxHeaders(HttpURLConnection httpConn) {
        httpConn.setRequestProperty("Accept", "application/json, text/plain, */*");
        httpConn.setRequestProperty("Accept-Language", "zh-CN,zh-Hans;q=0.9");
        httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate"); // 不包含 br（Brotli）
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.6 Safari/605.1.15");
        httpConn.setRequestProperty("Referer", "https://sou.makifx.com/");
        httpConn.setRequestProperty("Origin", "https://sou.makifx.com");
        httpConn.setRequestProperty("Sec-Fetch-Site", "same-origin");
        httpConn.setRequestProperty("Sec-Fetch-Mode", "cors");
        httpConn.setRequestProperty("Sec-Fetch-Dest", "empty");
        httpConn.setRequestProperty("Connection", "keep-alive");
        httpConn.setRequestProperty("Cache-Control", "no-cache");
    }
    
    /**
     * 发送 Makifx 搜索请求
     */
    private String sendMakifxRequest(String keyword) throws Exception {
        // 详细记录原始关键词
        log.info("Makifx 搜索原始关键词: [{}], 字符长度: {}, UTF-8字节: {}", 
                keyword, keyword.length(), keyword.getBytes("UTF-8").length);
        
        // 执行URL编码
        String encodedKeyword = URLEncoder.encode(keyword, "UTF-8"); 
        log.info("Makifx URL编码后: [{}]", encodedKeyword);
        
        // 构建完整URL
        String urlStr = "https://sou.makifx.com/?kw=" + encodedKeyword;
        log.info("Makifx 请求URL: {}", urlStr);
        
        // 验证编码是否正确
        String decoded = java.net.URLDecoder.decode(encodedKeyword, "UTF-8");
        log.info("编码验证 - 解码后: [{}], 与原始相同: {}", decoded, keyword.equals(decoded));
        
        URL url = new URL(urlStr);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        
        // 如果是HTTPS连接，设置SSL参数
        if (httpConn instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) httpConn;
            configureMakifxSSL(httpsConn);
        }
        
        httpConn.setRequestMethod("GET");
        httpConn.setConnectTimeout(TIMEOUT_MILLIS);
        httpConn.setReadTimeout(TIMEOUT_MILLIS);
        
        // 设置请求头
        setMakifxHeaders(httpConn);
        
        int responseCode = httpConn.getResponseCode();
        log.info("Makifx API 响应码: {}, 实际请求URL: {}", responseCode, urlStr);
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return handleMakifxResponse(httpConn);
        } else {
            log.error("Makifx HTTP请求失败，响应码: {}", responseCode);
            return "";
        }
    }
    
    /**
     * 配置Makifx HTTPS连接的SSL参数
     */
    private void configureMakifxSSL(HttpsURLConnection httpsConn) {
        try {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            // 为这个连接创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // 设置SSL Socket Factory和Hostname Verifier
            httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
            httpsConn.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            
            log.debug("Makifx HTTPS连接SSL配置完成");
            
        } catch (Exception e) {
            log.warn("Makifx SSL配置失败，将使用默认设置: {}", e.getMessage());
        }
    }
    
    /**
     * 处理 Makifx 响应数据
     */
    private String handleMakifxResponse(HttpURLConnection httpConn) throws IOException {
        InputStream inputStream = httpConn.getInputStream();
        String contentEncoding = httpConn.getContentEncoding();
        String contentType = httpConn.getContentType();
        
        log.info("Makifx 响应头 - Content-Encoding: {}, Content-Type: {}", contentEncoding, contentType);
        
        // 处理压缩格式（GZIP 或 Brotli）
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            inputStream = new GZIPInputStream(inputStream);
            log.info("检测到 GZIP 压缩，已解压");
        } else if ("br".equalsIgnoreCase(contentEncoding)) {
            // Brotli 压缩需要专门的库，这里先记录日志
            log.warn("Makifx API 使用 Brotli 压缩，需要修改请求头以避免压缩");
        }
        
        try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
            String responseContent = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            log.info("Makifx 原始响应长度: {} 字符", responseContent.length());
            
            // 输出响应内容的前500个字符用于调试
            if (responseContent.length() > 0) {
                String preview = responseContent.length() > 500 ? 
                    responseContent.substring(0, 500) + "..." : responseContent;
                log.info("Makifx 响应预览: {}", preview);
            }
            
            return responseContent;
        }
    }
    
    /**
     * 格式化 Makifx 搜索结果
     */
    private String formatMakifxResult(String jsonResponse, String keyword) {
        try {
            // 检查响应内容是否为空或异常
            if (StrUtil.isBlank(jsonResponse)) {
                log.warn("Makifx API 返回空响应");
                return "API 返回空响应";
            }
            
            // 检查是否是HTML响应（可能是错误页面）
            if (jsonResponse.trim().startsWith("<")) {
                log.warn("Makifx API 返回HTML页面，可能被限制访问");
                return "搜索服务暂时不可用，请稍后再试";
            }
            
            // 尝试解析JSON
            MakifxResponse response;
            try {
                response = JsonUtils.fromJson(jsonResponse, MakifxResponse.class);
            } catch (Exception jsonError) {
                log.error("JSON解析失败，尝试检查响应格式: {}", jsonError.getMessage());
                
                // 如果是数组格式，尝试处理
                if (jsonResponse.trim().startsWith("[")) {
                    return handleMakifxArrayResponse(jsonResponse, keyword);
                }
                
                // 如果包含乱码，可能是编码问题
                if (jsonResponse.contains("�")) {
                    log.error("响应内容包含乱码，可能是编码或压缩问题");
                    return "响应数据编码异常，无法解析";
                }
                
                throw jsonError;
            }
            
            if (response == null || response.getCode() != 0 || response.getData() == null) {
                log.warn("Makifx API 返回异常响应: code={}, data=null", 
                        response != null ? response.getCode() : "null");
                return "API 返回数据异常";
            }
            
            return formatMakifxData(response.getData(), keyword);
            
        } catch (Exception e) {
            log.error("格式化 Makifx 搜索结果失败，关键词: {}, 错误: {}", keyword, e.getMessage());
            log.error("错误的响应内容前500字符: {}", 
                     jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse);
            return "结果解析失败: " + e.getMessage();
        }
    }
    
    /**
     * 处理数组格式的Makifx响应
     */
    private String handleMakifxArrayResponse(String jsonResponse, String keyword) {
        try {
            log.info("尝试处理数组格式的Makifx响应");
            // 简单的数组处理，提取URL链接
            List<String> urls = extractUrlsFromText(jsonResponse);
            if (urls.isEmpty()) {
                return "未找到有效链接";
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("🔍 搜索关键词: %s\n", keyword));
            result.append(String.format("📊 找到 %d 个链接\n\n", urls.size()));
            
            for (int i = 0; i < Math.min(urls.size(), 20); i++) {
                result.append(String.format("%d. %s\n", i + 1, urls.get(i)));
            }
            
            if (urls.size() > 20) {
                result.append(String.format("\n... 还有 %d 个链接", urls.size() - 20));
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("处理数组格式响应失败: {}", e.getMessage());
            return "无法解析数组格式响应";
        }
    }
    
    /**
     * 从文本中提取URL链接
     */
    private List<String> extractUrlsFromText(String text) {
        List<String> urls = new ArrayList<>();
        Pattern urlPattern = Pattern.compile("https?://[^\\s\"'<>]+");
        Matcher matcher = urlPattern.matcher(text);
        
        while (matcher.find()) {
            String url = matcher.group();
            if (url.length() > 10 && !urls.contains(url)) {
                urls.add(url);
            }
        }
        
        return urls;
    }
    
    /**
     * 格式化标准的Makifx数据
     */
    private String formatMakifxData(MakifxData data, String keyword) {
        if (data.getTotal() == 0 || data.getMerged_by_type() == null || data.getMerged_by_type().isEmpty()) {
            return "未找到相关资源";
        }
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("🔍 搜索关键词: %s\n", keyword));
        result.append(String.format("📊 总计找到: %d 个资源\n\n", data.getTotal()));
        
        Map<String, List<MakifxItem>> mergedByType = data.getMerged_by_type();
        
        // 按平台分类显示（根据实际API响应调整）
        String[] platforms = {"xunlei", "quark", "baidu", "magnet", "aliyun", "others"};
        String[] platformNames = {"🚀 迅雷网盘", "⚡ 夸克网盘", "☁️ 百度网盘", "🧲 磁力链接", "☁️ 阿里云盘", "🔗 其他资源"};
        
        for (int i = 0; i < platforms.length; i++) {
            List<MakifxItem> items = mergedByType.get(platforms[i]);
            if (items != null && !items.isEmpty()) {
                result.append(String.format("%s (%d个)\n", platformNames[i], items.size()));
                result.append("─────────────────────\n");
                
                for (int j = 0; j < items.size() && j < 8; j++) { // 每个平台最多显示8个
                    MakifxItem item = items.get(j);
                    result.append(String.format("%d. %s\n", j + 1, item.getUrl()));
                    
                    if (StrUtil.isNotBlank(item.getPassword())) {
                        result.append(String.format("   🔑 提取码: %s\n", item.getPassword()));
                    }
                    
                    if (StrUtil.isNotBlank(item.getNote()) && item.getNote().length() <= 80) {
                        result.append(String.format("   📝 备注: %s\n", item.getNote().trim()));
                    }
                    
                    result.append("\n");
                }
                
                if (items.size() > 8) {
                    result.append(String.format("   ... 还有 %d 个资源\n", items.size() - 8));
                }
                
                result.append("\n");
            }
        }
        
        String finalResult = result.toString().trim();
        log.info("Makifx 搜索完成，关键词: {}, 结果长度: {} 字符", keyword, finalResult.length());
        return finalResult;
    }
}
