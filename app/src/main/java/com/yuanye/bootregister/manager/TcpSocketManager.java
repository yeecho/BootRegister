package com.yuanye.bootregister.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yuanye.bootregister.MainActivity;
import com.yuanye.bootregister.util.ByteHandle;

public class TcpSocketManager {
    private static final String TAG = "TcpSocketManager";
    private static final String HOST = "129.204.66.131";
    private static final int PORT = 9999;
    private InputStream in;
    private OutputStream out;
    private String receiveMsg;
    Context mContext;
    boolean isRun = false;
    Socket socket;
    Handler mHandler;
    public TcpSocketManager(Context context, Handler handler){
        mContext = context;
        mHandler = handler;

        mMyThreadRev = new MyThreadRev();
        mMyThreadRev.start();
    }
    MyThreadRev mMyThreadRev;
    private void initClient(String data){
        try {
            if(socket != null){
                socket.close();
                socket = null;
            }
            socket = new Socket(HOST, PORT);
            socket.setSoTimeout(60000);
            in = socket.getInputStream();
            out = socket.getOutputStream();

        } catch (Exception e) {
            Log.e(TAG, ("initClient:" + e.getMessage()));
            Message msg = new Message();
            msg.what = MainActivity.HANDLER_SOCKET_ERROR;
            msg.obj = e.toString();
            mHandler.sendMessage(msg);
            return;
        }

        send(data);
    }
    public void register(final String data){
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                initClient(data);
            }
        }).start();
    }
    public void send(String data){
        try {
            out.write(data.getBytes());
            out.flush();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, ("send error:" + e.getMessage()));
            Message msg = new Message();
            msg.what = MainActivity.HANDLER_SOCKET_ERROR;
            msg.obj = e.toString();
            mHandler.sendMessage(msg);
        }
    }

    public void send2(String data){
        try {
            out.write(data.getBytes());
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            initClient(data);
        }
    }

    public void destroyed(){
        isRun = false;
        try {
            if(socket != null){
                socket.close();
                socket = null;
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    class MyThreadRev extends Thread{

        public MyThreadRev() {
            // TODO Auto-generated constructor stub
            isRun = true;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            while (isRun) {
                if(in==null){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;
                }
                try {
                    byte[] b = new byte[1024];
                    int readBytes = 0;
                    int len = b.length;
                    int read = 0;
                    byte[] byteAll = null;

                    while ((read = in.read(b, readBytes, len)) != -1) {
                        if(read==-1)break;
                        byte[]b1 = new byte[read];
                        System.arraycopy(b, 0, b1, 0, read);
                        if(byteAll == null) {
                            byteAll = b1;
                        }else{
                            byteAll = ByteHandle.byteConnection(byteAll, b1);
                        }
                        if(in.available() == 0){
                            Message msg = new Message();
                            msg.what = MainActivity.HANDLER_RECEIVE;
                            //Log.i("mxl",ByteHandle.bytesToHexString(byteAll));
                            String revData = new String(byteAll);
                            Log.i("mxl",new String(byteAll));
                            msg.obj = revData;
                            //msg.obj = ByteHandle.decode(ByteHandle.bytesToHexString(byteAll));
                            mHandler.sendMessage(msg);
                            byteAll = null;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if(in != null){
                        in.close();
                        in = null;
                    }
                    if(out != null){
                        out.close();
                        out = null;
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }


        }
    }

}
