package com.rhino.http.param;

import java.util.HashMap;
import java.util.Map;


/**
 * @author LuoLin
 * @since Create on 2019/9/24.
 **/
public class FormParams extends HttpParams {

    public Map<String, String> formBodyMap = new HashMap<>(2);

    @Override
    public FormParams addHeader(String key, String value) {
        headerMap.put(key, value);
        return this;
    }

    @Override
    public FormParams setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
        return this;
    }

    public FormParams addFormBody(String key, String value) {
        formBodyMap.put(key, value);
        return this;
    }

    public FormParams setFormBodyMap(Map<String, String> formBodyMap) {
        this.formBodyMap = formBodyMap;
        return this;
    }

}
