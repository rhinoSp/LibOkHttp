package com.rhino.http.demo.http.request;

import com.rhino.http.param.HeaderField;
import com.rhino.http.param.HttpParams;
import com.rhino.http.param.ParamField;

/**
 * @author LuoLin
 * @since Create on 2019/9/25.
 **/
public class ReqCheckToken extends HttpParams {

    @HeaderField("dataToken")
    public String[] tokenHeader;

    @ParamField("dataToken")
    public String[] tokenBody;

    public ReqCheckToken(String tokenHeader[], String[] tokenBody) {
        this.tokenHeader = tokenHeader;
        this.tokenBody = tokenBody;
    }

}
