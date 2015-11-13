package socketConnection.model;

import com.baidao.data.Jsonable;
import com.google.gson.Gson;

/**
 * Created by Bruce on 1/7/15.
 */
public class ErrorResult implements Jsonable {
    private String msg;
    private int ret;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getRet() {
        return ret;
    }

    public void setRet(int ret) {
        this.ret = ret;
    }

    @Override
    public String toJson() {
        return new Gson().toJson(this);
    }

    public static ErrorResult fromJson(String json) {
        return new Gson().fromJson(json, ErrorResult.class);
    }
}
