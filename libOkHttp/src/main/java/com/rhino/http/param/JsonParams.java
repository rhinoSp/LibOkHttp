package com.rhino.http.param;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author LuoLin
 * @since Create on 2019/9/24.
 **/
public class JsonParams extends HttpParams {

    /**
     * JSONObject
     */
    public JSONObject jsonObject = new JSONObject();
    /**
     * json转换器，可以在请求前对json进行加密等操作
     */
    private JsonConvert jsonConvert;

    public JsonParams() {
    }

    public static JsonParams create() {
        return new JsonParams();
    }

    public static JsonParams create(String json) {
        JsonParams params = new JsonParams();
        try {
            params.jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params;
    }

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

    public JsonParams setJsonConvert(JsonConvert jsonConvert) {
        this.jsonConvert = jsonConvert;
        return this;
    }

    public JsonParams addProperty(String key, Object value) {
        try {
            jsonObject.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public String toJson() {
        return jsonObject.toString();
    }

    public String buildBodyJson() {
        if (jsonConvert != null) {
            return jsonConvert.format(this);
        }
        return jsonObject.toString();
    }

    public interface JsonConvert {
        String format(JsonParams params);
    }

}
