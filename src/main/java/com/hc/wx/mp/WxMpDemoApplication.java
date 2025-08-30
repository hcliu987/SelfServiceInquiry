package com.hc.wx.mp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.UnknownHostException;


/**
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@Slf4j
public class WxMpDemoApplication {

    public static void main(String[] args) throws UnknownHostException {

        ConfigurableApplicationContext run = SpringApplication.run(WxMpDemoApplication.class, args);

    }
}
