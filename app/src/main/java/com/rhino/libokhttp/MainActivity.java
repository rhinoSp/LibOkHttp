package com.rhino.libokhttp;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.rhino.http.Callback;
import com.rhino.http.OkHttpUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, PERMISSIONS, 11);
    }

    public void onViewClick(View view) {
//        String url = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1560275957304&di=80e1f568bda1dae89bf64699635cd7c2&imgtype=0&src=http%3A%2F%2Fpic37.nipic.com%2F20140113%2F8800276_184927469000_2.png";
        String url = "https://www.sample-videos.com/index.php#sample-mp4-video";
        String filePath = getExternalFilesDir(null).getParent() + File.separator + "video.mp4";
        new OkHttpUtils().downloadFile(url, filePath, new Callback() {
            @Override
            public void onFileRequestProgressChanged(int progress) {
                super.onFileRequestProgressChanged(progress);
            }

            @Override
            public void onFileRequestSuccess(File file) {
                super.onFileRequestSuccess(file);
            }

            @Override
            public void onError(String error) {
                super.onError(error);
            }
        });
    }


}