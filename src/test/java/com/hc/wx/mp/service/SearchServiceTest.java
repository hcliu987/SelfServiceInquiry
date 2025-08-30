package com.hc.wx.mp.service;

import cn.hutool.http.HttpUtil;
import com.hc.wx.mp.config.ApiConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
@Slf4j
@SpringBootTest
@DisplayName("搜索服务测试")
class SearchServiceTest {

    @Autowired
    private SearchService searchService;
    private static final String LOTTERY_RESULT_URL = "http://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=1";
    @Autowired
    private ApiConfig apiConfig;

    private static final String TEST_TEXT = "漂白";

    @Test
    public void testSearch() {
        String response = HttpUtil.createGet(LOTTERY_RESULT_URL).header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .execute().body();
        System.out.println(response);
    }


    @Test
    @DisplayName("测试彩票接口请求")
    public void testLotteryRequest()   {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=1")
                .header("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                .header("Accept", "*/*")
                .header("Host", "www.cwl.gov.cn")
                .header("Connection", "keep-alive")
                .header("Referer", "http://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=1")
                .header("Cookie", "HMF_CI=5cabd3da5bbc427669409d1929b5ee59021c4775f570587b9e89e18c0830e53217873d7569ce54e50e7305343c3c44eb148db4b9935ce742568ba6e29c0c84a214")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            System.out.println(response.body().string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




}