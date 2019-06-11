package com.rhino.http;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;


/**
 * @author LuoLin
 * @since Create on 2019/6/12.
 */
public class Callback implements okhttp3.Callback {

    @Override
    public void onFailure(Call call, IOException e) {

    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {

    }

    public void onFileRequestProgressChanged(int progress) {

    }

    public void onFileRequestSuccess(File file){

    }

    public void onError(String error){

    }

}
