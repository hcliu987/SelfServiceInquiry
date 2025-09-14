package com.hc.wx.mp.controller;

import com.hc.wx.mp.service.ResultStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 搜索结果控制器
 * 负责处理搜索结果页面展示
 */
@Controller
@RequiredArgsConstructor
public class SearchController {
    
    private final ResultStorageService resultStorageService;
    
    /**
     * 显示查询结果页面
     * 
     * @param key 结果唯一标识符
     * @param model 模型对象
     * @return 视图名称
     */
    @GetMapping("/result/{key}")
    public String showResult(@PathVariable String key, Model model) {
        String content = resultStorageService.getResult(key);
        if (content == null) {
            model.addAttribute("error", "结果不存在或已过期");
            return "error";
        }

        model.addAttribute("content", content);
        model.addAttribute("key", key);
        return "search_result";
    }
}
