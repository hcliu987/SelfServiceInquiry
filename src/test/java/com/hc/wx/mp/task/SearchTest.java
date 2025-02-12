package com.hc.wx.mp.task;

import com.hc.wx.mp.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SearchTest {

    @Autowired
    private SearchService service;

    @Test
    public void test() throws Exception {
        String msg = service.searchParallel("三叉戟");
        System.out.println("当前结果："+msg);
    }
}
