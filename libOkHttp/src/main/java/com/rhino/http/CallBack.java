package com.rhino.http;

import androidx.annotation.Nullable;

import com.rhino.http.param.HttpParams;
import com.rhino.log.LogUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @author LuoLin
 * @since Create on 2019/6/13.
 **/
public class CallBack implements Callback {

    public CallBack() {
    }

    public void onStart(String url, Object tag, HttpParams param) {
        LogUtils.i(OkHttpUtils.TAG, "onStart: url = " + url);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        LogUtils.e(OkHttpUtils.TAG, e.toString());
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        LogUtils.i(OkHttpUtils.TAG, "onResponse");
    }

    public void onFileRequestProgressChanged(long completeBytes, long totalBytes, float percent) {
        LogUtils.i(OkHttpUtils.TAG, "onFileRequestProgressChanged: completeBytes = " + completeBytes + ", totalBytes = " + totalBytes + ", percent = " + percent);
    }

    public void onFileRequestFinish(@Nullable File file) {
        LogUtils.i(OkHttpUtils.TAG, "onFileRequestFinish: " + (file == null ? "file is null" : file.getAbsolutePath()));
    }

    public void onFileFailure(String error) {
        LogUtils.e(OkHttpUtils.TAG, "onFileFailure: " + error);
    }
}
