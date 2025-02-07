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
        FileReader fileReader = FileReader.create(new File("/Users/liuhaicheng/Desktop/1.txt"));
        String[] split = fileReader.readString().split("\n");
        for (int i = 0; i < split.length; i++) {
            redisTemplate.opsForList().leftPush("sf",split[i]);
        }

    }

    @Test
    public void test() {
//        String requestBody = "{\"name\":\"sfsyUrl\",\"value\":\"884024720\",\"remarks\":null,\"id\":1}";
//        JSONObject jsonObject = JSONUtil.parseObj(requestBody);
//        jsonObject.put("value","2");
//
//        System.out.println(jsonObject.toString());
//       String auth ="Bearer eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.eyJkYXRhIjoiLWY5M0xtZ0M3X0RfcTFCLXZKNmNaMDM2MXFNd3YwazdPcU9JMWo2SmhPdlpzX2gzeloiLCJpYXQiOjE3MTcwNTU1NTksImV4cCI6MTcxODc4MzU1OX0.wIjJu9j2ilYGcFj8sSFrCCeCApli6-tI5vpzTXQkK53eWKoA256Bi0iJWKlrAq1t";

//       redisTemplate.opsForValue().set("auth",auth);
        System.out.println(redisTemplate.opsForList().range("sf", 0, -1));
    }

    private void processSubList(List<String> results) {

        OkHttpClient client = new OkHttpClient();


        String requestBody = "{\"name\":\"sfsyUrl\",\"value\":\"884024720\",\"remarks\":null,\"id\":1}";
        JSONObject jsonObject = JSONUtil.parseObj(requestBody);

        Request request = new Request.Builder()
                .url("http://139.196.92.81:5700/api/envs?t=1717033389419")
                .put(RequestBody.create(requestBody.getBytes()))
                .header("Host", "139.196.92.81:5700")
                .header("Accept", "application/json, text/plain, */*")
                .header("Authorization", "Bearer eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.eyJkYXRhIjoiLTVfVk5BcDl1WUh5U21SUmpkcXk2QkhpVFYyWW9QcHpjMlpGaEZVbkE5emJxVkxRSndDMVFjV3R5M0p0dEdTIiwiaWF0IjoxNzE1MzIxODM1LCJleHAiOjE3MTcwNDk4MzV9.89aDecMgtwoaI_X-PMu1Ca9-e-XpMDS2HnY7_WZPpUForZz0hLTuvOa1Af96SQ2j")
                .header("Accept-Language", "zh-CN,zh-Hans;q=0.9")
                .header("Content-Type", "application/json")
                .header("Origin", "http://139.196.92.81:5700")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15")
                .header("Referer", "http://139.196.92.81:5700/env")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            response.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}