package com.mahui.sockettest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mahui.sockettest.service.TCPServerService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TCPClientActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG="TCPClientActivity";
    private static final int MESSAGE_RECEIVE_NEW_MSG=1;
    private static final int MESSAGE_SOCKET_CONNECTED=2;
    private Button mSendButton;
    private TextView mMessageTextView;
    private EditText mMessageEditText;
    private PrintWriter mPrintWriter;
    private Socket mClientSocket;
    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_RECEIVE_NEW_MSG:
                    mMessageTextView.setText(mMessageTextView.getText()+(String)msg.obj);
                    break;
                case MESSAGE_SOCKET_CONNECTED:
                    mSendButton.setEnabled(true);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMessageTextView= (TextView) findViewById(R.id.msg_container);
        mSendButton= (Button) findViewById(R.id.send);
        mSendButton.setOnClickListener(this);
        mMessageEditText= (EditText) findViewById(R.id.msg);
        Intent service=new Intent(TCPClientActivity.this, TCPServerService.class);
        startService(service);
        new Thread(){
            @Override
            public void run() {
                connectTCPServer();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if(mClientSocket!=null){
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        String msg=mMessageEditText.getText().toString();
        if(!TextUtils.isEmpty(msg)&&mPrintWriter!=null){
            mPrintWriter.println(msg);
            mMessageEditText.setText("");
            String time=formDateTime(System.currentTimeMillis());
            String showMsg="我"+time+":"+msg+"\n";
            mMessageTextView.setText(mMessageTextView.getText()+showMsg);
        }
    }
    private String formDateTime(long time){
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }
    public void connectTCPServer(){
        Socket socket=null;
        while(socket==null){
            try {
                //自己PING的手机IP
                //socket=new Socket("10.0.0.106",8888);
                socket=new Socket("localhost",8688);
                //真实服务器主机和端口号
                //socket=new Socket("192.168.5.50",8888);
                mClientSocket=socket;
                mPrintWriter=new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                Log.d(TAG,"连接成功");
            } catch (IOException e) {
                e.printStackTrace();
                SystemClock.sleep(1000);
                Log.d(TAG,"connect tcp server failed,retry....");
            }
        }
        //接到服务器端消息
        try {
            BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while(!TCPClientActivity.this.isFinishing()){
                String msg=br.readLine();
                Log.d(TAG,"receive:"+msg);
                if(msg!=null){
                    String time=formDateTime(System.currentTimeMillis());
                    String showedMsg="server"+time+":"+msg+"\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG,showedMsg).sendToTarget();
                }
            }
            Log.d(TAG,"QUIT。。。");
            if(mPrintWriter!=null){
                mPrintWriter.close();
            }
            if(br!=null){
                br.close();
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
