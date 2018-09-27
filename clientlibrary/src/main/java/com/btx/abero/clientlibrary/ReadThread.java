package com.btx.abero.clientlibrary;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.btx.abero.clientlibrary.entity.StartRecordVideoBean;
import com.btx.abero.clientlibrary.entity.StopRecordVideoBean;
import com.btx.abero.clientlibrary.entity.TakeBean;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by abero on 2018/9/26.
 */

public class ReadThread extends Thread {

    private static final String TAG = "WifiClient";

    private Socket mSocket;
    private Handler mHandler;
    private Gson mGson = new Gson();

    public ReadThread(Handler handler, Socket socket) {
        mSocket = socket;
        mHandler = handler;
    }

    public void startRead() {
        if (!isAlive())
            start();
    }

    public void stopRead() {
        interrupt();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Log.i(TAG, "read ----");
                InputStreamReader inputStreamReader = new InputStreamReader(mSocket.getInputStream());
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                Log.i(TAG, "line=" + line);
                if (!TextUtils.isEmpty(line)) {
                    if (line.contains(String.valueOf(Constant.REQUEST_START_RECORD_VIDEO))) {
                        StartRecordVideoBean startBean = mGson.fromJson(line, StartRecordVideoBean.class);
                        sendMessage(WifiClient.RESPONSE_START_RECORD_VIDEO_WHAT, startBean);
                    } else if (line.contains(String.valueOf(Constant.REQUEST_STOP_RECORD_VIDEO))) {
                        StopRecordVideoBean stopBean = mGson.fromJson(line, StopRecordVideoBean.class);
                        sendMessage(WifiClient.RESPONSE_STOP_RECORD_VIDEO_WHAT, stopBean);
                    } else if (line.contains(String.valueOf(Constant.REQUEST_TAKE_PICTURE))) {
                        TakeBean takeBean = mGson.fromJson(line, TakeBean.class);
                        sendMessage(WifiClient.RESPONSE_TAKE_PICTURE_WHAT, takeBean);
                    }else if(line.contains(String.valueOf(Constant.REQUEST_TAKE_FTP)))
                    {
                        TakeBean takeBean = mGson.fromJson(line, TakeBean.class);
                        sendMessage(WifiClient.RESPONSE_TAKE_FTP_WHAT, takeBean);
                    }

                } else {
                    mHandler.sendEmptyMessage(WifiClient.SERVER_SOCKET_CLOSE_WHAT);
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        try {
            if (mSocket != null)
                mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "read thread exit!!!");
    }

    private void sendMessage(int what, Object obj) {
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        msg.obj = obj;
        mHandler.sendMessage(msg);
    }
}
