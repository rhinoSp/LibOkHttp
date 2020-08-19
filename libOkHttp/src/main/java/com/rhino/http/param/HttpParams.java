package com.rhino.http.param;

import java.util.HashMap;
import java.util.Map;

import okhttp3.CacheControl;

/**
 * @author LuoLin
 * @since Create on 2019/6/12.
 */
public class HttpParams {

    public Map<String, String> headerMap = new HashMap<>(2);
    public CacheControl cacheControl;

    public static FormParams form() {
        return new FormParams();
    }

    public static JsonParams json() {
        return new JsonParams();
    }

    public static FileParams file() {
        return new FileParams();
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
