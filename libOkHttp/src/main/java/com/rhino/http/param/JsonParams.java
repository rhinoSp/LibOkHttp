package com.rhino.http.param;


import java.util.Map;

/**
 * @author LuoLin
 * @since Create on 2019/9/24.
 **/
public class JsonParams extends HttpParams {

    public String bodyJson = "";


    @Override
    public JsonParams addHeader(String key, String value) {
        headerMap.put(key, value);
        return this;
    }

    @Override
    public JsonParams setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
        return this;
    }

    public JsonParams setBodyJson(String json) {
        bodyJson = json;
        return this;
    }

}
