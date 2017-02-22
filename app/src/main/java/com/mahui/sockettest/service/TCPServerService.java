package com.mahui.sockettest.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Created by Administrator on 2017/2/14.
 */

public class TCPServerService extends Service{
    private static final String TAG="TCPServerService";
    private boolean mIsServiceDestoryed=false;
    private String[] mDefinedMessages=new String[]{
            "你好啊，哈哈","请问你叫什么名字呀？","今天北京天气不错啊。","你知道吗？我可是可以跟多个人同时聊天"
            ,"给你讲个笑话吧。哈哈哈哈"
    };

    @Override
    public void onCreate() {
        new Thread(new TcpServer()).start();
        super.onCreate();
        Log.e(TAG,"建立TCP，接口8688");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mIsServiceDestoryed=true;
        super.onDestroy();
    }
    private class TcpServer implements Runnable{
        @Override
        public void run() {
            ServerSocket serverSocket=null;
            try {
                serverSocket=new ServerSocket(8688);
            } catch (IOException e) {
                Log.e(TAG,"建立TCP失败，接口8688");
                e.printStackTrace();
                return;
            }
            while (!mIsServiceDestoryed){
                try {
                    final Socket client=serverSocket.accept();
                    Log.d(TAG,"接收");
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                responseClient(client);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void responseClient(Socket client) throws  IOException{
        //用于接收客户端消息
        BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream()));
        //用于向客户端发送消息
        PrintWriter out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),true);
        out.println("欢迎来到聊天室");
        while (!mIsServiceDestoryed){
            String str=in.readLine();
            Log.d(TAG,"msg from client:"+str);
            if(str==null){
                //客户断开连接
                break;
            }
            int i=new Random().nextInt(mDefinedMessages.length);
            String msg=mDefinedMessages[i];
            out.println(msg);
            Log.d(TAG,"send :"+msg);
        }
        Log.d(TAG,"client quit.");
        //关闭流
        if(out!=null){
            out.close();
        }
        if(in!=null){
            in.close();
        }
        client.close();
    }
}
