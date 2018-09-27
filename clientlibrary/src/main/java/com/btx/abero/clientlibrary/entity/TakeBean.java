package com.btx.abero.clientlibrary.entity;

/**
 * Created by abero on 2018/9/27.
 */

public class TakeBean {

    public int request;
    public Data data;

    public static class Data {

        public String prefix;

        public Data(String prefix) {
            this.prefix = prefix;
        }
    }
}
