package com.btx.abero.clientlibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.btx.abero.clientlibrary.entity.LoginBean;
import com.btx.abero.clientlibrary.entity.Response;
import com.btx.abero.clientlibrary.entity.StartRecordVideoBean;
import com.btx.abero.clientlibrary.entity.StopRecordVideoBean;
import com.btx.abero.clientlibrary.entity.TakeBean;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by abero on 2018/9/26.
 */

public class WifiClient {

    private static final String TAG = "WifiClient";

    public static final int RECEIVE_LOGIN_WHAT = 2;
    public static final int RESPONSE_START_RECORD_VIDEO_WHAT = 3;
    public static final int RESPONSE_STOP_RECORD_VIDEO_WHAT = 4;
    public static final int RESPONSE_TAKE_PICTURE_WHAT = 5;
    public static final int RESPONSE_TAKE_FTP_WHAT=6;

    public static final int SERVER_SOCKET_CLOSE_WHAT = 120;

    private final int SEARCH_WHAT = 10;
    private final int SCAN_ACTION_WHAT = 20;
    private final int CONNECT_SERVER_WHAT = 30;


    private Context mContext;
    private String mDevId;
    private WifiManager mWifiManager;

    private HandlerThread mHandlerThread;
    private ClientHandler mHandler;

    private OnWifiClientListener mListener;

    private Socket mSocket;
    private PrintWriter mWriter;
    private Gson mGson;
    private boolean isLogin = false;
    private ReadThread mReadThread;


    public WifiClient(Context context, String devId) {
        mContext = context;
        mDevId = devId;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new ClientHandler(mHandlerThread.getLooper());

        mGson = new Gson();
    }

    public void setOnWifiClientListener(OnWifiClientListener listener) {
        mListener = listener;
    }


    public interface OnWifiClientListener {
        void onWifiConnected();

        void onServerConnected();

        void onServerConnectfailed();

        void onSendfailed(String str);

        boolean onStartRecordVideo(String prefix);

        boolean onStopRecordVideo(String prefix);

        boolean onTakePicture(String prefix);

        boolean onTakeFtp();
    }


    private class ClientHandler extends Handler {

        public ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEARCH_WHAT:
                    search();
                    break;

                case SCAN_ACTION_WHAT:
                    List<ScanResult> scanResults = mWifiManager.getScanResults();
                    for (ScanResult result : scanResults) {
                        if (result.SSID.equals("mtm" + mDevId)) {
                            WifiConfiguration config = isExsits(result.SSID);
                            if (null == config) {
                                config = createConfig(result, result.SSID, "66668888" + mDevId);
                            }
                            connect(config);

                            break;
                        }
                    }

                    break;

                case CONNECT_SERVER_WHAT:
                    LoginBean bean = new LoginBean();
                    bean.request = Constant.REQUEST_LOGIN;
                    bean.data = new LoginBean.Data(mDevId);

                    String jonstr = mGson.toJson(bean);
                    try {
                        mWriter.print(jonstr + "\n");
                        mWriter.flush();
                        Log.i(TAG, "send " + jonstr);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (mListener != null)
                            mListener.onSendfailed("send error!");
                    }

                    break;

                case RESPONSE_START_RECORD_VIDEO_WHAT:

                    StartRecordVideoBean startBean = (StartRecordVideoBean) msg.obj;
                    if (mListener != null) {
                        boolean b = mListener.onStartRecordVideo(startBean.data.prefix);
                        Response response = new Response();
                        response.response = Constant.REQUEST_START_RECORD_VIDEO;
                        if (b)
                            response.status = 0;
                        else
                            response.status = -2;

                        send(mGson.toJson(response));

                    }

                    break;

                case RESPONSE_STOP_RECORD_VIDEO_WHAT:
                    StopRecordVideoBean stopBean = (StopRecordVideoBean) msg.obj;
                    if (mListener != null) {
                        boolean b = mListener.onStopRecordVideo(stopBean.data.prefix);
                        Response response = new Response();
                        response.response = Constant.REQUEST_STOP_RECORD_VIDEO;
                        if (b)
                            response.status = 0;
                        else
                            response.status = -3;

                        send(mGson.toJson(response));
                    }
                    break;

