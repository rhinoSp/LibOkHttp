package com.rhino.http.param;

import java.util.HashMap;
import java.util.Map;

import okhttp3.CacheControl;

/**
 * @author LuoLin
 * @since Create on 2019/6/12.
 */
public class HttpParams {

    /**
     * 请求头参数
     */
    public Map<String, String> headerMap = new HashMap<>(2);
    /**
     * 缓存控制
     */
    public CacheControl cacheControl;

    public HttpParams() {
    }

    public static FormParams form() {
        return FormParams.create();
    }

    public static JsonParams json() {
        return JsonParams.create();
    }

    public static JsonParams json(String json) {
        return JsonParams.create(json);
    }

    public static FileParams file() {
        return FileParams.create();
    }

    public HttpParams setCacheControl(CacheControl cacheControl) {
        this.cacheControl = cacheControl;
        return this;
    }

    public HttpParams addHeader(String key, String value) {
        headerMap.put(key, value);
        return this;
    }

    public HttpParams setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
        return this;
    }

}
