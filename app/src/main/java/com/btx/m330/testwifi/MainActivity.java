package com.btx.m330.testwifi;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.btx.abero.hotspot.Hotspot;

import java.io.BufferedReader;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "Hotspot";

    Hotspot hotspot;
    private TextView textView;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        textView = findViewById(R.id.text);
        findViewById(R.id.start_record).setOnClickListener(this);
        findViewById(R.id.stop_record).setOnClickListener(this);
        findViewById(R.id.take_picture).setOnClickListener(this);
        findViewById(R.id.take_ftp).setOnClickListener(this);

        hotspot = new Hotspot(this, "5107");

        hotspot.setOnClientListener(new Hotspot.OnClientListener() {
            @Override
            public void onHotspotCreated() {
                Log.i(TAG, "onHotspotCreated: ");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("已创建热点");
                    }
                });

            }

            @Override
            public void onClientConnected() {
                Log.i(TAG, "onClientConnected: ");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("设备已连接");
                    }
                });
            }
        });
    }


    @Override
    public void onClick(View v) {

        int id = v.getId();
        switch (id) {
            case R.id.start_record:
                hotspot.startRecordVideo("I04041kkkk", new Hotspot.Callback() {
                    @Override
                    public void onSuccess(String data) {
                        Log.i(TAG, "record onSuccess: ");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "已开始录像", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                    @Override
                    public void onFailed(int status) {
                        Log.i(TAG, "record onFailed: ");
                        Toast.makeText(MainActivity.this, "开始录像失败", Toast.LENGTH_SHORT).show();
                    }
                });
                break;

            case R.id.stop_record:
                hotspot.stopRecordVideo("I04041kkkk", new Hotspot.Callback() {
                    @Override
                    public void onSuccess(String data) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "已停止录像", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailed(int status) {
                        Toast.makeText(MainActivity.this, "停止录像失败", Toast.LENGTH_SHORT).show();
                    }
                });
                break;

            case R.id.take_picture:
                hotspot.takePicture("I0405FFFFF", new Hotspot.Callback() {
                    @Override
                    public void onSuccess(String data) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailed(int status) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                break;

            case R.id.take_ftp:
                hotspot.takeFtp(new Hotspot.Callback() {
                    @Override
                    public void onSuccess(String data) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "已触发自动上传", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailed(int status) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "触发自动上传失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                break;

            default:
                break;
        }
    }
}
