package com.hc.wx.mp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lottery")
public class LotteryProperties {

    private String APPID;
    private String APPSECRET;
}
