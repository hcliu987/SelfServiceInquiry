package com.hc.wx.mp.handler;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.builder.TextBuilder;
import com.hc.wx.mp.config.LotteryProperties;
import com.hc.wx.mp.service.SearchService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.*;

import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType;

/**
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
@NoArgsConstructor
@Data
@Component
public class MsgHandler extends AbstractHandler {


    @Autowired
    private LotteryProperties lotteryProperties;
//    @Autowired
//    private RedisCache redisCache;

    @Autowired
    SearchService service;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService weixinService,
                                    WxSessionManager sessionManager) {

        if (!wxMessage.getMsgType().equals(XmlMsgType.EVENT)) {
            //TODO 可以选择将消息保存到本地
        }

        String content = wxMessage.getContent();
        String fromUser = wxMessage.getFromUser();


        try {
            content = service.searchAndMerge(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (content.length() < 1) {
            content = "没查询到相关内容";
        }
        logger.info("当前用户{}查询的内容:{}", fromUser, content);
        return new TextBuilder().build(content, wxMessage, weixinService);

    }

    private void extracted(WxMpXmlMessage wxMessage, String content) {
        String originalString = "http://139.196.92.81/res/show.html?r=" + content;
        String encodedString = Base64.getEncoder().encodeToString(originalString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, Object> paramMap = new HashMap<>();
        System.out.println("获取当前用户输出内容" + wxMessage.getFromUser());
        System.out.println(wxMessage.getToUser());
        paramMap.put("url", encodedString);
        paramMap.put("app_id", lotteryProperties.getAPPID());
        paramMap.put("app_secret", lotteryProperties.getAPPSECRET());
        String result = HttpUtil.get("https://www.mxnzp.com/api/lottery/common/check", paramMap);
        ResultMsg bean = JSONUtil.toBean(result, ResultMsg.class);
        System.out.println(bean.toString());
    }


    @Data
    @ToString
    class ResultMsg {
        private Integer code;
        private String msg;
        private ResultMsgData data;
    }

    @Data
    class ResultMsgData {

        private String shortUrl;
        private String url;
    }


    public static void main(String[] args) throws Exception {
        String encode = URLEncoder.encode("江河日上", "UTF-8");
        String originalString = "http://127.0.0.1/res/show?r=" + encode;
        String encodedString = Base64.getEncoder().encodeToString(originalString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("url", encodedString);
        paramMap.put("app_id", "vinnsglluwk0brol");
        paramMap.put("app_secret", "HbKwaYgoIr1DhFFZ9rFoHEHhHZB1bYUT");
        String result = HttpUtil.get("https://www.mxnzp.com/api/shortlink/create", paramMap);
        ResultMsg bean = JSONUtil.toBean(result, ResultMsg.class);
        System.out.println(bean.data.shortUrl);
        {
            {
                {
                    {
                    }
                }
            }
        }

    }

}
