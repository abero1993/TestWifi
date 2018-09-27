package com.btx.abero.hotspot;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.btx.abero.hotspot.entity.LoginBean;
import com.btx.abero.hotspot.entity.Response;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


/**
 * Created by abero on 2018/9/26.
 */

public class ReadThread extends Thread {

    private static final String TAG = "Hotspot";

    private Socket mSocket;
    private Handler mHandler;
    private Gson mGson;

    public ReadThread(Handler handler, Socket socket) {
        mSocket = socket;
        mGson = new Gson();
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
                Log.i(TAG, "run --");
                BufferedReader reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                String line = reader.readLine();
                Log.i(TAG, "line=" + line);
                if (!TextUtils.isEmpty(line)) {
                    if (line.contains(String.valueOf(Constant.REQUEST_LOGIN))) {
                        LoginBean loginBean = mGson.fromJson(line, LoginBean.class);
                        sendMessage(Hotspot.RECEIVE_LOGIN_WHAT, loginBean);
                    } else if (line.contains(String.valueOf(Constant.REQUEST_START_RECORD_VIDEO))) {
                        Response response = mGson.fromJson(line, Response.class);
                        sendMessage(Hotspot.RESPONSE_START_RECORD_VIDEO_WHAT, response);
                    } else if (line.contains(String.valueOf(Constant.REQUEST_STOP_RECORD_VIDEO))) {
                        Response response = mGson.fromJson(line, Response.class);
                        sendMessage(Hotspot.RESPONSE_STOP_RECORD_VIDEO_WHAT, response);
                    } else if (line.contains(String.valueOf(Constant.REQUEST_TAKE_PICTURE))) {
                        Response response = mGson.fromJson(line, Response.class);
                        sendMessage(Hotspot.RESPONSE_TAKE_PICTURE_WHAT, response);
                    } else if (line.contains(String.valueOf(Constant.REQUEST_TAKE_FTP))) {
                        Response response = mGson.fromJson(line, Response.class);
                        sendMessage(Hotspot.RESPONSE_TAKE_FTP_WHAT, response);
                    }
                } else {
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

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
