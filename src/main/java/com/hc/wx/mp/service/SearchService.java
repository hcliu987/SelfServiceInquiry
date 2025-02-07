package com.hc.wx.mp.service;


import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.config.ApiConfig;
import com.hc.wx.mp.entity.JsonsRootBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
public class SearchService {
    private final ApiConfig apiConfig;

    public SearchService(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    public String getJuzi(String text) throws Exception {
        URL url = new URL(apiConfig.getBaseUrl() + apiConfig.getJuziPath());
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

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

        httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write("name=%" + text + "&token=i69");
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        if ("gzip".equals(httpConn.getContentEncoding())) {
            responseStream = new GZIPInputStream(responseStream);
        }
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        return response;

    }

    public String getXiaoyu(String text) throws Exception {
        URL url = new URL(apiConfig.getBaseUrl() + apiConfig.getXiaoyuPath());
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

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

        httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write("name=%" + text + "&token=i69");
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        if ("gzip".equals(httpConn.getContentEncoding())) {
            responseStream = new GZIPInputStream(responseStream);
        }
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        return response;
    }

    public String search(String text) throws Exception {
        URL url = new URL(apiConfig.getBaseUrl() + apiConfig.getSearchPath());
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

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

        httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write("name=%" + text + "&token=i69");
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        if ("gzip".equals(httpConn.getContentEncoding())) {
            responseStream = new GZIPInputStream(responseStream);
        }
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        return response;
    }

    public List<JsonsRootBean> result(String text) throws Exception {
        String encode = URLEncoder.encode(text, "UTF-8");
        ArrayList<JsonsRootBean> objects = new ArrayList<>();
        System.out.println();
        objects.add(analysisJson(search(text)));
        objects.add(analysisJson(getJuzi(text)));
        objects.add(analysisJson(getXiaoyu(text)));
        return objects;
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

    public String resultMsg(String text) throws Exception {
        StringBuilder sb = new StringBuilder();
        long startTime = System.currentTimeMillis();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                if (search(text).length() > 40) {
                    if (analysisJson(search(text)).getList() != null) {
                        if (analysisJson(search(text)).getList().size() > 0) {

                            sb.append(analysisJson(search(text)).getList().get(0).getAnswer());
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return sb.toString();
        }, executorService);


        CompletableFuture<String> stringCompletableFuture1 = CompletableFuture.supplyAsync(() -> {

            try {
                if (getJuzi(text).length() > 40) {
                    if (getJuzi(text) != null) {
                        if (analysisJson(getJuzi(text)).getList().size() > 0) {
                            sb.append(analysisJson(getJuzi(text)).getList().get(0).getAnswer());
                        }

                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return sb.toString();
        }, executorService);

        CompletableFuture<String> stringCompletableFuture2 = CompletableFuture.supplyAsync(() -> {
            try {
                if (getJuzi(text).length() > 40) {
                    if (getXiaoyu(text) != null) {
                        if (analysisJson(getXiaoyu(text)).getList().size() > 0) {
                            sb.append(analysisJson(getXiaoyu(text)).getList().get(0).getAnswer());
                        }

                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return sb.toString();
        }, executorService);

        CompletableFuture<Object> allOf = CompletableFuture.anyOf(future,  stringCompletableFuture1, stringCompletableFuture2);
        System.out.println(allOf.get());


        long endTime = System.currentTimeMillis();
        stopWatch.stop();
        System.out.printf("当前方法查询时间: %d 秒. %n", (endTime - startTime) / 1000);
        System.out.printf("当前方法执行时长: %s 秒. %n", stopWatch.getTotalTimeSeconds() + "");
        log.info("当前方法查询时间: %d 秒", (endTime - startTime) / 1000);
        return (String) allOf.get();
    }

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public String searchParallel(String text) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<CompletableFuture<String>> futures = Arrays.asList(
                createSearchFuture(text, this::search),
                createSearchFuture(text, this::getJuzi),
                createSearchFuture(text, this::getXiaoyu)
        );

        try {
            CompletableFuture<String> result = CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(response -> processResponse((String) response));

            String answer = result.get(5, TimeUnit.SECONDS);

            stopWatch.stop();
            log.info("查询耗时: {} 秒", stopWatch.getTotalTimeSeconds());

            return answer;
        } catch (Exception e) {
            log.error("查询失败", e);
            return "查询超时或失败";
        }
    }

    private CompletableFuture<String> createSearchFuture(String text, SearchFunction searchFunction) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return searchFunction.apply(text);
            } catch (Exception e) {
                log.error("查询接口异常", e);
                return "";
            }
        }, executorService);
    }

    private String processResponse(String response) {
        if (StrUtil.isBlank(response) || response.length() <= 40) {
            return "";
        }

        JsonsRootBean result = analysisJson(response);
        if (result != null && result.getList() != null && !result.getList().isEmpty()) {
            return result.getList().get(0).getAnswer();
        }
        return "";
    }

    @FunctionalInterface
    private interface SearchFunction {
        String apply(String text) throws Exception;
    }

    public String searchAndMerge(String text) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<CompletableFuture<String>> futures = Arrays.asList(
                createSearchFuture(text, this::search),
                createSearchFuture(text, this::getJuzi),
                createSearchFuture(text, this::getXiaoyu)
        );

        try {
            CompletableFuture<List<String>> allResults = CompletableFuture.allOf(
                            futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .map(this::processResponse)
                            .filter(StrUtil::isNotBlank)
                            .collect(Collectors.toList()));

            List<String> results = allResults.get(5, TimeUnit.SECONDS);

            String mergedResult = mergeResults(results);

            stopWatch.stop();
            log.info("合并查询耗时: {} 秒", stopWatch.getTotalTimeSeconds());

            return StrUtil.isNotBlank(mergedResult) ? mergedResult : "未找到相关内容";
        } catch (Exception e) {
            log.error("合并查询失败", e);
            return "查询超时或失败";
        }
    }

    private String mergeResults(List<String> results) {
        if (results.isEmpty()) {
            return "";
        }

        // 找出最长的有效结果
        return results.stream()
                .filter(StrUtil::isNotBlank)
                .max(Comparator.comparingInt(String::length))
                .orElse("");
    }
}