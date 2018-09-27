package com.btx.m330.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.btx.abero.clientlibrary.WifiClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WifiClient";

    private WifiClient wifiClient;
    private String mPrefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiClient = new WifiClient(this.getApplicationContext(), "5107");

        wifiClient.setOnWifiClientListener(new WifiClient.OnWifiClientListener() {
            @Override
            public void onWifiConnected() {
                Log.i(TAG, "onWifiConnected: ");
            }

            @Override
            public void onServerConnected() {
                Log.i(TAG, "onServerConnected: ");
            }

            @Override
            public void onServerConnectfailed() {
                Log.i(TAG, "onServerConnectfailed: ");
            }

            @Override
            public void onSendfailed(String str) {
                Log.i(TAG, "onSendfailed: ");
            }

            @Override
            public boolean onStartRecordVideo(String prefix) {
                Log.i(TAG, "onStartRecordVideo: ");
                mPrefix = prefix;
                return true;
            }

            @Override
            public boolean onStopRecordVideo(String prefix) {
                Log.i(TAG, "onStopRecordVideo: ");
                if (prefix.equals(mPrefix))
                    return true;
                else
                    return false;
            }

            @Override
            public boolean onTakePicture(String prefix) {
                Log.i(TAG, "onTakePicture");
                return false;
            }

            @Override
            public boolean onTakeFtp() {
                return false;
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        wifiClient.registerReceiver();

    }

    @Override
    protected void onPause() {
        super.onPause();

        wifiClient.unregisterReceiver();

    }


}
