package com.rhino.http.demo.http.respose;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;
import com.rhino.log.LogUtils;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.List;

/**
 * @author LuoLin
 * @since Create on 2019/6/28.
 **/
public class ResCheckToken implements Serializable {

    /**
     * 分配token
     */
    public String dataToken;
    /**
     * 视频数组
     */
    public List<VideoData> videoData;

    @Override
    public String toString() {
        return "ResCheckToken{" +
                "dataToken='" + dataToken + '\'' +
                ", videoData=" + videoData +
                '}';
    }

    public ResCheckToken() {
    }

    /**
     * token是否有效
     *
     * @return true 有效
     */
    public boolean isTokenValid() {
        return !TextUtils.isEmpty(dataToken);
    }

    public static class VideoData implements Serializable {
        /**
         * 视频状态，0待审核，1审核通过，2.已归档，3审核不通过
         */
        public int dataStatus;
        /**
         * 视频id
         */
        public int dataClass;
        /**
         * 视频名称
         */
        public String dataEcspath;
    }

    public static BaseResponse<ResCheckToken> format(String json) {
        try {
            return new Gson().fromJson(json, new TypeToken<BaseResponse<ResCheckToken>>() {
            }.getType());
        } catch (Exception e) {
            LogUtils.e(e);
            try {
                return new Gson().fromJson(json, new TypeToken<BaseResponse>() {
                }.getType());
            } catch (Exception e1) {
                LogUtils.e(e1);
                return BaseResponse.createUnknownError(e1);
            }
        }
    }


}
