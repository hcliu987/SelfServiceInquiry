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
 * 微信消息处理器
 * 处理文本消息，支持彩票查询和搜索功能
 */
@NoArgsConstructor
@Data
@Component
public class MsgHandler extends AbstractHandler {

    // ================================ 常量定义 ================================
    private static final int MAX_CONTENT_LENGTH = 100;
    private static final List<String> LOTTERY_KEYWORDS = Arrays.asList("彩票", "双色球", "开奖");
    private static final Pattern LOTTERY_ISSUE_PATTERN = Pattern.compile("^\\d{7}$");

    // ================================ 依赖注入 ================================
    @Autowired
    private SearchService searchService;
    
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

        String content = validateAndPreprocessContent(wxMessage.getContent());
        if (content == null) {
            return buildErrorResponse("请输入有效的搜索内容", wxMessage, weixinService);
        }
        
        String fromUser = wxMessage.getFromUser();
        logger.info("用户 {} 搜索内容: {}", fromUser, content);

        // 处理彩票相关消息
        WxMpXmlOutMessage lotteryResponse = handleLotteryMessage(content, fromUser, wxMessage, weixinService);
        if (lotteryResponse != null) {
            return lotteryResponse;
        }

        // 处理搜索请求
        return handleSearchRequest(content, fromUser, wxMessage, weixinService);
    }

    // ================================ 输入验证方法 ================================

    private String validateAndPreprocessContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        content = content.trim();
        
        if (content.length() > MAX_CONTENT_LENGTH) {
            return null;
        }
        
        return content;
    }

    private WxMpXmlOutMessage buildErrorResponse(String message, WxMpXmlMessage wxMessage, WxMpService weixinService) {
        return new TextBuilder().build(message, wxMessage, weixinService);
    }
    // ================================ 彩票处理方法 ================================

    private WxMpXmlOutMessage handleLotteryMessage(String content, String fromUser, 
                                                   WxMpXmlMessage wxMessage, WxMpService weixinService) {
        // 处理彩票号码
        if (isLotteryNumberFormat(content)) {
            return processLotteryNumbers(content, fromUser, wxMessage, weixinService);
        }
        
        // 处理彩票关键词回复
        if (LOTTERY_KEYWORDS.contains(content)) {
            return handleLotteryKeyword(wxMessage, weixinService);
        }
        
        return null;
    }

    private boolean isLotteryNumberFormat(String content) {
        return content.contains("-") && content.contains(" ");
    }

    private WxMpXmlOutMessage processLotteryNumbers(String content, String fromUser,
                                                    WxMpXmlMessage wxMessage, WxMpService weixinService) {
        String[] lines = content.split("\\r?\\n");
        List<String> numberLines = Arrays.stream(lines)
                .filter(line -> line.contains("-"))
                .collect(Collectors.toList());

        String lastLine = lines[lines.length - 1].trim();
        String targetIssue = null;
        
        if (LOTTERY_ISSUE_PATTERN.matcher(lastLine).matches()) {
            targetIssue = lastLine;
            if (!numberLines.isEmpty()) {
                numberLines.remove(numberLines.size() - 1);
            }
        }

        if (numberLines.isEmpty()) {
            return null;
        }

        String responseContent = processLotteryWithIssue(fromUser, numberLines, targetIssue);
        return new TextBuilder().build(responseContent, wxMessage, weixinService);
    }

    private String processLotteryWithIssue(String fromUser, List<String> numberLines, String targetIssue) {
        if (targetIssue != null) {
            return handleScheduledLottery(fromUser, numberLines, targetIssue);
        } else {
            return handleImmediateLottery(fromUser, numberLines);
        }
    }

    private String handleScheduledLottery(String fromUser, List<String> numberLines, String targetIssue) {
        String latestIssue = lotteryService.getLatestLotteryIssue();
        if (latestIssue != null && targetIssue.compareTo(latestIssue) > 0) {
            lotteryService.scheduleLotteryCheck(fromUser, numberLines, targetIssue);
            return "已为您预约 " + targetIssue + " 期的开奖查询，届时将自动通知您。";
        } else {
            return "预约失败：您提供的期号 " + targetIssue + " 不是一个未来的期号。";
        }
    }

    private String handleImmediateLottery(String fromUser, List<String> numberLines) {
        lotteryService.processLotteryForUser(fromUser, numberLines);
        return "已收到 " + numberLines.size() + " 组号码，开奖后将为您自动核对并通知。";
    }

    private WxMpXmlOutMessage handleLotteryKeyword(WxMpXmlMessage wxMessage, WxMpService weixinService) {
        try {
            String lotteryInfo = lotteryService.getLatestLotteryInfo();
            return new TextBuilder().build(lotteryInfo, wxMessage, weixinService);
        } catch (Exception e) {
            logger.error("获取彩票信息失败", e);
            return new TextBuilder().build("获取彩票信息失败，请稍后再试", wxMessage, weixinService);
        }
    }
    // ================================ 搜索处理方法 ================================

    private WxMpXmlOutMessage handleSearchRequest(String content, String fromUser,
                                                  WxMpXmlMessage wxMessage, WxMpService weixinService) {
        // 检查是否是调试命令
        if (content.startsWith("debug:")) {
            return handleDebugCommand(content, fromUser, wxMessage, weixinService);
        }
        
        // 记录方法开始执行时间
        long methodStartTime = System.currentTimeMillis();
        logger.info("开始处理搜索请求，用户: {}, 查询内容: {}", fromUser, content);
        
        try {
            // 使用多线程并发搜索多个数据源，提升响应速度
            String mergedResult = performConcurrentSearch(content, fromUser);
            logger.info("合并搜索结果:"+mergedResult);
            if (mergedResult == null || mergedResult.length() < 1) {
                String noResultMessage = "🔍 未找到相关内容，请尝试其他关键词";
                long methodExecutionTime = System.currentTimeMillis() - methodStartTime;
                logger.info("搜索无结果，用户: {}, 总执行时间: {:.2f} 秒", fromUser, methodExecutionTime / 1000.0);
                return new TextBuilder().build(noResultMessage, wxMessage, weixinService);
            }
            
            WxMpXmlOutMessage response = generateSearchResponse(mergedResult, fromUser, wxMessage, weixinService);
            
            // 记录方法总执行时间
            long methodExecutionTime = System.currentTimeMillis() - methodStartTime;
            logger.info("搜索请求处理完成，用户: {}, 方法总执行时间: {:.2f} 秒", fromUser, methodExecutionTime / 1000.0);
            
            return response;
            
        } catch (Exception e) {
            long methodExecutionTime = System.currentTimeMillis() - methodStartTime;
            logger.error("搜索服务异常，用户: {}, 查询内容: {}, 执行时间: {:.2f} 秒", 
                        fromUser, content, methodExecutionTime / 1000.0, e);
            return new TextBuilder().build("搜索服务暂时不可用，请稍后再试", wxMessage, weixinService);
        }
    }

    /**
     * 执行并发搜索，整合多个数据源的结果
     * 优化多线程处理，减少总执行时间
     */
    public String performConcurrentSearch(String content, String fromUser) {
        logger.info("开始并发搜索，用户: {}, 查询内容: {}", fromUser, content);
        long startTime = System.currentTimeMillis();
        
        try {
            // 记录各个搜索任务的开始时间
            long kkqwsStartTime = System.currentTimeMillis();
            long makifxStartTime = System.currentTimeMillis();
            
            // 使用 CompletableFuture 并发执行两个搜索任务
            java.util.concurrent.CompletableFuture<String> kkqwsSearchFuture = 
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        long taskStart = System.currentTimeMillis();
                        String result = searchService.searchAndMergeRaw(content);
                        long taskDuration = System.currentTimeMillis() - taskStart;
                        logger.info("KKQWS搜索任务完成，用户: {}, 执行时间: {:.2f} 秒", fromUser, taskDuration / 1000.0);
                        return result;
                    } catch (Exception e) {
                        logger.warn("KKQWS搜索异常，用户: {}, 错误: {}", fromUser, e.getMessage());
                        return "";
                    }
                });
            
            java.util.concurrent.CompletableFuture<String> makifxSearchFuture = 
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        long taskStart = System.currentTimeMillis();
                        // 详细记录传递给searchMakifx的参数
                        logger.info("调用searchMakifx，用户: {}, 传入参数: [{}], 字符长度: {}, 字符编码检查: {}", 
                                   fromUser, content, content.length(), java.util.Arrays.toString(content.toCharArray()));
                        
                        String result = searchService.searchMakifx(content);
                        logger.info("Makifx搜索任务完成，用户: {}, 执行结果:{}",fromUser, result);
                        long taskDuration = System.currentTimeMillis() - taskStart;
                        logger.info("Makifx搜索任务完成，用户: {}, 执行时间: {:.2f} 秒", fromUser, taskDuration / 1000.0);
                        return result;
                    } catch (Exception e) {
                        logger.warn("Makifx搜索异常，用户: {}, 错误: {}", fromUser, e.getMessage());
                        return "";
                    }
                });
            
            // 等待所有搜索任务完成，设置5秒超时
            java.util.concurrent.CompletableFuture<Void> allSearches = 
                java.util.concurrent.CompletableFuture.allOf(kkqwsSearchFuture, makifxSearchFuture);
            
            allSearches.get(5, java.util.concurrent.TimeUnit.SECONDS);
            
            // 获取搜索结果
            String kkqwsResult = kkqwsSearchFuture.get();
            String makifxResult = makifxSearchFuture.get();
            
            // 记录结果合并开始时间
            long mergeStartTime = System.currentTimeMillis();
            String mergedResult = mergeSearchResults(kkqwsResult, makifxResult, content);
            long mergeDuration = System.currentTimeMillis() - mergeStartTime;
            
            long totalExecutionTime = System.currentTimeMillis() - startTime;
            logger.info("并发搜索完成统计 - 用户: {}, 总执行时间: {:.2f} 秒, 结果合并耗时: {:.2f} 秒, 最终结果长度: {} 字符", 
                       fromUser, totalExecutionTime / 1000.0, mergeDuration / 1000.0, mergedResult.length());
            
            return mergedResult;
            
        } catch (java.util.concurrent.TimeoutException e) {
            long timeoutDuration = System.currentTimeMillis() - startTime;
            logger.warn("搜索超时，用户: {}, 内容: {}, 超时时间: {:.2f} 秒", fromUser, content, timeoutDuration / 1000.0);
            // 超时情况下尝试获取已完成的结果
            return getPartialResults(content, fromUser);
        } catch (Exception e) {
            long errorDuration = System.currentTimeMillis() - startTime;
            logger.error("并发搜索异常，用户: {}, 内容: {}, 执行时间: {:.2f} 秒", fromUser, content, errorDuration / 1000.0, e);
            // 异常情况下降级到单个搜索
            return fallbackSingleSearch(content, fromUser);
        }
    }
    
    /**
     * 合并多个数据源的搜索结果
     */
    private String mergeSearchResults(String kkqwsResult, String makifxResult, String keyword) {
        long mergeStartTime = System.currentTimeMillis();
        logger.info("开始合并搜索结果，关键词: {}", keyword);
        
        StringBuilder mergedResult = new StringBuilder();
        
        // 添加搜索结果头部信息
        mergedResult.append("🔍 搜索关键词: ").append(keyword).append("\n\n");
        
        boolean hasKkqwsResult = kkqwsResult != null && !kkqwsResult.trim().isEmpty() 
                                && !kkqwsResult.contains("未找到相关内容");
        boolean hasMakifxResult = makifxResult != null && !makifxResult.trim().isEmpty() 
                                 && !makifxResult.contains("未找到相关资源");
        
        // 添加 KKQWS 搜索结果
        if (hasKkqwsResult) {
            mergedResult.append("📚 KKQWS 搜索结果\n")
                       .append("═══════════════════════════════\n")
                       .append(kkqwsResult.trim())
                       .append("\n\n");
        }
        
        // 添加 Makifx 搜索结果
        if (hasMakifxResult) {
            mergedResult.append("🎬 Makifx 影视资源\n")
                       .append("═══════════════════════════════\n")
                       .append(makifxResult.trim())
                       .append("\n\n");
        }
        
        // 如果都没有结果
        if (!hasKkqwsResult && !hasMakifxResult) {
            long mergeDuration = System.currentTimeMillis() - mergeStartTime;
            logger.warn("合并结果：无有效数据，关键词: {}, 合并耗时: {:.3f} 秒", keyword, mergeDuration / 1000.0);
            return "";
        }
        
        // 添加结果统计
        int sourceCount = (hasKkqwsResult ? 1 : 0) + (hasMakifxResult ? 1 : 0);
        mergedResult.append("\n📊 共整合了 ").append(sourceCount).append(" 个数据源的搜索结果");
        
        String finalResult = mergedResult.toString().trim();
        long mergeDuration = System.currentTimeMillis() - mergeStartTime;
        
        logger.info("搜索结果合并完成 - 关键词: {}, KKQWS: {}, Makifx: {}, 合并耗时: {:.3f} 秒, 最终结果长度: {} 字符", 
                   keyword, hasKkqwsResult, hasMakifxResult, mergeDuration / 1000.0, finalResult.length());
        
        return finalResult;
    }
    
    /**
     * 获取部分已完成的搜索结果（超时情况）
     */
    private String getPartialResults(String content, String fromUser) {
        long startTime = System.currentTimeMillis();
        try {
            // 优先尝试获取快速的搜索结果
            String quickResult = searchService.searchMakifx(content);
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (quickResult != null && !quickResult.trim().isEmpty() && !quickResult.contains("未找到")) {
                logger.info("超时情况下获取到Makifx结果，用户: {}, 执行时间: {:.2f} 秒", fromUser, executionTime / 1000.0);
                return "⚡ 快速搜索结果\n" + quickResult;
            }
            
            logger.warn("超时情况下未获取到有效结果，用户: {}, 尝试时间: {:.2f} 秒", fromUser, executionTime / 1000.0);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.warn("获取部分结果失败，用户: {}, 执行时间: {:.2f} 秒, 错误: {}", 
                       fromUser, executionTime / 1000.0, e.getMessage());
        }
        return "";
    }
    
    /**
     * 降级到单个搜索（异常情况）
     */
    private String fallbackSingleSearch(String content, String fromUser) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("降级执行单个搜索，用户: {}", fromUser);
            String result = searchService.searchAndMergeRaw(content);
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (result != null && !result.trim().isEmpty()) {
                logger.info("降级搜索成功，用户: {}, 执行时间: {:.2f} 秒, 结果长度: {} 字符", 
                           fromUser, executionTime / 1000.0, result.length());
                return "📝 基础搜索结果\n" + result;
            }
            
            logger.warn("降级搜索无结果，用户: {}, 执行时间: {:.2f} 秒", fromUser, executionTime / 1000.0);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("降级搜索也失败，用户: {}, 执行时间: {:.2f} 秒, 错误: {}", 
                        fromUser, executionTime / 1000.0, e.getMessage());
        }
        return "";
    }

    private WxMpXmlOutMessage generateSearchResponse(String searchResult, String fromUser,
                                                     WxMpXmlMessage wxMessage, WxMpService weixinService) {
        long responseStartTime = System.currentTimeMillis();
        logger.info("开始生成搜索响应，用户: {}, 结果长度: {} 字符", fromUser, searchResult.length());
        
        try {
            // 存储合并后的搜索结果
            long storeStartTime = System.currentTimeMillis();
            String resultKey = resultStorageService.storeResult(searchResult);
            long storeDuration = System.currentTimeMillis() - storeStartTime;
            
            // 生成结果链接
            long urlStartTime = System.currentTimeMillis();
            String resultUrl = urlService.generateResultUrl(resultKey);
            String shortUrl = urlService.shortenUrl(resultUrl);
            long urlDuration = System.currentTimeMillis() - urlStartTime;
            
            // 根据结果类型生成不同的响应消息
            long messageStartTime = System.currentTimeMillis();
            String responseMessage = buildResponseMessage(searchResult, shortUrl);
            long messageDuration = System.currentTimeMillis() - messageStartTime;
            
            long totalResponseTime = System.currentTimeMillis() - responseStartTime;
            
            logger.info("用户 {} 响应生成完成 - 总耗时: {:.3f} 秒 (存储: {:.3f}秒, URL生成: {:.3f}秒, 消息构建: {:.3f}秒), 短链接: {}", 
                       fromUser, totalResponseTime / 1000.0, storeDuration / 1000.0, 
                       urlDuration / 1000.0, messageDuration / 1000.0, shortUrl);
            
            return new TextBuilder().build(responseMessage, wxMessage, weixinService);
        } catch (Exception e) {
            long errorResponseTime = System.currentTimeMillis() - responseStartTime;
            logger.error("生成结果链接失败，用户: {}, 执行时间: {:.3f} 秒", fromUser, errorResponseTime / 1000.0, e);
            return new TextBuilder().build("生成结果链接失败，请稍后再试", wxMessage, weixinService);
        }
    }
    
    /**
     * 根据搜索结果内容构建响应消息
     */
    private String buildResponseMessage(String searchResult, String shortUrl) {
        StringBuilder message = new StringBuilder();
        
        // 判断搜索结果类型
        boolean hasKkqws = searchResult.contains("KKQWS 搜索结果");
        boolean hasMakifx = searchResult.contains("Makifx 影视资源");
        boolean isPartial = searchResult.contains("快速搜索结果") || searchResult.contains("基础搜索结果");
        
        if (hasKkqws && hasMakifx) {
            message.append("✨ 全面搜索完成！\n")
                   .append("📊 已整合多个数据源的结果\n")
                   .append("🔗 点击查看详细内容：\n")
                   .append(shortUrl);
        } else if (hasKkqws || hasMakifx) {
            String source = hasKkqws ? "KKQWS" : "Makifx";
            message.append("🔍 搜索完成！\n")
                   .append("📝 来源: ").append(source).append(" 数据库\n")
                   .append("🔗 点击查看详细结果：\n")
                   .append(shortUrl);
        } else if (isPartial) {
            message.append("⚡ 快速搜索完成！\n")
                   .append("📝 部分结果已准备就绪\n")
                   .append("🔗 点击查看：\n")
                   .append(shortUrl);
        } else {
            message.append("🔍 搜索完成！\n")
                   .append("🔗 点击查看详细结果：\n")
                   .append(shortUrl);
        }
        
        return message.toString();
    }
    
    // ================================ 调试命令处理 ================================
    
    /**
     * 处理调试命令
     * 格式: debug:关键词
     */
    private WxMpXmlOutMessage handleDebugCommand(String content, String fromUser,
                                                 WxMpXmlMessage wxMessage, WxMpService weixinService) {
        try {
            String keyword = content.substring(6); // 移除 "debug:" 前缀
            logger.info("处理调试命令，用户: {}, 关键词: [{}]", fromUser, keyword);
            
            String debugResult = searchService.debugMakifxSearch(keyword);
            return new TextBuilder().build(debugResult, wxMessage, weixinService);
            
        } catch (Exception e) {
            logger.error("调试命令处理失败，用户: {}", fromUser, e);
            return new TextBuilder().build("调试命令处理失败: " + e.getMessage(), wxMessage, weixinService);
        }
    }
}
