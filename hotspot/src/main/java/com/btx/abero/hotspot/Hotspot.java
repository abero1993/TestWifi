package com.btx.abero.hotspot;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.btx.abero.hotspot.entity.LoginBean;
import com.btx.abero.hotspot.entity.LoginResult;
import com.btx.abero.hotspot.entity.Response;
import com.btx.abero.hotspot.entity.StartRecordVideoBean;
import com.btx.abero.hotspot.entity.StopRecordVideoBean;
import com.btx.abero.hotspot.entity.TakeBean;
import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by abero on 2018/9/25.
 */

public class Hotspot {

    private static final String TAG = "Hotspot";

    public static final int HOTSPOT_CREATE_WHAT = 1;
    public static final int RECEIVE_LOGIN_WHAT = 2;
    public static final int REQUEST_START_RECORD_VIDEO_WHAT = 3;
    public static final int RESPONSE_START_RECORD_VIDEO_WHAT = 4;
    public static final int REQUEST_STOP_RECORD_VIDEO_WHAT = 5;
    public static final int RESPONSE_STOP_RECORD_VIDEO_WHAT = 6;
    public static final int REQUEST_TAKE_PICTURE_WAHT = 7;
    public static final int RESPONSE_TAKE_PICTURE_WHAT = 8;
    public static final int REQUEST_TAKE_FTP_WAHT = 9;
    public static final int RESPONSE_TAKE_FTP_WHAT = 10;


    private Context mContext;
    private String mDevId;
    private OnClientListener mListener;
    private HandlerThread mHandlerThread;
    private HotspotHandler mHandler;

    private ListenerThread mListenerThread;
    private Socket mSocket;
    private BufferedWriter mWriter;
    private Gson mGson;
    private ReadThread mReadThread;
    private boolean isClientLogin = false;

    public Hotspot(Context context, String devId) {

        mContext = context;
        mDevId = devId;

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new HotspotHandler(mHandlerThread.getLooper());

        mHandler.sendEmptyMessage(HOTSPOT_CREATE_WHAT);

        mGson = new Gson();
    }

    private class HotspotHandler extends Handler implements ListenerThread.OnServerCallback {


        private Map<Integer, Hotspot.Callback> mCallbackList = new HashMap<>(10);

        public HotspotHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            Response response;
            DataHolder dataHolder;
            switch (msg.what) {
                case HOTSPOT_CREATE_WHAT:
                    boolean b = WifiUtils.createWifiHotspot(mContext, mDevId);
                    if (b) {
                        if (mListener != null)
                            mListener.onHotspotCreated();
                    }

                    if (null == mListenerThread) {
                        mListenerThread = new ListenerThread(this, 1343);
                        mListenerThread.startListener();
                    }

                    break;

                case RECEIVE_LOGIN_WHAT:
                    Log.i(TAG, "RECEIVE_LOGIN_WHAT");
                    LoginBean bean = (LoginBean) msg.obj;

                    LoginResult result = new LoginResult();
                    result.response = Constant.REQUEST_LOGIN;
                    if (mDevId.equals(bean.data.devid)) {
                        if (mListener != null)
                            mListener.onClientConnected();

                        result.status = 0;
                        isClientLogin = true;

                    } else {
                        result.status = -1;
                        isClientLogin = false;
                    }

                    send(mGson.toJson(result));

                    break;

                case REQUEST_START_RECORD_VIDEO_WHAT:
                    dataHolder = (DataHolder) msg.obj;
                    StartRecordVideoBean startBean = new StartRecordVideoBean();
                    startBean.data = new StartRecordVideoBean.Data(dataHolder.data);
                    mCallbackList.put(Constant.REQUEST_START_RECORD_VIDEO, dataHolder.callback);
                    send(mGson.toJson(startBean));
                    break;

                case RESPONSE_START_RECORD_VIDEO_WHAT:
                    response = (Response) msg.obj;
                    onResponse(response, Constant.REQUEST_START_RECORD_VIDEO);

                    break;

                case REQUEST_STOP_RECORD_VIDEO_WHAT:
                    dataHolder = (DataHolder) msg.obj;
                    StopRecordVideoBean stopBean = new StopRecordVideoBean();
                    stopBean.data = new StartRecordVideoBean.Data(dataHolder.data);
                    mCallbackList.put(Constant.REQUEST_STOP_RECORD_VIDEO, dataHolder.callback);
                    send(mGson.toJson(stopBean));
                    break;

                case RESPONSE_STOP_RECORD_VIDEO_WHAT:
                    response = (Response) msg.obj;
                    onResponse(response, Constant.REQUEST_STOP_RECORD_VIDEO);
                    break;

                case REQUEST_TAKE_PICTURE_WAHT:
                    dataHolder = (DataHolder) msg.obj;
                    TakeBean takeBean = new TakeBean();
                    takeBean.request = Constant.REQUEST_TAKE_PICTURE;
                    takeBean.data = new TakeBean.Data(dataHolder.data);
                    mCallbackList.put(Constant.REQUEST_TAKE_PICTURE, dataHolder.callback);
                    send(mGson.toJson(takeBean));
                    break;

                case RESPONSE_TAKE_PICTURE_WHAT:
                    response = (Response) msg.obj;
                    onResponse(response, Constant.REQUEST_TAKE_PICTURE);
                    break;

                case REQUEST_TAKE_FTP_WAHT:
                    dataHolder = (DataHolder) msg.obj;
                    TakeBean ftpBean = new TakeBean();
                    ftpBean.request = Constant.REQUEST_TAKE_FTP;
                    mCallbackList.put(Constant.REQUEST_TAKE_FTP, dataHolder.callback);
                    send(mGson.toJson(ftpBean));
                    break;

                case RESPONSE_TAKE_FTP_WHAT:
                    response = (Response) msg.obj;
                    onResponse(response, Constant.REQUEST_TAKE_FTP);
                    break;

                default:
                    break;
            }
        }

