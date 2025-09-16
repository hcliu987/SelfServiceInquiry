package com.hc.wx.mp.controller;

import com.hc.wx.mp.entity.LotteryResult;
import com.hc.wx.mp.service.LotteryService;
import com.hc.wx.mp.service.SearchService;
import com.hc.wx.mp.service.ResultStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API控制器
 * 提供RESTful API接口，支持第三方调用
 */
@Tag(name = "API接口", description = "提供搜索、彩票查询等功能的RESTful API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final SearchService searchService;
    private final LotteryService lotteryService;
    private final ResultStorageService resultStorageService;

    // ================================ 搜索相关API ================================

    @Operation(summary = "多源搜索", description = "调用多个数据源进行并发搜索，返回合并后的结果")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "搜索成功", 
            content = @Content(schema = @Schema(implementation = SearchResponse.class))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @Parameter(description = "搜索关键词", required = true, example = "Java学习资料")
            @RequestBody @NotBlank String keyword) {
        
        log.info("API搜索请求，关键词: {}", keyword);
        
        try {
            String result = searchService.searchAndMergeRaw(keyword);
            SearchResponse response = new SearchResponse();
            response.setSuccess(true);
            response.setMessage("搜索成功");
            response.setData(result);
            response.setKeyword(keyword);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("搜索失败", e);
            SearchResponse response = new SearchResponse();
            response.setSuccess(false);
            response.setMessage("搜索失败: " + e.getMessage());
            response.setKeyword(keyword);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(summary = "快速搜索", description = "使用kkqws数据源进行快速搜索")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @GetMapping("/search/quick")
    public ResponseEntity<SearchResponse> quickSearch(
            @Parameter(description = "搜索关键词", required = true, example = "Python教程")
            @RequestParam @NotBlank String q) {
        
        log.info("API快速搜索请求，关键词: {}", q);
        
        try {
            String result = searchService.searchAndMerge(q);
            SearchResponse response = new SearchResponse();
            response.setSuccess(true);
            response.setMessage("快速搜索成功");
            response.setData(result);
            response.setKeyword(q);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("快速搜索失败", e);
            SearchResponse response = new SearchResponse();
            response.setSuccess(false);
            response.setMessage("快速搜索失败: " + e.getMessage());
            response.setKeyword(q);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(summary = "Makifx资源搜索", description = "专门搜索Makifx网盘资源")
    @GetMapping("/search/makifx")
    public ResponseEntity<SearchResponse> searchMakifx(
            @Parameter(description = "搜索关键词", required = true, example = "编程资料")
            @RequestParam @NotBlank String keyword) {
        
        log.info("API Makifx搜索请求，关键词: {}", keyword);
        
        try {
            String result = searchService.searchMakifx(keyword);
            SearchResponse response = new SearchResponse();
            response.setSuccess(true);
            response.setMessage("Makifx搜索成功");
            response.setData(result);
            response.setKeyword(keyword);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Makifx搜索失败", e);
            SearchResponse response = new SearchResponse();
            response.setSuccess(false);
            response.setMessage("Makifx搜索失败: " + e.getMessage());
            response.setKeyword(keyword);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    // ================================ 彩票相关API ================================

    @Operation(summary = "获取最新彩票信息", description = "获取最新一期双色球开奖信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/lottery/latest")
    public ResponseEntity<LotteryResponse> getLatestLottery() {
        log.info("API获取最新彩票信息请求");
        
        try {
            String lotteryInfo = lotteryService.getLatestLotteryInfo();
            String issue = lotteryService.getLatestLotteryIssue();
            
            LotteryResponse response = new LotteryResponse();
            response.setSuccess(true);
            response.setMessage("获取最新彩票信息成功");
            response.setData(lotteryInfo);
            response.setIssue(issue);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取最新彩票信息失败", e);
            LotteryResponse response = new LotteryResponse();
            response.setSuccess(false);
            response.setMessage("获取失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(summary = "提交彩票号码核对", description = "提交用户彩票号码进行中奖核对")
    @PostMapping("/lottery/check")
    public ResponseEntity<Map<String, Object>> checkLottery(
            @Parameter(description = "彩票号码请求", required = true)
            @RequestBody LotteryCheckRequest request) {
        
        log.info("API彩票核对请求，用户: {}, 号码数量: {}", request.getOpenid(), request.getNumbers().size());
        
        try {
            // 异步处理彩票核对
            lotteryService.processLotteryForUser(request.getOpenid(), request.getNumbers());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "彩票号码已提交核对，结果将通过推送发送");
            response.put("openid", request.getOpenid());
            response.put("numbersCount", request.getNumbers().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("彩票核对失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "彩票核对失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    // ================================ 结果管理API ================================

    @Operation(summary = "获取搜索结果", description = "根据结果Key获取搜索结果详情")
    @GetMapping("/result/{key}")
    public ResponseEntity<Map<String, Object>> getResult(
            @Parameter(description = "结果唯一标识符", required = true, example = "abc123")
            @PathVariable @NotBlank String key) {
        
        log.info("API获取搜索结果请求，key: {}", key);
        
        String content = resultStorageService.getResult(key);
        Map<String, Object> response = new HashMap<>();
        
        if (content != null) {
            response.put("success", true);
            response.put("message", "获取结果成功");
            response.put("key", key);
            response.put("content", content);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "结果不存在或已过期");
            response.put("key", key);
            return ResponseEntity.status(404).body(response);
        }
    }

    // ================================ 系统信息API ================================

    @Operation(summary = "获取系统状态", description = "获取API服务状态信息")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "自助查询API服务");
        status.put("version", "1.0.0");
        status.put("status", "running");
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }

    // ================================ 响应实体类 ================================

    @Schema(description = "搜索响应")
    public static class SearchResponse {
        @Schema(description = "请求是否成功")
        private boolean success;
        
        @Schema(description = "响应消息")
        private String message;
        
        @Schema(description = "搜索关键词")
        private String keyword;
        
        @Schema(description = "搜索结果数据")
        private String data;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    @Schema(description = "彩票响应")
    public static class LotteryResponse {
        @Schema(description = "请求是否成功")
        private boolean success;
        
        @Schema(description = "响应消息")
        private String message;
        
        @Schema(description = "彩票期号")
        private String issue;
        
        @Schema(description = "彩票数据")
        private String data;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getIssue() { return issue; }
        public void setIssue(String issue) { this.issue = issue; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    @Schema(description = "彩票核对请求")
    public static class LotteryCheckRequest {
        @Schema(description = "用户OpenID", required = true)
        private String openid;
        
        @Schema(description = "彩票号码列表", required = true, example = "[\"01 02 03 04 05 06-07\", \"08 09 10 11 12 13-14\"]")
        private List<String> numbers;

        // Getters and Setters
        public String getOpenid() { return openid; }
        public void setOpenid(String openid) { this.openid = openid; }
        public List<String> getNumbers() { return numbers; }
        public void setNumbers(List<String> numbers) { this.numbers = numbers; }
    }
}