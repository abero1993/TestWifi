package com.btx.abero.hotspot.entity;

import com.btx.abero.hotspot.Constant;

/**
 * Created by abero on 2018/9/27.
 */

public class StopRecordVideoBean {

    public int response = Constant.REQUEST_STOP_RECORD_VIDEO;
    public StartRecordVideoBean.Data data;

    public static class Data {

        public String prefix;

        public Data(String prefix)
        {
            this.prefix=prefix;
        }
    }
}
