package com.btx.abero.hotspot.entity;

import com.btx.abero.hotspot.Constant;

/**
 * Created by abero on 2018/9/27.
 */

public class StartRecordVideoBean {

    public int request = Constant.REQUEST_START_RECORD_VIDEO;
    public Data data;

    public static class Data {

        public String prefix;

        public Data(String prefix)
        {
            this.prefix=prefix;
        }
    }
}
