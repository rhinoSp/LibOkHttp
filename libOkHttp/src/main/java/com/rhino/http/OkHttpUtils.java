package com.rhino.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.rhino.http.param.FileParams;
import com.rhino.http.param.FormParams;
import com.rhino.http.param.HeaderField;
import com.rhino.http.param.HttpParams;
import com.rhino.http.param.JsonParams;
import com.rhino.http.param.ParamField;
import com.rhino.log.LogUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * <p>The utils of HTTP.</p>
 * <p>
 * android.permission.INTERNET
 * android.permission.ACCESS_NETWORK_STATE
 * android.permission.WRITE_EXTERNAL_STORAGE
 * android.permission.READ_EXTERNAL_STORAGE
 *
 * @author LuoLin
 * @since Create on 2019/6/12.
 */
public class OkHttpUtils {

    public static final String TAG = OkHttpUtils.class.getSimpleName();
    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    public static final int DEFAULT_TIMEOUT_TIME = 60;

    public Context mContext;
    public OkHttpClient.Builder builder;
    public OkHttpClient okHttpClient;

    public File cacheDirectoryFile;
    public int cacheMaxAge = 60 * 60; // 1h
    public int cacheMaxSize = 1024 * 1024 * 10; // 10MB
    public String[] cacheGetUrls;

    public OkHttpUtils(@NonNull Context context) {
        this(context, null, null);
    }

    public OkHttpUtils(@NonNull Context context, @NonNull OkHttpClient.Builder builder) {
        this(context, builder, null);
    }

    public OkHttpUtils(@NonNull Context context, @NonNull String[] cacheGetUrls) {
        this(context, null, cacheGetUrls);
    }

    public OkHttpUtils(@NonNull Context context, @Nullable OkHttpClient.Builder builder, @Nullable String[] cacheGetUrls) {
        this.mContext = context.getApplicationContext();
        this.builder = builder == null ? buildBuilder() : builder;
        this.cacheGetUrls = cacheGetUrls;
        init();
    }

    protected void init() {
        if (cacheGetUrls != null && cacheGetUrls.length > 0) {
            cacheDirectoryFile = new File(mContext.getCacheDir().getAbsolutePath(), "OkHttpGetCache");
            Cache cache = new Cache(cacheDirectoryFile, cacheMaxSize);
            builder.cache(cache)
                    .addInterceptor(new RequestHeaderInterceptor())
                    .addNetworkInterceptor(new NetCacheInterceptor());
        }
        okHttpClient = builder.build();
    }

