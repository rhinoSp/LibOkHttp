package com.rhino.http.demo.http.respose;

import java.io.Serializable;

/**
 * @author LuoLin
 * @since Create on 2019/9/3.
 **/
public class BaseResponse<T> implements Serializable {

//    /**
//     * 成功
//     */
//    SUCCESS("0000","成功"),
//    /**
//     * 对响应码的一些封装,状态码0001开始
//     */
//    SYSTEM_INTERNAL_ERROR("0001","系统内部错误"),
//    INVALID_PARAM("0002","参数错误,请检查入参"),REQUEST_METHOD_NOT_SUPPORT("0003","Http请求方法不支持"),
//    TOKEN_INVALID("0004","Token无效"),TOKEN_WITHOUT("0005","Token不存在"),
//    DATA_STORAGE_FAILURE("0006","数据存储失败"),REQ_INVALID("0007","数据请求异常"),
//    VIDEO_OPERATE_COMPLETE("0008","视频全部通过或归档，不允许再上传"),TOKEN_ALLOCATED_OVER("0009","Token已分配完"),
//    TOKEN_WITHOUT_VIDEO("0010","Token不存在对应的视频")

    public static final String ERROR_UNKNOWN = "unknown";

    private String code;
    private String msg;
    private T content;

    @Override
    public String toString() {
        return "BaseResponse{" +
                "code='" + code + '\'' +
                ", msg='" + msg + '\'' +
                ", content=" + content +
                '}';
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public boolean isSuccess() {
        return "0000".equals(code);
    }

    public static <T> BaseResponse<T> createUnknownError(Exception e) {
        BaseResponse<T> baseResponse = new BaseResponse<>();
        baseResponse.code = ERROR_UNKNOWN;
        baseResponse.msg = "未知错误: " + e.toString();
        return baseResponse;
    }

}
