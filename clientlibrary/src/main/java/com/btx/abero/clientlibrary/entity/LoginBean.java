package com.btx.abero.clientlibrary.entity;

/**
 * Created by abero on 2018/9/26.
 */

public class LoginBean {

    public int request;

    public Data data;

    public static class Data {

        public Data(String devid)
        {
            this.devid=devid;
        }

        public String devid;
    }
}
