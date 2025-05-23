package com.hc.wx.mp.task;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.config.LotteryProperties;
import com.hc.wx.mp.config.NoticeProperties;
import me.chanjar.weixin.common.error.WxErrorException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
class TaskTest {

    @Autowired

    private NoticeProperties properties;
    @Autowired
    private LotteryProperties lotteryProperties;
    @Autowired
    RedisTemplate redisTemplate;

    @Test
    void appointmentResults() throws InterruptedException, WxErrorException {
        FileReader fileReader = FileReader.create(new File("/Users/liuhaicheng/Desktop/脚本文件/2.txt"));
        String[] split = fileReader.readString().split("\n");
        for (int i = 0; i < split.length; i++) {
            redisTemplate.opsForList().leftPush("sf",split[i]);
        }

    }

    @Test
    public void test() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(URI.create("http://118.89.200.61:5700/api/envs?t=1739257619559"));
        builder.PUT(HttpRequest.BodyPublishers.ofString("{\"name\":\"sfsyUrl\",\"value\":\"1\",\"remarks\":null,\"id\":53}"));
        builder.setHeader("Accept", "application/json, text/plain, */*");
        builder.setHeader("Origin", "http://118.89.200.61:5700");
        builder.setHeader("Content-Type", "application/json");
        builder.setHeader("Authorization", "");
        builder.setHeader("Accept-Language", "zh-CN,zh-Hans;q=0.9");
        builder.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15");
        builder.setHeader("Accept-Encoding", "gzip, deflate");
        builder.setHeader("Connection", "keep-alive");
        HttpRequest request = builder
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void processSubList() {

        OkHttpClient client = new OkHttpClient();


        String requestBody = "{\"name\":\"sfsyUrl\",\"value\":\"884024720\",\"remarks\":null,\"id\":1}";
        JSONObject jsonObject = JSONUtil.parseObj(requestBody);

        Request request = new Request.Builder()
                .url("http://118.89.200.61:5700/api/envs?t=1717033389419")
                .put(RequestBody.create(requestBody.getBytes()))
                .header("Host", "118.89.200.61:5700")
                .header("Accept", "application/json, text/plain, */*")
                .header("Authorization", "Bearer eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.eyJkYXRhIjoieW9zc0hYNmVzVHpXQy1CQU1rNXZpdlE1ZjdWdEFzUk9nRDRfVVd4STAxNGFIeFZ6bEI3MEpEV2hWbGEwdVRibmduRmlyNG5xYjNyUHJnS1U2ZzhlRThKUmpreTkiLCJpYXQiOjE3Mzc4NTg2MzgsImV4cCI6MTczOTU4NjYzOH0.fjauo3tK-Nv1TD97SbRswBQ5d9orbYkQj9EzJ-F1KB5P8yGwM7RWgWPxJ7bUca-N")
                .header("Accept-Language", "zh-CN,zh-Hans;q=0.9")
                .header("Content-Type", "application/json")
                .header("Origin", "http://118.89.200.61:5700")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15")
                .header("Referer", "http://118.89.200.61:5700/env")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            response.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}