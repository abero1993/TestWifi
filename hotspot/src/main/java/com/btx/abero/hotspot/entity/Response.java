package com.btx.abero.hotspot.entity;

/**
 * Created by abero on 2018/9/27.
 */

public class Response {

    public int response;
    public int status;

    public Data data;

    public static class Data {
        public String value;

        public Data(String value) {
            this.value = value;
        }

    }

}
