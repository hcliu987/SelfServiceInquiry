package com.hc.wx.mp.controller;

import com.hc.wx.mp.config.ApiConfig;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ApiConfigController {
    private final ApiConfig apiConfig;

    public ApiConfigController(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    @GetMapping
    public ApiConfig getConfig() {
        return apiConfig;
    }

    @PutMapping
    public ApiConfig updateConfig(@RequestBody ApiConfig newConfig) {
        apiConfig.setBaseUrl(newConfig.getBaseUrl());
        apiConfig.setJuziPath(newConfig.getJuziPath());
        apiConfig.setXiaoyuPath(newConfig.getXiaoyuPath());
        apiConfig.setSearchPath(newConfig.getSearchPath());
        apiConfig.setToken(newConfig.getToken());
        return apiConfig;
    }
}