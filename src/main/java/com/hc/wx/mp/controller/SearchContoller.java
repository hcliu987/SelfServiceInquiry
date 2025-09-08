package com.hc.wx.mp.controller;


import com.hc.wx.mp.entity.JsonsRootBean;
import com.hc.wx.mp.service.SearchService;
import com.hc.wx.mp.service.ResultStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.util.List;

@Controller
public class SearchContoller {

    @Autowired
    SearchService searchService;
    
    @Autowired
    ResultStorageService resultStorageService;
    
    /**
     * 封装查询结果,
     */


    /**
     * 显示查询结果页面
     */
    @RequestMapping("/result/{key}")
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
