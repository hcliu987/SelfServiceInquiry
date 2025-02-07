package com.hc.wx.mp.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notice")
public class NoticeProperties {
    private String brakId;
}
