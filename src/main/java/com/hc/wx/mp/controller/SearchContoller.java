package com.hc.wx.mp.controller;


import com.hc.wx.mp.entity.JsonsRootBean;
import com.hc.wx.mp.service.SearchService;
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
    /**
     * 封装查询结果,
     */

    @RequestMapping("/res/show")
    public String show(@RequestParam(name="r") String r, Model model) throws Exception {
        List<JsonsRootBean> result = searchService.result(r);
        //封装要显示到视图中的数据
        model.addAttribute("list", result);
        return "show";

    }


}
