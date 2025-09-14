package com.hc.wx.mp.config;

import com.hc.wx.mp.handler.*;
import lombok.AllArgsConstructor;
import me.chanjar.weixin.common.redis.JedisWxRedisOps;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import me.chanjar.weixin.mp.config.impl.WxMpRedisConfigImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.stream.Collectors;

import static me.chanjar.weixin.common.api.WxConsts.EventType;
import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType.EVENT;

/**
 * 微信公众号配置类
 * 负责初始化微信服务和消息路由器
 */
@AllArgsConstructor
@Configuration
@EnableConfigurationProperties(WxMpProperties.class)
public class WxMpConfiguration {
    
    private final LogHandler logHandler;
    private final NullHandler nullHandler;
    private final MsgHandler msgHandler;
    private final WxMpProperties properties;

    @Bean
    public WxMpService wxMpService() {
        final List<WxMpProperties.MpConfig> configs = this.properties.getConfigs();
        if (configs == null) {
            throw new RuntimeException("大哥，拜托先看下项目首页的说明（readme文件），添加下相关配置，注意别配错了！");
        }

        WxMpService service = new WxMpServiceImpl();
        service.setMultiConfigStorages(configs
            .stream()
            .map(this::buildConfigStorage)
            .collect(Collectors.toMap(WxMpDefaultConfigImpl::getAppId, a -> a, (o, n) -> o)));
        return service;
    }

    private WxMpDefaultConfigImpl buildConfigStorage(WxMpProperties.MpConfig config) {
        WxMpDefaultConfigImpl configStorage;
        
        if (this.properties.isUseRedis()) {
            configStorage = createRedisConfigStorage(config);
        } else {
            configStorage = new WxMpDefaultConfigImpl();
        }

        configStorage.setAppId(config.getAppId());
        configStorage.setSecret(config.getSecret());
        configStorage.setToken(config.getToken());
        configStorage.setAesKey(config.getAesKey());
        
        return configStorage;
    }

    private WxMpDefaultConfigImpl createRedisConfigStorage(WxMpProperties.MpConfig config) {
        final WxMpProperties.RedisConfig redisConfig = this.properties.getRedisConfig();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, redisConfig.getHost(), redisConfig.getPort(),
            redisConfig.getTimeout(), redisConfig.getPassword());
        return new WxMpRedisConfigImpl(new JedisWxRedisOps(jedisPool), config.getAppId());
    }

    @Bean
    public WxMpMessageRouter messageRouter(WxMpService wxMpService) {
        final WxMpMessageRouter newRouter = new WxMpMessageRouter(wxMpService);

        // 记录所有事件的日志 （异步执行）
        newRouter.rule().handler(this.logHandler).next();

        // 点击菜单连接事件
        newRouter.rule().async(false).msgType(EVENT).event(EventType.VIEW).handler(this.nullHandler).end();

        // 默认消息处理
        newRouter.rule().async(false).handler(this.msgHandler).end();

        return newRouter;
    }
}