        private void onResponse(Response response, int key) {
            if (mCallbackList.containsKey(key)) {
                Hotspot.Callback callback = mCallbackList.get(key);

                if (response.status < 0)
                    callback.onFailed(response.status);
                else
                    callback.onSuccess(response.data.value);
            }

            mCallbackList.remove(key);
        }

        @Override
        public void onAccept(Socket socket) {
            Log.i(TAG, "onAccept: ");
            mSocket = socket;
            try {
                mWriter = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mListener != null)
                mListener.onClientConnected();

            if (mReadThread != null) {
                mReadThread.stopRead();
                mReadThread = null;
                Log.i(TAG, "stop read");
            }

            if (null == mReadThread) {
                mReadThread = new ReadThread(mHandler, socket);
                mReadThread.startRead();
                Log.i(TAG, "start read");
            }
        }

        @Override
        public void onServerOut() {
            Log.i(TAG, "onServerOut: ");
        }
    }

    public void startRecordVideo(String prefix, Callback callback) {
        attempt(callback);
        if (isClientLogin) {
            DataHolder dataHolder = new DataHolder(prefix, callback);
            sendMessage(REQUEST_START_RECORD_VIDEO_WHAT, dataHolder);
        } else {
            callback.onFailed(Constant.RESPONSE_NOT_LOGIN_CODE);
        }
    }

    public void stopRecordVideo(String prefix, Callback callback) {
        attempt(callback);
        if (isClientLogin) {
            DataHolder dataHolder = new DataHolder(prefix, callback);
            sendMessage(REQUEST_STOP_RECORD_VIDEO_WHAT, dataHolder);
        } else {
            callback.onFailed(Constant.RESPONSE_NOT_LOGIN_CODE);
        }
    }

    public void takePicture(String prefix, Callback callback) {
        attempt(callback);
        if (isClientLogin) {
            DataHolder dataHolder = new DataHolder(prefix, callback);
            sendMessage(REQUEST_TAKE_PICTURE_WAHT, dataHolder);
        } else {
            callback.onFailed(Constant.RESPONSE_NOT_LOGIN_CODE);
        }
    }

    public void takeFtp(Callback callback) {
        attempt(callback);
        if (isClientLogin) {
            DataHolder dataHolder = new DataHolder("", callback);
            sendMessage(REQUEST_TAKE_FTP_WAHT, dataHolder);
        } else {
            callback.onFailed(Constant.RESPONSE_NOT_LOGIN_CODE);
        }
    }


    private void send(String str) {
        try {
            if (null == mWriter)
                mWriter = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mWriter.write(str + "\n");
            mWriter.flush();
            Log.i(TAG, "send: " + str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(int what, Object obj) {
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        msg.obj = obj;
        mHandler.sendMessage(msg);
    }

    private void attempt(Callback callback) {
        if (null == callback)
            throw new NullPointerException("callback can not be null!");
    }


    public interface OnClientListener {

        void onHotspotCreated();

        void onClientConnected();

    }

    public void setOnClientListener(OnClientListener onClientListener) {
        mListener = onClientListener;
    }


    public void closeHotspot() {
        WifiUtils.closeWifiHotspot(mContext);
    }


    public interface Callback {

        void onSuccess(String data);

        void onFailed(int status);
    }

    public static class DataHolder {

        public String data;
        public Callback callback;

        public DataHolder(String data, Callback callback) {
            this.data = data;
            this.callback = callback;
        }
    }


}