                case RESPONSE_TAKE_PICTURE_WHAT:
                    TakeBean takeBean = (TakeBean) msg.obj;
                    if (mListener != null) {
                        boolean b = mListener.onTakePicture(takeBean.data.prefix);
                        Response response = new Response();
                        response.response = Constant.REQUEST_TAKE_PICTURE;
                        if (b)
                            response.status = 0;
                        else
                            response.status = Constant.RESPOSNE_TAKE_PICTURE_FAILED;

                        send(mGson.toJson(response));
                    }
                    break;

                case RESPONSE_TAKE_FTP_WHAT:
                    if (mListener != null) {
                        boolean b = mListener.onTakeFtp();
                        Response response = new Response();
                        response.response = Constant.REQUEST_TAKE_FTP;
                        if (b)
                            response.status = 0;
                        else
                            response.status = Constant.RESPONSE_TAKE_FTP_FAILED;

                        send(mGson.toJson(response));
                    }
                    break;


                case SERVER_SOCKET_CLOSE_WHAT:
                    Log.i(TAG, "SERVER_SOCKET_CLOSE_WHAT");
                    mSocket = null;
                    if (mReadThread != null) {
                        mReadThread.stopRead();
                        mReadThread = null;
                    }
                    break;

                default:

                    break;
            }
        }
    }

    private void send(String jonstr) {

        Log.i(TAG, "send: "+jonstr);
        if (mWriter != null) {
            mWriter.print(jonstr + "\n");
            mWriter.flush();
        }
    }


    private void search() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        mWifiManager.startScan();
    }

    public void registerReceiver() {

        mHandler.sendEmptyMessage(SEARCH_WHAT);

        if (mContext != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            mContext.registerReceiver(receiver, intentFilter);
        }
    }

    public void unregisterReceiver() {
        if (mContext != null)
            mContext.unregisterReceiver(receiver);
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG, "onReceive: " + action);
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mHandler.sendEmptyMessage(SCAN_ACTION_WHAT);
            }

            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                //WIFI_STATE_DISABLED
                Log.i(TAG, "wifiState:" + wifiState);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        break;
                }

            }

        }

    };

    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private WifiConfiguration createConfig(ScanResult scan, String ssid, String pwd) {

        WifiConfiguration config = new WifiConfiguration();

        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        config.SSID = "\"" + ssid + "\"";
        config.hiddenSSID = false;


        config.preSharedKey = "\"" + pwd + "\"";
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.status = WifiConfiguration.Status.ENABLED;

        return config;
    }


    private void connect(WifiConfiguration config) {

        int wcgID = mWifiManager.addNetwork(config);
        boolean b = mWifiManager.enableNetwork(wcgID, true);
        if (b) {
            if (mListener != null)
                mListener.onWifiConnected();

            List<String> list = getConnectedIP();
            for (String s : list) {
                if (null == mSocket) {
                    try {
                        Log.i(TAG, "connect:  ip=" + s);
                        mSocket = new Socket(s, 1343);
                        if (mListener != null)
                            mListener.onServerConnected();

                        mWriter = new PrintWriter(mSocket.getOutputStream());
                        mHandler.sendEmptyMessage(CONNECT_SERVER_WHAT);

                        mReadThread = new ReadThread(mHandler, mSocket);
                        mReadThread.startRead();


                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        mSocket = null;
                        if (mListener != null)
                            mListener.onServerConnectfailed();
                    }
                }
            }
        }
    }

    /**
     * 获取连接到热点上的手机ip
     *
     * @return
     */
    private ArrayList<String> getConnectedIP() {

        ArrayList<String> connectedIP = new ArrayList<String>();
        DhcpInfo dhcpinfo = mWifiManager.getDhcpInfo();
        String serverAddress = intToIp(dhcpinfo.serverAddress);
        connectedIP.add(serverAddress);
        return connectedIP;
    }

    private String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }


}
