package com.btx.abero.hotspot;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by abero on 2018/9/25.
 */

public class ListenerThread extends Thread {

    private static final String TAG = "ListenerThread";

    private ServerSocket mServerSocket = null;
    private int port;
    private OnServerCallback mCallback;

    public ListenerThread(OnServerCallback callback, int port) {
        this.mCallback = callback;
        this.port = port;
        try {
            mServerSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface OnServerCallback {

        void onAccept(Socket socket);

        void onServerOut();
    }

    public void startListener() {
        if (!isAlive())
            start();
    }

    public void stopListener() {
        interrupt();
    }


    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Socket socket = mServerSocket.accept();
                Log.i(TAG, "accept from  " + socket.getRemoteSocketAddress());
                if (mCallback != null)
                    mCallback.onAccept(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mServerSocket != null)
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        if (mCallback != null)
            mCallback.onServerOut();


        Log.i(TAG, "listener thread exit!");
    }

}
