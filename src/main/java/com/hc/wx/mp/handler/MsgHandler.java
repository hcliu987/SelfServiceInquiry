package com.hc.wx.mp.handler;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.hc.wx.mp.builder.TextBuilder;
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


}
