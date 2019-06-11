package com.rhino.http;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * <p>The utils of HTTP.</p>
 *
 * @author LuoLin
 * @since Create on 2019/6/12.
 */
public class OkHttpUtils {

    public static final String TAG = OkHttpUtils.class.getSimpleName();
    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    public static final int DEFAULT_TIMEOUT_TIME = 60;

    public OkHttpClient mOkHttpClient;

    public OkHttpUtils() {
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_TIME, TimeUnit.SECONDS)
                .build();
    }

    public void doPost(String url, Callback callback) {
        doPost(url, null, callback);
    }

    public void doPost(String url, HttpParams params, okhttp3.Callback callback) {
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, "url = " + url);
        Request.Builder requestBuilder = new Request.Builder().url(url);
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        if (params != null) {
            Class<? extends HttpParams> clazz = params.getClass();
            Field fields[] = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                ParamField paramField = field.getAnnotation(ParamField.class);
                if (paramField != null && !TextUtils.isEmpty(paramField.value())) {
                    try {
                        Log.i(TAG, paramField.value() + " = " + field.get(params));
                        bodyBuilder.add(paramField.value(), String.valueOf(field.get(params)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Request mRequest = requestBuilder.post(bodyBuilder.build()).build();
        mOkHttpClient.newCall(mRequest).enqueue(callback);
        Log.i(TAG, "-------------------------------------------------");
    }

    public void doPostByMap(String url, Map<String, String> paramsMap, Callback callback) {
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, "url = " + url);
        Request.Builder requestBuilder = new Request.Builder().url(url);
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        if (paramsMap != null) {
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                bodyBuilder.add(entry.getKey(), entry.getValue());
                Log.i(TAG, entry.getKey() + " = " + entry.getValue());
            }
        }
        Request mRequest = requestBuilder.post(bodyBuilder.build()).build();
        mOkHttpClient.newCall(mRequest).enqueue(callback);
        Log.i(TAG, "-------------------------------------------------");
    }

    public void doPostByJson(String url, String json, Callback callback) {
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, "url = " + url);
        Log.i(TAG, "json = " + json);
        RequestBody requestBody = RequestBody.create(MEDIA_TYPE, json);
        Request.Builder requestBuilder = new Request.Builder().url(url);
        Request mRequest = requestBuilder.post(requestBody).build();
        mOkHttpClient.newCall(mRequest).enqueue(callback);
        Log.i(TAG, "-------------------------------------------------");
    }

    public void doGet(String url, HttpParams params, Callback callback) {
        doGet(buildHttpUrl(url, params, null), callback);
    }

    public void doGetByMap(String url, Map<String, String> paramsMap, Callback callback) {
        doGet(buildHttpUrl(url, null, paramsMap), callback);
    }

    public void doGet(String url, Callback callback) {
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, "url = " + url);
        Request.Builder requestBuilder = new Request.Builder().url(url);
        Request request = requestBuilder.build();
        mOkHttpClient.newCall(request).enqueue(callback);
        Log.i(TAG, "-------------------------------------------------");
    }

    public void uploadFile(final String url, Map<String, String> paramsMap, final File file, Callback callback) {
        Log.i(TAG, "-------------------------------------------------");
        String httpUrl = buildHttpUrl(url, null, paramsMap);
        Log.i(TAG, "url = " + httpUrl);
        Log.i(TAG, "filePath = " + file.getPath());
        Request.Builder requestBuilder = new Request.Builder().url(httpUrl);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("multipart/form-data"), file))
                .build();
        Request request = requestBuilder.header("Authorization", "Client-ID " + UUID.randomUUID())
                .url(httpUrl)
                .post(new FileProgressRequestBody(requestBody, file, callback)).build();
        mOkHttpClient.newCall(request).enqueue(callback);
        Log.i(TAG, "-------------------------------------------------");
    }

    public void downloadFile(final String url, final String filePath, final Callback callback) {
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, "url = " + url);
        Log.i(TAG, "filePath = " + filePath);
        Request request = new Request.Builder().url(url).build();
        mOkHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.toString());
                callback.onFailure(call, e);
                callback.onError(e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) {
                InputStream is = null;
                FileOutputStream fos = null;
                byte[] buf = new byte[2048];
                int len;
                try {
                    File file = new File(filePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        Log.i(TAG, "download progress = " + progress + ", total = " + total);
                        //TODO update progress
                        if (progress > 0) {
                            callback.onFileRequestProgressChanged(progress);
                        }
                    }
                    fos.flush();
                    Log.i(TAG, "download success");
                    callback.onFileRequestSuccess(file);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    callback.onError(e.toString());
                } finally {
                    closeQuietly(is);
                    closeQuietly(fos);
                }
            }
        });
        Log.i(TAG, "-------------------------------------------------");
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private String buildHttpUrl(String url, HttpParams params, Map<String, String> paramsMap) {
        StringBuilder httpUrl = new StringBuilder(url);
        if (params != null) {
            Class<? extends HttpParams> clazz = params.getClass();
            Field fields[] = clazz.getDeclaredFields();
            httpUrl.append("?");
            for (Field field : fields) {
                field.setAccessible(true);
                ParamField json = field.getAnnotation(ParamField.class);
                if (json != null && !TextUtils.isEmpty(json.value())) {
                    try {
                        httpUrl.append(json.value() + "=" + field.get(params) + "&");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (paramsMap != null) {
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                if (httpUrl.toString().equals(url)) {
                    httpUrl.append("?");
                }
                httpUrl.append(entry.getKey() + "=" + entry.getValue() + "&");
            }
        }
        if (httpUrl.toString().endsWith("&")) {
            return httpUrl.toString().substring(0, httpUrl.length() - 1);
        }
        return httpUrl.toString();
    }

    public class FileProgressRequestBody extends RequestBody {

        private RequestBody mDelegate;
        private Callback mCallback;
        private File mFile;

        public FileProgressRequestBody(RequestBody delegate, File file, Callback callback) {
            mDelegate = delegate;
            mFile = file;
            mCallback = callback;
        }

        @Override
        public MediaType contentType() {
            return mDelegate.contentType();
        }

        @Override
        public long contentLength() {
            try {
                return mDelegate.contentLength();
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        public void writeTo(@NonNull BufferedSink sink) throws IOException {
            BufferedSink bufferedSink = null;
            try {
                bufferedSink = Okio.buffer(new MyForwardingSink(sink));
                bufferedSink.timeout().timeout(120, TimeUnit.SECONDS);
                mDelegate.writeTo(bufferedSink);
                bufferedSink.flush();
                Log.d(TAG, "upload success " + mFile.toString() + mDelegate.toString());
                mCallback.onFileRequestSuccess(mFile);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                mCallback.onError(e.getMessage());
                bufferedSink.close();
                sink.close();
                throw e;
            }
        }

        protected final class MyForwardingSink extends ForwardingSink {
            private long bytesWritten = 0;
            private int progressTemp = 0;

            public MyForwardingSink(Sink delegate) {
                super(delegate);
            }

            @Override
            public void write(Buffer buffer, long byteCount) throws IOException {
                super.write(buffer, byteCount);
                bytesWritten += byteCount;
                int progress = (int) (100F * bytesWritten / contentLength());
                if (progress > progressTemp) {
                    Log.d(TAG, "upload progress = " + progress);
                    mCallback.onFileRequestProgressChanged(progress);
                    progressTemp = progress;
                }
            }
        }
    }

}
