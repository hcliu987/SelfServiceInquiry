package com.hc.wx.mp.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
public class JsonUtils {
    public static <T> T fromJson(String json, Class<T> clazz) {
        Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
        return gson.fromJson(json, clazz);
    }

    public static String toJson(Object obj) {
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
        return gson.toJson(obj);
    }
}
