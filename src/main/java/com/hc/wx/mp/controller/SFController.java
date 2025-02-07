package com.hc.wx.mp.controller;

import com.hc.wx.mp.entity.SFUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sf/user")
@Api("用户管理")
public class SFController {
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/add")
    @ApiOperation(value = "添加顺丰用户")
    public void addUser(@RequestBody SFUser user) {
        redisTemplate.opsForValue().set("sf:" + user.getPhone(), user);
    }

    @PostMapping("up")
    public void up(@RequestBody String auth) {
        redisTemplate.opsForValue().set("auth",auth);
    }
}
