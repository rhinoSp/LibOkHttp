package com.rhino.http.demo;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.rhino.http.CallBack;
import com.rhino.http.OkHttpUtils;
import com.rhino.http.demo.http.respose.BaseResponse;
import com.rhino.http.demo.http.respose.ResCheckToken;
import com.rhino.http.param.FileParams;
import com.rhino.http.param.HttpParams;
import com.rhino.log.LogUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String[] PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    private String[] cacheGetUrl = new String[]{
            "http://www.baidu.com"
    };
    private OkHttpUtils httpUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogUtils.init(this, true, false);
        httpUtils = new OkHttpUtils(getApplicationContext(), cacheGetUrl);

        ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
    }

    public void onViewClick(View view) {
        int id = view.getId();
        if (id == R.id.bt_post) {
            post();
        } else if (id == R.id.bt_get) {
            get();
        } else if (id == R.id.bt_upload_file) {
            uploadFile();
        } else if (id == R.id.bt_download_file) {
            downloadFile();
        } else if (id == R.id.bt_cancel_request) {
            cancelRequest();
        } else if (id == R.id.bt_clear_cache) {
            if (httpUtils.clearCache()) {
                Toast.makeText(this, "删除成功！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "删除失败！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void post() {
//        ReqCheckToken param = new ReqCheckToken("V9f0hTv3cBqfltwqT2P0GFSIuV1A7aQ6dvNktrCjHGk=", new String[]{"V9f0hTv3cBqfltwqT2P0GFSIuV1A7aQ6dvNktrCjHGk=", "xxxx", "123"});
        // 等价于
        HttpParams param = HttpParams.form()
                .addFormBody("dataToken[0]", "V9f0hTv3cBqfltwqT2P0GFSIuV1A7aQ6dvNktrCjHGk=")
                .addFormBody("dataToken[1]", "xxxx")
                .addFormBody("dataToken[2]", "123")
                .addHeader("dataToken", "V9f0hTv3cBqfltwqT2P0GFSIuV1A7aQ6dvNktrCjHGk=");

        httpUtils.doPost("https://www.baidu.com", param, new CallBack() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                super.onResponse(call, response);
                String body = response.body().string();
                LogUtils.d("body = " + body);

                BaseResponse<ResCheckToken> baseResponse = ResCheckToken.format(body);
                LogUtils.d(baseResponse.toString());
                showToast("请求成功！" + body);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                super.onFailure(call, e);
                LogUtils.e(e.toString());
                showToast("请求失败！" + e.toString());
            }
        });
    }

    private void get() {
        httpUtils.doGet("http://www.baidu.com", new CallBack() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                super.onResponse(call, response);
                String body = response.body().string();
                LogUtils.d("body = " + body);
                showToast("请求成功！" + body);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                super.onFailure(call, e);
                LogUtils.e(e.toString());
                showToast("请求失败！" + e.toString());
            }
        });
    }

    private void showToast(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadFile() {
        String appid = "0734c4c3-e14c-40a4-92bf-3a3032dee2f3";
        String url = "https://api.cmburl.cn:8065/ailabcd/kuangshipoc/liveness/faceid/v1/liveness_cfg";
        FileParams httpParams = HttpParams.file()
                .addHeader("appid", appid)
                .addFile("img", new File("/sdcard/Album/20191113154526824836810.jpg"));
        httpUtils.uploadFile(url, null, httpParams, new CallBack() {
            @Override
            public void onFailure(Call call, IOException e) {
                super.onFailure(call, e);
                LogUtils.e(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                super.onResponse(call, response);
                String json = response.body().string();
                LogUtils.d("code = " + response.code() + ", json = " + json);
            }
        });
    }


    private void downloadFile() {
//        String url = "http://49.234.139.59:9000/tpm/app/TPM-v1.0.2-2020-08-13.apk";
//        String filePath = "/sdcard/update.apk";

//        String url = "https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=3153405721,1524067674&fm=26&gp=0.jpg";
//        String filePath = "/sdcard/1/tmp.jpg";
//        httpUtils.download(url, filePath);

//        String url = "https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=3153405721,1524067674&fm=26&gp=0.jpg";
//        String filePath = "/sdcard/1/tmp.jpg";

//        String url = "http://v.ysbang.cn//data/video/2015/rkb/2015rkb01.mp4";
//        String filePath = "/sdcard/1/tmp.mp4";

        // completeBytes = 379747, totalBytes = 78524858
//        String url = "http://117.51.159.28:8100/group1/default/20210124/22/18/1/969f56e7b79e9eabd6c79a0c4834aa2d.MP4?download=0&auth_token=8505A098C0BB43E9B2D5C198A2FCCFB7";
//        String filePath = "/sdcard/1/tmp.MP4";

        // completeBytes = 282793, totalBytes = 16351946
        String token = "89D04DB76A50440C8DA6ADBC5623F846";
        String url = "http://117.51.159.28:8100/group1/default/20210124/22/18/1/efa8d6ad0cb3500d633ce4aae7d91cd1.mov" + "?download=0&auth_token=" + token;
        String saveFilePath = "/sdcard/1/efa8d6ad0cb3500d633ce4aae7d91cd1.mov";
        FileParams fileParams = FileParams.file();

        httpUtils.downloadFile(url, fileParams, saveFilePath, new CallBack() {
            @Override
            public void onStart(String url, Object tag, HttpParams param) {
                super.onStart(url, tag, param);
                showResult("开始下载", "");
            }

            @Override
            public void onFileFailure(String error) {
                super.onFileFailure(error);
                showResult(null, "下载失败，" + error);
            }

            @Override
            public void onFileRequestProgressChanged(long completeBytes, long totalBytes, float percent) {
                super.onFileRequestProgressChanged(completeBytes, totalBytes, percent);
                showResult("文件下载中..." + (int) (100 * percent) + "%", "");
            }

            @Override
            public void onFileRequestFinish(@Nullable File file) {
                super.onFileRequestFinish(file);
                showResult("下载完成", "");
            }
        });
    }

    private void cancelRequest() {
        httpUtils.cancelRequest();
    }

    private void showResult(String msg1, String msg2) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (msg1 != null) {
                    ((TextView) findViewById(R.id.tv_tips1)).setText(msg1);
                }
                if (msg2 != null) {
                    ((TextView) findViewById(R.id.tv_tips2)).setText(msg2);
                }
            }
        });
    }
}
