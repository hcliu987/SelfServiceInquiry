package com.hc.wx.mp.service;


import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.config.ApiConfig;
import com.hc.wx.mp.entity.JsonsRootBean;
import com.hc.wx.mp.entity.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


@Service
@Slf4j
public class SearchService {
    private final ApiConfig apiConfig;
    private final ExecutorService executorService;
    private static final int TIMEOUT_SECONDS = 1;

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
        ((ThreadPoolExecutor) executorService).prestartAllCoreThreads();
    }

    private String sendHttpRequest(String path, String text) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("sendHttpRequest");
        log.info("开始执行HTTP请求，path: {}, text: {}", path, text);
        try {
            URL url = new URL(apiConfig.getBaseUrl() + path);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setConnectTimeout(500);
            httpConn.setReadTimeout(500);

            setCommonHeaders(httpConn);
            sendRequestBody(httpConn, text);

            String result = handleResponse(httpConn);
            log.info("HTTP请求执行成功，结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("HTTP请求执行失败，path: {}, text: {}, 异常: {}", path, text, e.getMessage());
            throw e;
        } finally {
            stopWatch.stop();
            log.info("HTTP请求执行完成，耗时：{}ms", stopWatch.getTotalTimeMillis());
        }
    }

    private void setCommonHeaders(HttpURLConnection httpConn) {
        httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        httpConn.setRequestProperty("Accept", "*/*");
        httpConn.setRequestProperty("Accept-Language", "zh-CN,zh-Hans;q=0.9");
        httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        httpConn.setRequestProperty("Host", "m.kkqws.com");
        httpConn.setRequestProperty("Origin", "http://m.kkqws.com");
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15");
        httpConn.setRequestProperty("Connection", "keep-alive");
        httpConn.setRequestProperty("Referer", "http://m.kkqws.com/app/index.html?name=%E6%B1%9F%E6%B2%B3%E6%97%A5%E4%B8%8A&token=i69");
        httpConn.setRequestProperty("Content-Length", "51");
        httpConn.setRequestProperty("Cookie", "Hm_lpvt_83af2d74162d7cbbebdab5495e78e543=1710471278; Hm_lvt_83af2d74162d7cbbebdab5495e78e543=1710466479");
        httpConn.setRequestProperty("Proxy-Connection", "keep-alive");
        httpConn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    }

    private void sendRequestBody(HttpURLConnection httpConn, String text) throws IOException {
        httpConn.setDoOutput(true);
        try (OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream())) {
            writer.write("name=%" + text + "&token=" + apiConfig.getToken());
            writer.flush();
        }
    }

    private String handleResponse(HttpURLConnection httpConn) throws IOException {
        try (InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream()) {
            
            InputStream stream = responseStream;
            if ("gzip".equals(httpConn.getContentEncoding())) {
                stream = new GZIPInputStream(responseStream);
            }
            
            try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
                return s.hasNext() ? s.next() : "";
            }
        }
    }

    public String getJuzi(String text) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("getJuzi");
        log.info("开始执行getJuzi方法，参数text: {}", text);
        try {
            String result = sendHttpRequest(apiConfig.getJuziPath(), text);
            log.info("getJuzi方法执行成功，结果: {}", result);
            return result;
        } finally {
            stopWatch.stop();
            log.info("getJuzi方法执行完成，耗时：{}ms", stopWatch.getTotalTimeMillis());
        }
    }

    public String getXiaoyu(String text) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("getXiaoyu");
        log.info("开始执行getXiaoyu方法，参数text: {}", text);
        try {
            String result = sendHttpRequest(apiConfig.getXiaoyuPath(), text);
            log.info("getXiaoyu方法执行成功，结果: {}", result);
            return result;
        } finally {
            stopWatch.stop();
            log.info("getXiaoyu方法执行完成，耗时：{}ms", stopWatch.getTotalTimeMillis());
        }
    }

    public String search(String text) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("search");
        log.info("开始执行search方法，参数text: {}", text);
        try {
            String result = sendHttpRequest(apiConfig.getSearchPath(), text);
            log.info("search方法执行成功，结果: {}", result);
            return result;
        } finally {
            stopWatch.stop();
            log.info("search方法执行完成，耗时：{}ms", stopWatch.getTotalTimeMillis());
        }
    }

    public String searchX(String text) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("searchX");
        log.info("开始执行searchX方法，参数text: {}", text);
        try {
            String result = sendHttpRequest(apiConfig.getSearchXpath(), text);
            log.info("searchX方法执行成功，结果: {}", result);
            return result;
        } finally {
            stopWatch.stop();
            log.info("searchX方法执行完成，耗时：{}ms", stopWatch.getTotalTimeMillis());
        }
    }


    public String optimizeJsonFormat(List<String> jsonResponses) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        StringBuffer sb=new StringBuffer();
        for (String jsonResponse : jsonResponses) {
            if (StrUtil.hasEmpty(jsonResponse) || !JSONUtil.isTypeJSON(jsonResponse)) {
                continue;
            }

            JsonsRootBean rootBean = JSONUtil.toBean(jsonResponse, JsonsRootBean.class);
            if (rootBean == null || rootBean.getList() == null) {
                continue;
            }


            for (Lists item : rootBean.getList()) {
                String question = item.getQuestion();
                String answer = item.getAnswer();
                sb.append(question).append(answer);
            }

        }


        return  JSONUtil.toJsonStr(sb.toString());
    }

    public JsonsRootBean analysisJson(String json) {
        JsonsRootBean jsonsRootBean = null;

        if (!StrUtil.hasEmpty(json)) {
            if (JSONUtil.isTypeJSON(json)) {
                jsonsRootBean = JSONUtil.toBean(json, JsonsRootBean.class);
                return jsonsRootBean;
            }

        }
        return jsonsRootBean;
    }

    public String searchParallel(String text) {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        // 并发调用四个搜索方法
        futures.add(createSearchFuture(() -> getJuzi(text), "获取句子"));
        futures.add(createSearchFuture(() -> getXiaoyu(text), "获取小语"));
        futures.add(createSearchFuture(() -> search(text), "搜索"));
        futures.add(createSearchFuture(() -> searchX(text), "搜索X"));

        // 等待所有任务完成并收集结果，添加超时控制
        List<String> responses = futures.stream()
                .map(future -> {
                    try {
                        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        log.warn("搜索请求超过{}秒超时", TIMEOUT_SECONDS);
                        return "";
                    } catch (Exception e) {
                        log.error("搜索请求异常", e);
                        return "";
                    }
                })
                .filter(result -> !result.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // 优化JSON格式
        return optimizeJsonFormat(responses);
    }

    private CompletableFuture<String> createSearchFuture(SearchSupplier supplier, String operationName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                log.error(operationName + "异常", e);
                return "";
            }
        }, executorService);
    }

    @FunctionalInterface
    private interface SearchSupplier {
        String get() throws Exception;
    }

    public String searchAndMerge(String text) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String result = searchParallel(text);

        stopWatch.stop();
        log.info("并发搜索耗时：{}ms", stopWatch.getTotalTimeMillis());

        return result;
    }

    public String searchBest(String text) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("searchBest");
        log.info("开始执行searchBest方法，参数text: {}", text);

        List<CompletableFuture<String>> futures = new ArrayList<>();

        // 并发调用四个搜索方法，使用更短的超时时间
        futures.add(createSearchFuture(() -> getJuzi(text), "获取句子"));
        futures.add(createSearchFuture(() -> getXiaoyu(text), "获取小语"));
        futures.add(createSearchFuture(() -> search(text), "搜索"));
        futures.add(createSearchFuture(() -> searchX(text), "搜索X"));

        // 使用CompletableFuture.anyOf快速返回第一个有效结果
        CompletableFuture<Object> anyResult = CompletableFuture.anyOf(
            futures.toArray(new CompletableFuture[0])
        );

        String bestResult = "";
        try {
            // 等待第一个有效结果，设置更短的超时时间
            Object result = anyResult.get(800, TimeUnit.MILLISECONDS);
            if (result instanceof String && !((String) result).isEmpty() && JSONUtil.isTypeJSON((String) result)) {
                JsonsRootBean rootBean = JSONUtil.toBean((String) result, JsonsRootBean.class);
                if (rootBean != null && rootBean.getList() != null && !rootBean.getList().isEmpty()) {
                    Lists item = rootBean.getList().get(0);
                    bestResult = item.getQuestion() + item.getAnswer();
                }
            }

            // 如果第一个结果无效，尝试获取其他结果
            if (bestResult.isEmpty()) {
                bestResult = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(200, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            return "";
                        }
                    })
                    .filter(r -> !r.isEmpty() && JSONUtil.isTypeJSON(r))
                    .map(r -> {
                        try {
                            JsonsRootBean rootBean = JSONUtil.toBean(r, JsonsRootBean.class);
                            if (rootBean != null && rootBean.getList() != null && !rootBean.getList().isEmpty()) {
                                Lists item = rootBean.getList().get(0);
                                return item.getQuestion() + item.getAnswer();
                            }
                        } catch (Exception e) {
                            log.warn("解析JSON结果异常", e);
                        }
                        return "";
                    })
                    .filter(r -> !r.isEmpty())
                    .findFirst()
                    .orElse("");
            }
        } catch (TimeoutException e) {
            log.warn("搜索请求超时");
        } catch (Exception e) {
            log.error("搜索请求异常", e);
        }

        stopWatch.stop();
        log.info("searchBest方法执行完成，耗时：{}ms，最佳结果长度：{}", 
                stopWatch.getTotalTimeMillis(), bestResult.length());

        return bestResult;
    }
}