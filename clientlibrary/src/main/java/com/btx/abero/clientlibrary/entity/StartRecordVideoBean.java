package com.btx.abero.clientlibrary.entity;


import com.btx.abero.clientlibrary.Constant;

/**
 * Created by abero on 2018/9/27.
 */

public class StartRecordVideoBean {

    public int request = Constant.REQUEST_LOGIN;
    public Data data;

    public static class Data {

        public String prefix;

        public Data(String prefix)
        {
            this.prefix=prefix;
        }
    }
}
