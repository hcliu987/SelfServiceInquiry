package com.hc.wx.mp.handler;

import com.hc.wx.mp.builder.TextBuilder;
import com.hc.wx.mp.service.LotteryService;
import com.hc.wx.mp.service.SearchService;
import com.hc.wx.mp.service.ResultStorageService;
import com.hc.wx.mp.service.UrlService;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    @Lazy
    @Autowired
    private LotteryService lotteryService;
    
    @Autowired
    private ResultStorageService resultStorageService;
    
    @Autowired
    private UrlService urlService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService weixinService,
                                    WxSessionManager sessionManager) {

        if (!wxMessage.getMsgType().equals(XmlMsgType.EVENT)) {
            //TODO 可以选择将消息保存到本地
        }

        String content = wxMessage.getContent();
        String fromUser = wxMessage.getFromUser();

        //
        // 主要业务逻辑：处理彩票号码和预约
        // --------------------------------------------------------------------------------
        // 检查消息是否包含彩票号码的典型特征（"-" 和 " "）
        if (content.contains("-") && content.contains(" ")) {
            // 将消息按行分割
            String[] lines = content.split("\\r?\\n");
            // 过滤出所有包含 "-" 的行，这些被认为是彩票号码
            List<String> numberLines = Arrays.stream(lines)
                .filter(line -> line.contains("-"))
                .collect(Collectors.toList());

            // 检查最后一行是否为7位数字，这被认为是用户指定的期号
            String lastLine = lines[lines.length - 1].trim();
            String targetIssue = null;
            if (Pattern.matches("^\\d{7}$", lastLine)) {
                targetIssue = lastLine;
                // 如果最后一行是期号，则从号码列表中移除
                if (!numberLines.isEmpty()) {
                    numberLines.remove(numberLines.size() - 1);
                }
            }

            // 如果过滤后仍然有合法的号码行
            if (!numberLines.isEmpty()) {
                String responseContent;
                // 场景1：用户指定了期号
                if (targetIssue != null) {
                    String latestIssue = lotteryService.getLatestLotteryIssue();
                    // 如果指定的期号大于最新的期号，则创建预约任务
                    if (latestIssue != null && targetIssue.compareTo(latestIssue) > 0) {
                        lotteryService.scheduleLotteryCheck(fromUser, numberLines, targetIssue);
                        responseContent = "已为您预约 " + targetIssue + " 期的开奖查询，届时将自动通知您。";
                    } else {
                        // 如果是历史或当前期号，提示用户此功能需要未来期号
                        responseContent = "预约失败：您提供的期号 " + targetIssue + " 不是一个未来的期号。";
                    }
                }
                // 场景2：用户未提供期号，执行立即查询
                else {
                    lotteryService.processLotteryForUser(fromUser, numberLines);
                    responseContent = "已收到 " + numberLines.size() + " 组号码，开奖后将为您自动核对并通知。";
                }
                return new TextBuilder().build(responseContent, wxMessage, weixinService);
            }
        }

        // 新增：关键词回复彩票信息
        List<String> lotteryKeywords = Arrays.asList("彩票", "双色球", "开奖");
        if (lotteryKeywords.contains(content.trim())) {
            String lotteryInfo = lotteryService.getLatestLotteryInfo();
            return new TextBuilder().build(lotteryInfo, wxMessage, weixinService);
        }

        try {
            content = service.searchAndMergeRaw(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (content.length() < 1) {
            content = "没查询到相关内容";
            logger.info("当前用户{}查询的内容:{}", fromUser, content);
            return new TextBuilder().build(content, wxMessage, weixinService);
        }
        
        // 存储搜索结果并生成短链接
        String resultKey = resultStorageService.storeResult(content);
        String resultUrl = urlService.generateResultUrl(resultKey);
        String shortUrl = urlService.shortenUrl(resultUrl);
        
        String responseMessage = "🔍 搜索完成！点击链接查看详细结果：\n" + shortUrl;
        logger.info("当前用户{}搜索完成，返回链接:{}", fromUser, shortUrl);
        return new TextBuilder().build(responseMessage, wxMessage, weixinService);

    }





}
