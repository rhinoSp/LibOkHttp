package com.rhino.http.param;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LuoLin
 * @since Create on 2019/9/24.
 **/
public class FileParams extends HttpParams {

    /**
     * 请求参数
     */
    public Map<String, String> formBodyMap = new HashMap<>(2);
    /**
     * mediaType
     */
    public String mediaType = "multipart/form-data";
    /**
     * 上传文件集合
     */
    public Map<String, List<File>> fileMap = new HashMap<>(2);
    /**
     * 是否断点续传
     */
    public boolean breakpointResumeAble = true;

    private FileParams() {
    }

    public static FileParams create() {
        return new FileParams();
    }

    @Override
    public FileParams addHeader(String key, String value) {
        headerMap.put(key, value);
        return this;
    }

    @Override
    public FileParams setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
        return this;
    }

    public FileParams addFormBody(String key, String value) {
        formBodyMap.put(key, value);
        return this;
    }

    public FileParams setFormBodyMap(Map<String, String> formBodyMap) {
        this.formBodyMap = formBodyMap;
        return this;
    }

    public FileParams setMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public FileParams addFile(String key, File file) {
        if (!fileMap.containsKey(key)) {
            fileMap.put(key, new ArrayList<>());
        }
        fileMap.get(key).add(file);
        return this;
    }

    public FileParams setFileMap(Map<String, List<File>> fileMap) {
        this.fileMap = fileMap;
        return this;
    }

    public FileParams setBreakpointResumeAble(boolean breakpointResumeAble) {
        this.breakpointResumeAble = breakpointResumeAble;
        return this;
    }
}