    protected OkHttpClient.Builder buildBuilder() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                return getNormalBuilder();
            }
            return getNormalBuilder()
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .hostnameVerifier(new SSLUtils.TrustAllHostnameVerifier());
        } catch (Exception e) {
            LogUtils.e(TAG, e);
        }
        return getNormalBuilder();
    }

    public void rebuild() {
        okHttpClient = builder.build();
    }

    public OkHttpClient.Builder getBuilder() {
        return builder;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public void cancelRequest(Object... tags) {
        if (okHttpClient == null || tags == null) {
            return;
        }
        for (Object tag : tags) {
            for (Call call : okHttpClient.dispatcher().queuedCalls()) {
                Object t = call.request().tag();
                if (t != null && t.equals(tag)) {
                    LogUtils.w(TAG, "Cancel request: " + t.toString());
                    call.cancel();
                }
            }
            for (Call call : okHttpClient.dispatcher().runningCalls()) {
                Object t = call.request().tag();
                if (t != null && t.equals(tag)) {
                    LogUtils.w(TAG, "Cancel request: " + t.toString());
                    call.cancel();
                }
            }
        }
    }

    public void cancelAllRequest() {
        if (okHttpClient == null) {
            return;
        }
        for (Call call : okHttpClient.dispatcher().queuedCalls()) {
            LogUtils.w(TAG, "Cancel request");
            call.cancel();
        }
        for (Call call : okHttpClient.dispatcher().runningCalls()) {
            LogUtils.w(TAG, "Cancel request");
            call.cancel();
        }
    }

    public void doPost(String url, CallBack callBack) {
        doPost(url, null, callBack);
    }

    public void doPost(String url, HttpParams param, CallBack callBack) {
        doPost(url, url, param, callBack);
    }

    public void doPost(String url, Object tag, HttpParams param, CallBack callBack) {
        if (param instanceof FileParams) {
            uploadFile(url, tag, (FileParams) param, callBack);
        } else {
            LogUtils.i(TAG, "-------------------------------------------------");
            LogUtils.i(TAG, "url = " + url);
            if (param instanceof JsonParams) {
                Request.Builder requestBuilder = buildRequestBuilder(param).url(url).tag(tag);
                String bodyJson = ((JsonParams) param).buildBodyJson();
                LogUtils.i(TAG, "BODY: " + bodyJson);
                RequestBody requestBody = RequestBody.create(MEDIA_TYPE, bodyJson);
                Request request = requestBuilder.post(requestBody).build();
                callBack.onStart(url, tag, param);
                okHttpClient.newCall(request).enqueue(callBack);
            } else {
                Request.Builder requestBuilder = buildRequestBuilder(param).url(url).tag(tag);
                FormBody.Builder bodyBuilder = buildFormBodyBuilder(param);
                Request request = requestBuilder.post(bodyBuilder.build()).build();
                callBack.onStart(url, tag, param);
                okHttpClient.newCall(request).enqueue(callBack);
            }
            LogUtils.i(TAG, "-------------------------------------------------");
        }
    }

    public String doPostSync(String url, HttpParams param) {
        LogUtils.i(TAG, "-------------------------------------------------");
        LogUtils.i(TAG, "url = " + url);
        okhttp3.Request.Builder requestBuilder = this.buildRequestBuilder(param).url(url);
        Request request;
        if (param instanceof JsonParams) {
            String bodyJson = ((JsonParams) param).buildBodyJson();
            LogUtils.i(TAG, "BODY: " + bodyJson);
            RequestBody requestBody = RequestBody.create(MEDIA_TYPE, bodyJson);
            request = requestBuilder.post(requestBody).build();
        } else {
            okhttp3.FormBody.Builder bodyBuilder = this.buildFormBodyBuilder(param);
            request = requestBuilder.post(bodyBuilder.build()).build();
        }
        try {
            return okHttpClient.newCall(request).execute().body().string();
        } catch (Exception e) {
            LogUtils.e(TAG, e);
        }
        LogUtils.i(TAG, "-------------------------------------------------");
        return "";
    }

    public void doPut(String url, HttpParams param, CallBack callBack) {
        doPut(url, null, param, callBack);
    }

    public void doPut(String url, Object tag, HttpParams param, CallBack callBack) {
        LogUtils.i(TAG, "-------------------------------------------------");
        LogUtils.i(TAG, "url = " + url);
        okhttp3.Request.Builder requestBuilder = buildRequestBuilder(param).url(url).tag(tag);
        Request request;
        if (param instanceof JsonParams) {
            String bodyJson = ((JsonParams) param).buildBodyJson();
            LogUtils.i(TAG, "BODY: " + bodyJson);
            RequestBody requestBody = RequestBody.create(MEDIA_TYPE, bodyJson);
            request = requestBuilder.put(requestBody).build();
        } else {
            okhttp3.FormBody.Builder bodyBuilder = buildFormBodyBuilder(param);
            request = requestBuilder.put(bodyBuilder.build()).build();
        }
        callBack.onStart(url, tag, param);
        this.okHttpClient.newCall(request).enqueue(callBack);
        LogUtils.i(TAG, "-------------------------------------------------");
    }

    public String doPutSync(String url, Object tag, HttpParams param) {
        LogUtils.i(TAG, "-------------------------------------------------");
        LogUtils.i(TAG, "url = " + url);
        okhttp3.Request.Builder requestBuilder = buildRequestBuilder(param).url(url).tag(tag);
        Request request;
        if (param instanceof JsonParams) {
            String bodyJson = ((JsonParams) param).buildBodyJson();
            LogUtils.i(TAG, "BODY: " + bodyJson);
            RequestBody requestBody = RequestBody.create(MEDIA_TYPE, bodyJson);
            request = requestBuilder.put(requestBody).build();
        } else {
            okhttp3.FormBody.Builder bodyBuilder = buildFormBodyBuilder(param);
            request = requestBuilder.put(bodyBuilder.build()).build();
        }
        try {
            return okHttpClient.newCall(request).execute().body().string();
        } catch (Exception e) {
            LogUtils.e(TAG, e);
        }
        LogUtils.i(TAG, "-------------------------------------------------");
        return "";
    }

    public void doDelete(String url, HttpParams param, CallBack callBack) {
        doDelete(url, null, param, callBack);
    }

    public void doDelete(String url, Object tag, HttpParams param, CallBack callBack) {
        LogUtils.i(TAG, "-------------------------------------------------");
        LogUtils.i(TAG, "url = " + url);
        okhttp3.Request.Builder requestBuilder = buildRequestBuilder(param).url(url).tag(tag);
        Request request;
        if (param instanceof JsonParams) {
            String bodyJson = ((JsonParams) param).buildBodyJson();
            LogUtils.i(TAG, "BODY: " + bodyJson);
            RequestBody requestBody = RequestBody.create(MEDIA_TYPE, bodyJson);
            request = requestBuilder.delete(requestBody).build();
        } else {
            okhttp3.FormBody.Builder bodyBuilder = buildFormBodyBuilder(param);
            request = requestBuilder.delete(bodyBuilder.build()).build();
        }
        callBack.onStart(url, tag, param);
        okHttpClient.newCall(request).enqueue(callBack);
        LogUtils.i(TAG, "-------------------------------------------------");
    }

    public String doDeleteSync(String url, Object tag, HttpParams param) {
        LogUtils.i(TAG, "-------------------------------------------------");
        LogUtils.i(TAG, "url = " + url);
        okhttp3.Request.Builder requestBuilder = buildRequestBuilder(param).url(url).tag(tag);
        Request request;
        if (param instanceof JsonParams) {
            String bodyJson = ((JsonParams) param).buildBodyJson();
            LogUtils.i(TAG, "BODY: " + bodyJson);
            RequestBody requestBody = RequestBody.create(MEDIA_TYPE, bodyJson);
            request = requestBuilder.delete(requestBody).build();
        } else {
            okhttp3.FormBody.Builder bodyBuilder = buildFormBodyBuilder(param);
            request = requestBuilder.delete(bodyBuilder.build()).build();
        }
        try {
            return okHttpClient.newCall(request).execute().body().string();
        } catch (Exception e) {
            LogUtils.e(TAG, e);
        }
        LogUtils.i(TAG, "-------------------------------------------------");
        return "";
    }

    public void doGet(String url, CallBack callBack) {
        doGet(url, null, callBack);
    }

    public void doGet(String url, @Nullable HttpParams param, CallBack callBack) {
        doGet(url, url, param, callBack);
    }

    public void doGet(String url, Object tag, @Nullable HttpParams param, CallBack callBack) {
        LogUtils.i(TAG, "-------------------------------------------------");
        String httpUrl = buildHttpUrl(url, param);
        LogUtils.i(TAG, "url = " + httpUrl);
        Request.Builder requestBuilder = buildRequestBuilder(param).url(httpUrl).tag(tag);
        Request request = requestBuilder.build();
        callBack.onStart(url, tag, param);
        okHttpClient.newCall(request).enqueue(callBack);
        LogUtils.i(TAG, "-------------------------------------------------");
    }

    public String doGetSync(String url, Object tag, @Nullable HttpParams param, CallBack callBack) {
        LogUtils.i(TAG, "-------------------------------------------------");
        String httpUrl = buildHttpUrl(url, param);
        LogUtils.i(TAG, "url = " + httpUrl);
        Request.Builder requestBuilder = buildRequestBuilder(param).url(httpUrl).tag(tag);
        Request request = requestBuilder.build();
        try {
            return okHttpClient.newCall(request).execute().body().string();
        } catch (Exception e) {
            LogUtils.e(TAG, e);
        }
        LogUtils.i(TAG, "-------------------------------------------------");
        return "";
    }

    public void uploadFile(final String url, FileParams param, CallBack callBack) {
        uploadFile(url, url, param, callBack);
    }

    public void uploadFile(final String url, Object tag, FileParams param, CallBack callBack) {
        LogUtils.i(TAG, "-------------------------------------------------");
        LogUtils.i(TAG, "url = " + url);
        Request.Builder requestBuilder = buildRequestBuilder(param).url(url).tag(tag);
        MultipartBody.Builder bodyBuilder = buildMultipartBodyBuilder(param)
                .setType(MultipartBody.FORM);
        if (param != null && param.fileMap != null) {
            Map<String, List<File>> fileMap = param.fileMap;
            for (Map.Entry<String, List<File>> entry : fileMap.entrySet()) {
                List<File> list = entry.getValue();
                for (File f : list) {
                    LogUtils.i(TAG, "filePath = " + f.getPath());
                    bodyBuilder.addFormDataPart(entry.getKey(), f.getName(), RequestBody.create(MediaType.parse(param.mediaType), f));
                }
            }
        }
        Request request = requestBuilder.url(url)
                .post(new FileProgressRequestBody(bodyBuilder.build(), callBack)).build();
        callBack.onStart(url, tag, param);
        okHttpClient.newCall(request).enqueue(new CallBack() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.e(TAG, e.toString());
                callBack.onFailure(call, e);
                callBack.onFileFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callBack.onResponse(call, response);
            }
        });
        LogUtils.i(TAG, "-------------------------------------------------");
    }

    public void downloadFile(final String url, final String saveFilePath, final CallBack callBack) {
        downloadFile(url, null, saveFilePath, callBack);
    }

    public void downloadFile(final String url, FileParams param, final String saveFilePath, final CallBack callBack) {
        downloadFile(url, url, param, saveFilePath, callBack);
    }

    public void downloadFile(final String url, Object tag, @Nullable FileParams param, final String saveFilePath, final CallBack callBack) {
        LogUtils.i(TAG, "-------------------------------------------------");
        LogUtils.i(TAG, "url = " + url);
        LogUtils.i(TAG, "saveFilePath = " + saveFilePath);
        Request.Builder requestBuilder = buildRequestBuilder(param).url(url).tag(tag);
        File saveFile = new File(saveFilePath);
        int completeBytes = param != null && param.breakpointResumeAble ? getFileSize(saveFile) : 0;
        if ((param == null || !param.breakpointResumeAble) && saveFile.exists()) {
            // 不支持断点续传，文件存在就删除
            saveFile.delete();
        }
        if (completeBytes > 0) {
            // 是否断点续传
            String range = "bytes=" + completeBytes + "-";
            requestBuilder.addHeader("RANGE", range);
            LogUtils.i(TAG, "RANGE = " + range);
        }
        FormBody.Builder bodyBuilder = buildFormBodyBuilder(param);
        FormBody formBody = bodyBuilder.build();
        Request request = requestBuilder.build();
        if (formBody.size() > 0) {
            request = requestBuilder.post(formBody).build();
        }
        callBack.onStart(url, tag, param);
        okHttpClient.newCall(request).enqueue(new CallBack() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.e(TAG, e.toString());
                callBack.onFailure(call, e);
                callBack.onFileFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                BufferedSource source = null;
                BufferedSink sink = null;
                try {
                    callBack.onResponse(call, response);

                    FileProgressResponseBody responseBody = new FileProgressResponseBody(response.body(), param, saveFile, completeBytes, callBack);
                    source = responseBody.source();
                    sink = responseBody.sink();

                    source.readAll(sink);
                    sink.flush();
                    source.close();
                    sink.close();
                    LogUtils.i(TAG, "Download file finish");
                    callBack.onFileRequestFinish(saveFile);
                } catch (Exception e) {
                    LogUtils.e(TAG, e.toString());
                    callBack.onFileFailure(e.getMessage());
                } finally {
                    closeQuietly(source);
                    closeQuietly(sink);
                }
            }
        });
        LogUtils.i(TAG, "-------------------------------------------------");
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private String buildHttpUrl(String url, @Nullable HttpParams param) {
        StringBuilder httpUrl = new StringBuilder(url);
        if (param != null) {
            Class<? extends HttpParams> cls = param.getClass();
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                ParamField paramField = field.getAnnotation(ParamField.class);
                if (paramField != null && !TextUtils.isEmpty(paramField.value())) {
                    try {
                        if (httpUrl.toString().equals(url)) {
                            httpUrl.append("?");
                        }
                        if (field.getType().equals(String[].class)) {
                            String[] arr = (String[]) field.get(param);
                            for (int i = 0; i < arr.length; i++) {
                                if (i != 0) {
                                    httpUrl.append("?");
                                }
                                httpUrl.append(paramField.value() + "[" + i + "]").append("=").append(getNotNullValue(arr[i])).append("&");
                            }
                        } else {
                            httpUrl.append(paramField.value()).append("=").append(getNotNullFileValue(field, param)).
                                    append("&");
                        }

                    } catch (IllegalAccessException e) {
                        LogUtils.e(e);
                    }
                }
            }
            Map<String, String> formBodyMap = null;
            if (param instanceof FormParams && ((FormParams) param).formBodyMap != null) {
                formBodyMap = ((FormParams) param).formBodyMap;
            } else if (param instanceof FileParams && ((FileParams) param).formBodyMap != null) {
                formBodyMap = ((FileParams) param).formBodyMap;
            }
            if (formBodyMap != null) {
                for (Map.Entry<String, String> entry : formBodyMap.entrySet()) {
                    if (httpUrl.toString().equals(url)) {
                        httpUrl.append("?");
                    }
                    httpUrl.append(entry.getKey()).append("=").append(getNotNullValue(entry.getValue())).append("&");
                }
            }
        }
        if (httpUrl.toString().endsWith("&")) {
            return httpUrl.toString().substring(0, httpUrl.length() - 1);
        }
        return httpUrl.toString();
    }

    private Request.Builder buildRequestBuilder(HttpParams param) {
        Request.Builder requestBuilder = new Request.Builder();
        if (param != null) {
            Class<? extends HttpParams> clazz = param.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                HeaderField headerField = field.getAnnotation(HeaderField.class);
                if (headerField != null && !TextUtils.isEmpty(headerField.value())) {
                    try {
                        if (field.getType().equals(String[].class)) {
                            String[] arr = (String[]) field.get(param);
                            for (int i = 0; i < arr.length; i++) {
                                LogUtils.i(TAG, "HEADER: " + headerField.value() + "[" + i + "]" + " = " + getNotNullValue(arr[i]));
                                requestBuilder.addHeader(headerField.value() + "[" + i + "]", getNotNullValue(arr[i]));
                            }
                        } else {
                            LogUtils.i(TAG, "HEADER: " + headerField.value() + " = " + getNotNullFileValue(field, param));
                            requestBuilder.addHeader(headerField.value(), getNotNullFileValue(field, param));
                        }

                    } catch (IllegalAccessException e) {
                        LogUtils.e(e);
                    }
                }
            }
            if (param.headerMap != null) {
                for (Map.Entry<String, String> entry : param.headerMap.entrySet()) {
                    LogUtils.i(TAG, "HEADER: " + entry.getKey() + " = " + getNotNullValue(entry.getValue()));
                    requestBuilder.addHeader(entry.getKey(), getNotNullValue(entry.getValue()));
                }
            }
        }
        return requestBuilder;
    }

    private FormBody.Builder buildFormBodyBuilder(HttpParams param) {
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        if (param != null) {
            Class<? extends HttpParams> clazz = param.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                ParamField paramField = field.getAnnotation(ParamField.class);
                if (paramField != null && !TextUtils.isEmpty(paramField.value())) {
                    try {
                        if (field.getType().equals(String[].class)) {
                            String[] arr = (String[]) field.get(param);
                            for (int i = 0; i < arr.length; i++) {
                                LogUtils.i(TAG, "BODY: " + paramField.value() + "[" + i + "]" + " = " + getNotNullValue(arr[i]));
                                bodyBuilder.add(paramField.value() + "[" + i + "]", getNotNullValue(arr[i]));
                            }
                        } else {
                            LogUtils.i(TAG, "BODY: " + paramField.value() + " = " + getNotNullFileValue(field, param));
                            bodyBuilder.add(paramField.value(), getNotNullFileValue(field, param));
                        }
                    } catch (IllegalAccessException e) {
                        LogUtils.e(e);
                    }
                }
            }
            Map<String, String> formBodyMap = null;
            if (param instanceof FormParams && ((FormParams) param).formBodyMap != null) {
                formBodyMap = ((FormParams) param).formBodyMap;
            } else if (param instanceof FileParams && ((FileParams) param).formBodyMap != null) {
                formBodyMap = ((FileParams) param).formBodyMap;
            }
            if (formBodyMap != null) {
                for (Map.Entry<String, String> entry : formBodyMap.entrySet()) {
                    LogUtils.i(TAG, "BODY: " + entry.getKey() + " = " + getNotNullValue(entry.getValue()));
                    bodyBuilder.add(entry.getKey(), getNotNullValue(entry.getValue()));
                }
            }
        }
        return bodyBuilder;
    }

    private MultipartBody.Builder buildMultipartBodyBuilder(HttpParams param) {
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
        if (param != null) {
            Class<? extends HttpParams> clazz = param.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                ParamField paramField = field.getAnnotation(ParamField.class);
                if (paramField != null && !TextUtils.isEmpty(paramField.value())) {
                    try {
                        if (field.getType().equals(String[].class)) {
                            String[] arr = (String[]) field.get(param);
                            for (int i = 0; i < arr.length; i++) {
                                LogUtils.i(TAG, "BODY: " + paramField.value() + "[" + i + "]" + " = " + getNotNullValue(arr[i]));
                                bodyBuilder.addFormDataPart(paramField.value() + "[" + i + "]", getNotNullValue(arr[i]));
                            }
                        } else {
                            LogUtils.i(TAG, "BODY: " + paramField.value() + " = " + getNotNullFileValue(field, param));
                            bodyBuilder.addFormDataPart(paramField.value(), getNotNullFileValue(field, param));
                        }
                    } catch (IllegalAccessException e) {
                        LogUtils.e(e);
                    }
                }
            }
            Map<String, String> formBodyMap = null;
            if (param instanceof FormParams && ((FormParams) param).formBodyMap != null) {
                formBodyMap = ((FormParams) param).formBodyMap;
            } else if (param instanceof FileParams && ((FileParams) param).formBodyMap != null) {
                formBodyMap = ((FileParams) param).formBodyMap;
            }
            if (formBodyMap != null) {
                for (Map.Entry<String, String> entry : formBodyMap.entrySet()) {
                    LogUtils.i(TAG, "BODY: " + entry.getKey() + " = " + getNotNullValue(entry.getValue()));
                    bodyBuilder.addFormDataPart(entry.getKey(), getNotNullValue(entry.getValue()));
                }
            }
        }
        return bodyBuilder;
    }

    private String getNotNullFileValue(Field field, HttpParams param) {
        try {
            String value = String.valueOf(field.get(param));
            return "null".equals(value) ? "" : value;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getNotNullValue(String value) {
        return value == null || "null".equals(value) ? "" : value;
    }

    public class FileProgressRequestBody extends RequestBody {

        private RequestBody requestBody;
        private CallBack callBack;

        FileProgressRequestBody(RequestBody requestBody, CallBack callBack) {
            this.requestBody = requestBody;
            this.callBack = callBack;
        }

        @Override
        public MediaType contentType() {
            return requestBody.contentType();
        }

        @Override
        public long contentLength() {
            try {
                return requestBody.contentLength();
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        public void writeTo(@NonNull BufferedSink sink) throws IOException {
            BufferedSink bufferedSink = null;
            try {
                bufferedSink = Okio.buffer(new MyForwardingSink(sink, contentLength()));
                bufferedSink.timeout().timeout(120, TimeUnit.SECONDS);
                requestBody.writeTo(bufferedSink);
                bufferedSink.flush();
                LogUtils.i(TAG, "Upload file finish");
                callBack.onFileRequestFinish(null);
            } catch (IOException e) {
                LogUtils.e(TAG, e.getMessage());
                callBack.onFileFailure(e.getMessage());
                closeQuietly(bufferedSink);
                closeQuietly(sink);
                throw e;
            }
        }

        protected final class MyForwardingSink extends ForwardingSink {
            private long writeBytes = 0;
            private long totalBytes;

            MyForwardingSink(Sink delegate, long totalBytes) {
                super(delegate);
                this.totalBytes = totalBytes;
            }

            @Override
            public void write(Buffer buffer, long byteCount) throws IOException {
                super.write(buffer, byteCount);
                writeBytes += byteCount;
                float percent = 1F * writeBytes / totalBytes;
                LogUtils.i(TAG, "Upload file totalBytes = " + writeBytes + ", totalBytes = " + totalBytes + ", percent = " + percent);
                callBack.onFileRequestProgressChanged(totalBytes, totalBytes, percent);
            }
        }
    }

    public class FileProgressResponseBody extends ResponseBody {
        private final ResponseBody responseBody;
        private final FileParams fileParams;
        private final File saveFile;
        private final int completeBytes;
        private BufferedSource progressSource;
        private BufferedSink bufferedSink;
        private CallBack callBack;

        FileProgressResponseBody(ResponseBody responseBody, FileParams fileParams, File saveFile, int completeBytes, CallBack callBack) {
            this.responseBody = responseBody;
            this.fileParams = fileParams;
            this.completeBytes = completeBytes;
            this.saveFile = saveFile;
            this.callBack = callBack;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (callBack == null) {
                return responseBody.source();
            }
            ProgressInputStream progressInputStream = new ProgressInputStream(responseBody.source().inputStream());
            progressInputStream.setCompleteBytes(completeBytes());
            progressInputStream.setTotalBytes(totalBytes());
            progressSource = Okio.buffer(Okio.source(progressInputStream));
            return progressSource;
        }

        @Override
        public void close() {
            closeQuietly(progressSource);
            closeQuietly(bufferedSink);
        }

        private long completeBytes() {
            return saveFile.exists() && completeBytes > 0 ? completeBytes : 0;
        }

        private long totalBytes() {
            return saveFile.exists() && completeBytes > 0 ? completeBytes + contentLength() : contentLength();
        }

        public BufferedSink sink() throws Exception {
            ProgressOutputStream progressOutputStream;
            if (saveFile.exists() && completeBytes > 0) {
                progressOutputStream = new ProgressOutputStream(saveFile, true);
            } else {
                if (saveFile.exists() && !saveFile.delete()) {
                    LogUtils.e(TAG, "delete file failed.");
                }
                if (!saveFile.getParentFile().exists() && !saveFile.getParentFile().mkdirs()) {
                    LogUtils.e(TAG, "makdir failed.");
                }
                if (!saveFile.exists() && !saveFile.createNewFile()) {
                    LogUtils.e(TAG, "create file failed.");
                }
                progressOutputStream = new ProgressOutputStream(saveFile);
            }
            progressOutputStream.setCompleteBytes(completeBytes());
            progressOutputStream.setTotalBytes(totalBytes());
            bufferedSink = Okio.buffer(Okio.sink(progressOutputStream));
            return bufferedSink;
        }

        protected final class ProgressOutputStream extends FileOutputStream {

            private long completeBytes;
            private long totalBytes;
            private long writeBytes = 0;

            public ProgressOutputStream(File file) throws FileNotFoundException {
                super(file);
            }

            public ProgressOutputStream(File file, boolean append) throws FileNotFoundException {
                super(file, append);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                super.write(b, off, len);
                if (totalBytes < 0) {
                    LogUtils.e(TAG, "File total size is " + totalBytes);
                    callBack.onFileRequestProgressChanged(-1, -1, -1);
                    return;
                }
                if (len >= 0) {
                    writeBytes += len;
                    float percent = 1F * (completeBytes + writeBytes) / totalBytes;
                    callBack.onFileRequestProgressChanged(completeBytes + writeBytes, totalBytes, percent);
                }
            }

            public long getCompleteBytes() {
                return completeBytes;
            }

            public void setCompleteBytes(long completeBytes) {
                this.completeBytes = completeBytes;
            }

            public long getTotalBytes() {
                return totalBytes;
            }

            public void setTotalBytes(long totalBytes) {
                this.totalBytes = totalBytes;
            }
        }

        protected final class ProgressInputStream extends InputStream {
            private final InputStream stream;
            private long completeBytes;
            private long totalBytes;
            private long readBytes = 0;

            ProgressInputStream(InputStream stream) {
                this.stream = stream;
            }

            @Override
            public int read() throws IOException {
                int read = this.stream.read();
                if (totalBytes < 0) {
                    LogUtils.e(TAG, "File total size is " + totalBytes);
                    callBack.onFileRequestProgressChanged(-1, -1, -1);
                    return read;
                }
                if (read >= 0) {
                    float percent = 1F * (completeBytes + readBytes) / totalBytes;
                    //this.callBack.onFileRequestProgressChanged(completeBytes + readBytes, totalBytes, percent);
                    readBytes++;
                }
                return read;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int read = this.stream.read(b, off, len);
                if (totalBytes < 0) {
                    LogUtils.e(TAG, "File total size is " + totalBytes);
                    callBack.onFileRequestProgressChanged(-1, -1, -1);
                    return read;
                }
                if (read >= 0) {
                    float percent = 1F * (completeBytes + readBytes) / totalBytes;
                    //this.callBack.onFileRequestProgressChanged(completeBytes + readBytes, totalBytes, percent);
                    readBytes += read;
                }
                return read;
            }

            @Override
            public void close() {
                closeQuietly(stream);
            }

            public long getCompleteBytes() {
                return completeBytes;
            }

            public void setCompleteBytes(long completeBytes) {
                this.completeBytes = completeBytes;
            }

            public long getTotalBytes() {
                return totalBytes;
            }

            public void setTotalBytes(long totalBytes) {
                this.totalBytes = totalBytes;
            }
        }
    }

    public class RequestHeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();
            String url = request.url().url().toString();
            boolean cacheUrl = cacheGetUrl(url);
            if (cacheUrl) {
                if (!isNetConnected(mContext)) {
                    request = request.newBuilder()
                            .cacheControl(CacheControl.FORCE_CACHE)
                            .build();
                }
            }
            return chain.proceed(request);
        }
    }

    public class NetCacheInterceptor implements Interceptor {
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();
            String url = request.url().url().toString();
            boolean cacheUrl = cacheGetUrl(url);
            Response response = chain.proceed(request);
            if (cacheUrl) {
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxAge(cacheMaxAge, TimeUnit.SECONDS)
                        .build();
                response = response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", cacheControl.toString())
                        .build();
            }
            return response;
        }
    }

    public boolean cacheGetUrl(String url) {
        if (cacheGetUrls != null) {
            url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            for (String s : cacheGetUrls) {
                String cacheUrl = s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
                if (url.contains(cacheUrl)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setCacheMaxAge(int cacheMaxAge) {
        this.cacheMaxAge = cacheMaxAge;
    }

    public void setCacheGetUrls(String[] cacheGetUrls) {
        this.cacheGetUrls = cacheGetUrls;
        init();
    }

    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
        init();
    }

    public boolean clearCache() {
        if (cacheDirectoryFile != null) {
            File[] files = cacheDirectoryFile.listFiles();
            for (File file : files) {
                file.delete();
            }
            return true;
        }
        return true;
    }

    public static OkHttpClient.Builder getNormalBuilder() {
        return new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS);
    }

    public static NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    public static boolean isNetConnected(Context context) {
        NetworkInfo networkInfo = getActiveNetworkInfo(context);
        return networkInfo != null && networkInfo.isConnected();
    }

    public static int getFileSize(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return fis.available();
        } catch (Exception e) {
            LogUtils.e(e);
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    LogUtils.e(e);
                }
            }
        }
        return 0;
    }

}
